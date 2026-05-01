package com.melodify.integration.suno;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.melodify.config.SunoApiProperties;
import com.melodify.constants.MusicTaskStatuses;
import com.melodify.entity.MusicTask;
import com.melodify.service.GeneratePointsRefundService;
import com.melodify.service.MusicTaskCompletionFacade;
import com.melodify.service.MusicTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 使用 SunoAPI 官方路径：POST /generate + 轮询 GET /generate/record-info。
 * <p>与 JS 示例一致：成功时从 {@code data.response.data[0]} 取 {@code audio_url}、标题与时长。</p>
 * <p>请求体对齐 OpenAPI：必选 {@code customMode}、{@code instrumental}、{@code callBackUrl}、{@code model}；
 * 非自定义模式下只传提示词与其它参数清空；回调仅作合规占位时可用配置或内置占位 URI，实际完成态仍可依赖轮询。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicSunoGenerationRunner {

	/** Suno 文档允许的可选字段，从 {@code params} 原样透出（不传则不写入 JSON）。 */
	private static final List<String> SUNO_OPTIONAL_PARAM_KEYS =
			List.of("negativeTags", "vocalGender", "styleWeight", "weirdnessConstraint",
					"audioWeight", "personaId", "personaModel");

	private final MusicTaskService musicTaskService;
	private final SunoApiClient sunoApiClient;
	private final MusicTaskCompletionFacade completionFacade;
	private final GeneratePointsRefundService refundService;
	private final SunoApiProperties sunoApiProperties;
	private final ObjectMapper objectMapper;

	@Async("musicTaskExecutor")
	public void completeAfterSubmit(Long internalMusicTaskPk) {
		MusicTask task = musicTaskService.getById(internalMusicTaskPk);
		if (task == null) {
			return;
		}
		if (!Integer.valueOf(MusicTaskStatuses.GENERATING).equals(task.getStatus())) {
			return;
		}

		String sunoVendorId;
		try {
			JsonNode payload = buildGeneratePayload(task);
			sunoVendorId = sunoApiClient.postGenerate(payload);
		} catch (Exception ex) {
			log.warn("Suno 提交生成失败 pk={}: {}", internalMusicTaskPk, ex.toString());
			handleFailure(internalMusicTaskPk, task,
					ex instanceof SunoApiClient.SunoApiException sa ? sa.getErrorCode() : "SUNO_HTTP",
					ex.getMessage());
			return;
		}

		musicTaskService.lambdaUpdate()
				.eq(MusicTask::getId, internalMusicTaskPk)
				.eq(MusicTask::getStatus, MusicTaskStatuses.GENERATING)
				.set(MusicTask::getVendorTaskId, sunoVendorId)
				.update();

		pollUntilDone(internalMusicTaskPk, sunoVendorId);
	}

	private void pollUntilDone(Long internalPk, String sunoVendorId) {
		MusicTask task = musicTaskService.getById(internalPk);
		if (task == null) {
			return;
		}
		long deadline = System.currentTimeMillis() + Math.max(5_000L, sunoApiProperties.getMaxWaitMs());
		while (System.currentTimeMillis() < deadline) {
			JsonNode info;
			try {
				info = sunoApiClient.fetchGenerateRecord(sunoVendorId);
			} catch (Exception ex) {
				log.warn("Suno record-info 失败 pk={}: {}", internalPk, ex.toString());
				sleepSafe();
				continue;
			}
			String st = info.path("status").asText("").trim().toUpperCase(Locale.ROOT);
			if ("SUCCESS".equals(st)) {
				onSuccess(internalPk, task, info);
				return;
			}
			if ("FAILED".equals(st)) {
				String err = info.path("errorMessage").asText("Suno 生成失败");
				handleFailure(internalPk, task, "SUNO_FAILED", err);
				return;
			}
			sleepSafe();
		}
		handleFailure(internalPk, task, "SUNO_TIMEOUT", "Suno 生成超时");
	}

	private void onSuccess(Long internalPk, MusicTask task, JsonNode recordData) {
		JsonNode response = recordData.get("response");
		if (response == null || !response.has("data") || !response.get("data").isArray()
				|| response.get("data").isEmpty()) {
			handleFailure(internalPk, task, "SUNO_PAYLOAD", "成功响应缺少 response.data");
			return;
		}
		JsonNode first = response.get("data").get(0);
		String audioUrl = first.hasNonNull("audio_url") ? first.get("audio_url").asText("")
				: first.path("audioUrl").asText("");
		if (!StringUtils.hasText(audioUrl)) {
			handleFailure(internalPk, task, "SUNO_AUDIO", "未返回可用的音频 URL");
			return;
		}
		String title = first.hasNonNull("title") ? first.get("title").asText("") : "";
		int durationSec = 0;
		if (first.has("duration")) {
			durationSec = (int) Math.round(first.get("duration").asDouble(0));
		}
		try {
			completionFacade.markSucceededAndPersistAsset(internalPk,
					StringUtils.hasText(title) ? title : null,
					audioUrl.strip(),
					durationSec);
		} catch (Exception ex) {
			log.error("写入成品失败 pk={}", internalPk, ex);
			handleFailure(internalPk, task, "PERSIST", ex.getMessage());
		}
	}

	private void handleFailure(Long internalPk, MusicTask task, String code, String message) {
		boolean moved = completionFacade.markGenerationFailed(internalPk, code, message);
		if (moved) {
			task = musicTaskService.getById(internalPk);
			if (task != null) {
				refundService.refundGenerationCostIfConsumed(task);
			}
		}
	}

	private JsonNode buildGeneratePayload(MusicTask task) {
		ObjectNode root = objectMapper.createObjectNode();
		Map<String, Object> p = task.getParams();

		boolean customMode = resolveCustomMode(p);
		boolean instrumental = resolveInstrumental(p);

		root.put("customMode", customMode);
		root.put("instrumental", instrumental);

		if (StringUtils.hasText(task.getPrompt())) {
			root.put("prompt", task.getPrompt().strip());
		}

		if (customMode) {
			copyJsonField(root, p, "style");
			copyJsonField(root, p, "title");
			if (p != null) {
				for (String key : SUNO_OPTIONAL_PARAM_KEYS) {
					copyJsonField(root, p, key);
				}
			}
		}

		String model = null;
		if (p != null && p.get("model") != null) {
			model = String.valueOf(p.get("model")).strip();
		}
		if (!StringUtils.hasText(model) && task.getModelCode() != null) {
			model = task.getModelCode().strip();
		}
		if (!StringUtils.hasText(model)) {
			model = sunoApiProperties.getDefaultModel();
		}
		root.put("model", model);

		// OpenAPI 将 callBackUrl 标为必填；未配置服务端 webhook 时使用不可路由占位域名，只靠轮询落库。
		String callback = resolveCallbackUrl(p);
		root.put("callBackUrl", callback);
		return root;
	}

	/**
	 * 未显式传 {@code customMode} 时：若既没有 style 也没有 title，则按文档推荐走非自定义模式（仅 prompt）。
	 */
	private boolean resolveCustomMode(Map<String, Object> p) {
		if (p != null && p.containsKey("customMode")) {
			return toBoolean(p.get("customMode"), false);
		}
		return hasNonBlank(p, "style") || hasNonBlank(p, "title");
	}

	private static boolean resolveInstrumental(Map<String, Object> p) {
		if (p != null && p.containsKey("instrumental")) {
			return toBoolean(p.get("instrumental"), false);
		}
		return false;
	}

	private String resolveCallbackUrl(Map<String, Object> p) {
		if (p != null && p.get("callBackUrl") != null) {
			String fromTask = String.valueOf(p.get("callBackUrl")).strip();
			if (StringUtils.hasText(fromTask)) {
				return fromTask;
			}
		}
		if (StringUtils.hasText(sunoApiProperties.getCallbackUrl())) {
			return sunoApiProperties.getCallbackUrl().strip();
		}
		return "https://example.invalid/melodify-no-http-callback";
	}

	private static boolean hasNonBlank(Map<String, Object> p, String key) {
		if (p == null || !p.containsKey(key) || p.get(key) == null) {
			return false;
		}
		return StringUtils.hasText(String.valueOf(p.get(key)).strip());
	}

	private static void copyJsonField(ObjectNode root, Map<String, Object> params, String key) {
		if (params == null || !params.containsKey(key) || params.get(key) == null) {
			return;
		}
		Object v = params.get(key);
		if (v instanceof Number n) {
			root.put(key, n.doubleValue());
		} else if (v instanceof Boolean b) {
			root.put(key, b);
		} else {
			String s = String.valueOf(v).strip();
			if (StringUtils.hasText(s)) {
				root.put(key, s);
			}
		}
	}

	private static boolean toBoolean(Object v, boolean defaultVal) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof String s) {
			return Boolean.parseBoolean(s);
		}
		return defaultVal;
	}

	private void sleepSafe() {
		try {
			Thread.sleep(Math.max(2_000L, sunoApiProperties.getPollIntervalMs()));
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
