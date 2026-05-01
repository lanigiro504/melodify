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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 使用 SunoAPI 官方路径：POST /generate + 轮询 GET /generate/record-info。
 * <p>与文档一致：成功时优先从 {@code data.response.sunoData[]}（OpenAPI）；兼容旧版 {@code response.data[]}。</p>
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
	private static final Set<String> SUCCESS_STATUSES =
			Set.of("SUCCESS", "FIRST_SUCCESS", "COMPLETE", "COMPLETED", "DONE", "FINISHED");

	private final MusicTaskService musicTaskService;
	private final SunoApiClient sunoApiClient;
	private final MusicTaskCompletionFacade completionFacade;
	private final GeneratePointsRefundService refundService;
	private final SunoApiProperties sunoApiProperties;
	private final ObjectMapper objectMapper;

	private record CompletedClip(String title, String audioUrl, int durationSec) {
	}

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
		} catch (SunoApiClient.SunoApiException ex) {
			log.warn("Suno 提交生成失败 pk={}: {}", internalMusicTaskPk, ex.toString());
			handleFailure(internalMusicTaskPk, task, ex.getErrorCode(), ex.getMessage());
			return;
		} catch (RuntimeException ex) {
			log.warn("Suno 提交生成失败 pk={}: {}", internalMusicTaskPk, ex.toString());
			handleFailure(internalMusicTaskPk, task, "SUNO_HTTP", ex.getMessage());
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
			} catch (SunoApiClient.SunoApiException | RuntimeException ex) {
				log.warn("Suno record-info 失败 pk={}: {}", internalPk, ex.toString());
				sleepSafe();
				continue;
			}
			String st = resolveStatus(info);
			if (SUCCESS_STATUSES.contains(st)) {
				if (tryPersistCompletedClip(internalPk, task, info, false)) {
					return;
				}
				sleepSafe();
				continue;
			}
			if (isGenerationFailedStatus(st)) {
				String err = resolveErrorMessage(info, "Suno 生成失败");
				handleFailure(internalPk, task, "SUNO_FAILED", err.isEmpty() ? st : err);
				return;
			}
			sleepSafe();
		}
		handleFailure(internalPk, task, "SUNO_TIMEOUT", "Suno 生成超时");
	}

	/** @return true 已成功落库并应结束轮询 */
	public boolean trySyncCompletedAsset(Long internalPk) {
		MusicTask task = musicTaskService.getById(internalPk);
		if (task == null || !StringUtils.hasText(task.getVendorTaskId())) {
			return false;
		}
		try {
			JsonNode info = sunoApiClient.fetchGenerateRecord(task.getVendorTaskId());
			String st = resolveStatus(info);
			if (!SUCCESS_STATUSES.contains(st)) {
				return false;
			}
			return tryPersistCompletedClip(internalPk, task, info, true);
		} catch (SunoApiClient.SunoApiException | RuntimeException ex) {
			log.warn("Suno 补偿同步失败 pk={}: {}", internalPk, ex.toString());
			return false;
		}
	}

	/**
	 * 处理 Suno 官方文档中的 HTTP 回调体（POST application/json）。
	 * <p>应在独立线程中调用，尽快返回 HTTP 200 给网关。</p>
	 */
	public void handleSunoHttpCallback(JsonNode root) {
		if (root == null || root.isNull()) {
			return;
		}
		JsonNode data = root.get("data");
		if (data == null || !data.isObject()) {
			log.warn("Suno 回调缺少 data");
			return;
		}
		String vendorTaskId = firstNonEmpty(textField(data, "task_id"), textField(data, "taskId"));
		if (!StringUtils.hasText(vendorTaskId)) {
			log.warn("Suno 回调缺少 task_id");
			return;
		}
		MusicTask task = musicTaskService.lambdaQuery()
				.eq(MusicTask::getVendorTaskId, vendorTaskId)
				.one();
		if (task == null) {
			log.warn("Suno 回调未匹配本地任务 vendorTaskId={}", vendorTaskId);
			return;
		}
		Long internalPk = task.getId();
		int code = root.path("code").asInt(-1);
		String callbackType = textField(data, "callbackType").toLowerCase(Locale.ROOT);
		String msg = root.path("msg").asText("");

		if (code != 200) {
			if ("error".equals(callbackType) || code >= 400) {
				handleFailure(internalPk, task, "SUNO_CALLBACK",
						StringUtils.hasText(msg) ? msg : ("code=" + code));
			}
			return;
		}
		if ("error".equals(callbackType)) {
			handleFailure(internalPk, task, "SUNO_CALLBACK", StringUtils.hasText(msg) ? msg : "error");
			return;
		}
		if ("text".equals(callbackType)) {
			return;
		}
		JsonNode tracks = data.get("data");
		if (tracks != null && tracks.isArray() && !tracks.isEmpty()) {
			persistClipFromCallback(internalPk, tracks.get(0));
			return;
		}
		trySyncCompletedAsset(internalPk);
	}

	private void persistClipFromCallback(Long internalPk, JsonNode clipNode) {
		MusicTask task = musicTaskService.getById(internalPk);
		if (task == null) {
			return;
		}
		String audioUrl = extractAudioUrl(clipNode);
		if (!StringUtils.hasText(audioUrl)) {
			trySyncCompletedAsset(internalPk);
			return;
		}
		String title = clipNode.path("title").asText("");
		int durationSec = (int) Math.round(clipNode.path("duration").asDouble(0));
		Integer st = task.getStatus();
		try {
			if (Integer.valueOf(MusicTaskStatuses.SUCCEEDED).equals(st)) {
				completionFacade.syncSucceededAssetFromSuno(internalPk,
						StringUtils.hasText(title) ? title : null,
						audioUrl,
						durationSec);
				return;
			}
			if (Integer.valueOf(MusicTaskStatuses.GENERATING).equals(st)) {
				completionFacade.markSucceededAndPersistAsset(internalPk,
						StringUtils.hasText(title) ? title : null,
						audioUrl,
						durationSec);
			}
		} catch (Exception ex) {
			log.error("Suno 回调落库失败 pk={}", internalPk, ex);
			handleFailure(internalPk, task, "SUNO_CALLBACK_PERSIST", ex.getMessage());
		}
	}

	private static String firstNonEmpty(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		if (StringUtils.hasText(b)) {
			return b;
		}
		return "";
	}

	private boolean tryPersistCompletedClip(Long internalPk, MusicTask task, JsonNode recordData,
			boolean allowAlreadySucceeded) {
		CompletedClip clip = resolveCompletedClip(recordData);
		if (clip == null) {
			log.warn("Suno 成功响应未找到音频 URL pk={}，顶层字段={}", internalPk, topLevelFieldNames(recordData));
			return false;
		}
		try {
			if (allowAlreadySucceeded) {
				return completionFacade.syncSucceededAssetFromSuno(internalPk,
						StringUtils.hasText(clip.title()) ? clip.title() : null,
						clip.audioUrl(),
						clip.durationSec());
			}
			completionFacade.markSucceededAndPersistAsset(internalPk,
					StringUtils.hasText(clip.title()) ? clip.title() : null,
					clip.audioUrl(),
					clip.durationSec());
		} catch (Exception ex) {
			log.error("写入成品失败 pk={}", internalPk, ex);
			handleFailure(internalPk, task, "PERSIST", ex.getMessage());
		}
		return true;
	}

	private static CompletedClip resolveCompletedClip(JsonNode recordData) {
		JsonNode clip = resolveFirstClipWithAudio(recordData);
		if (clip == null) {
			return null;
		}
		String audioUrl = extractAudioUrl(clip);
		if (!StringUtils.hasText(audioUrl)) {
			return null;
		}
		String title = clip.hasNonNull("title") ? clip.get("title").asText("") : "";
		int durationSec = clip.has("duration")
				? (int) Math.round(clip.get("duration").asDouble(0))
				: 0;
		return new CompletedClip(title, audioUrl.strip(), durationSec);
	}

	private static List<String> topLevelFieldNames(JsonNode node) {
		if (node == null || !node.isObject()) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		node.fieldNames().forEachRemaining(names::add);
		return names;
	}

	/**
	 * 官方 record-info：音轨在 {@code data.response.sunoData[]}，元素字段为 camelCase（audioUrl）。
	 * 旧版/回调示例可能为 {@code response.data[]} + snake_case（audio_url），此处一并兼容。
	 */
	private static JsonNode resolveFirstClipWithAudio(JsonNode recordData) {
		JsonNode recursive = findClipWithAudio(recordData, 0);
		if (recursive != null) {
			return recursive;
		}
		for (JsonNode candidate : responseCandidates(recordData)) {
			JsonNode directClip = clipIfHasAudio(candidate);
			if (directClip != null) {
				return directClip;
			}
			JsonNode arr = clipsArray(candidate);
			if (arr == null || !arr.isArray() || arr.isEmpty()) {
				continue;
			}
			for (JsonNode clip : arr) {
				if (clipIfHasAudio(clip) != null) {
					return clip;
				}
			}
		}
		return null;
	}

	private static JsonNode findClipWithAudio(JsonNode node, int depth) {
		if (node == null || depth > 8) {
			return null;
		}
		if (clipIfHasAudio(node) != null) {
			return node;
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				JsonNode found = findClipWithAudio(child, depth + 1);
				if (found != null) {
					return found;
				}
			}
			return null;
		}
		if (node.isObject()) {
			var fields = node.fields();
			while (fields.hasNext()) {
				JsonNode found = findClipWithAudio(fields.next().getValue(), depth + 1);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static List<JsonNode> responseCandidates(JsonNode recordData) {
		List<JsonNode> candidates = new ArrayList<>();
		addCandidate(candidates, recordData);
		JsonNode data = objectChild(recordData, "data");
		JsonNode response = objectChild(recordData, "response");
		JsonNode nestedResponse = objectChild(data, "response");
		addCandidate(candidates, response);
		addCandidate(candidates, data);
		addCandidate(candidates, nestedResponse);
		return candidates;
	}

	private static void addCandidate(List<JsonNode> candidates, JsonNode node) {
		if (node != null && node.isObject()) {
			candidates.add(node);
		}
	}

	private static JsonNode objectChild(JsonNode node, String fieldName) {
		if (node == null || !node.isObject()) {
			return null;
		}
		JsonNode child = node.get(fieldName);
		return child != null && child.isObject() ? child : null;
	}

	private static JsonNode clipIfHasAudio(JsonNode clip) {
		return clip != null && clip.isObject() && StringUtils.hasText(extractAudioUrl(clip)) ? clip : null;
	}

	private static JsonNode clipsArray(JsonNode responseObj) {
		if (responseObj == null || !responseObj.isObject()) {
			return null;
		}
		JsonNode sunoData = responseObj.get("sunoData");
		if (sunoData != null && sunoData.isArray() && !sunoData.isEmpty()) {
			return sunoData;
		}
		JsonNode legacyData = responseObj.get("data");
		if (legacyData != null && legacyData.isArray() && !legacyData.isEmpty()) {
			return legacyData;
		}
		JsonNode clips = responseObj.get("clips");
		if (clips != null && clips.isArray() && !clips.isEmpty()) {
			return clips;
		}
		JsonNode songs = responseObj.get("songs");
		if (songs != null && songs.isArray() && !songs.isEmpty()) {
			return songs;
		}
		return null;
	}

	private static String extractAudioUrl(JsonNode clip) {
		if (clip == null || !clip.isObject()) {
			return "";
		}
		String[] keys = {"audio_url", "audioUrl", "source_audio_url", "sourceAudioUrl",
				"source_stream_audio_url", "sourceStreamAudioUrl",
				"stream_audio_url", "streamAudioUrl", "downloadUrl", "download_url"};
		for (String k : keys) {
			JsonNode v = clip.get(k);
			if (v != null && v.isValueNode() && !v.isNull()) {
				String url = v.asText("").strip();
				if (StringUtils.hasText(url) && looksLikeHttpUrl(url)) {
					return url;
				}
			}
		}
		/* 网关字段偶发更名：扫一级字段里名称含 audio/stream/song/track 的 http(s) 串 */
		var it = clip.fields();
		while (it.hasNext()) {
			var e = it.next();
			JsonNode v = e.getValue();
			if (v == null || !v.isValueNode() || v.isNull()) {
				continue;
			}
			String name = e.getKey().toLowerCase(Locale.ROOT);
			if (!fieldNameLikelyAudioUrl(name)) {
				continue;
			}
			String url = v.asText("").strip();
			if (StringUtils.hasText(url) && looksLikeHttpUrl(url)) {
				return url;
			}
		}
		return "";
	}

	private static boolean fieldNameLikelyAudioUrl(String keyLower) {
		return keyLower.contains("audio")
				|| keyLower.contains("stream")
				|| keyLower.contains("song")
				|| keyLower.contains("track")
				|| keyLower.contains("media")
				|| keyLower.contains("playback");
	}

	private static boolean looksLikeHttpUrl(String url) {
		String u = url.toLowerCase(Locale.ROOT);
		return u.startsWith("http://") || u.startsWith("https://");
	}

	private static boolean isGenerationFailedStatus(String st) {
		if (st.isEmpty()) {
			return false;
		}
		return switch (st) {
			case "FAILED",
					"FAILURE",
					"ERROR",
					"CREATE_TASK_FAILED",
					"GENERATE_AUDIO_FAILED",
					"CALLBACK_EXCEPTION",
					"SENSITIVE_WORD_ERROR" -> true;
			default -> false;
		};
	}

	private static String resolveStatus(JsonNode info) {
		String status = textField(info, "status");
		if (!StringUtils.hasText(status)) {
			status = textField(info, "taskStatus");
		}
		if (!StringUtils.hasText(status)) {
			status = textField(info, "state");
		}
		return status.trim().toUpperCase(Locale.ROOT);
	}

	private static String resolveErrorMessage(JsonNode info, String fallback) {
		for (String key : List.of("errorMessage", "error_message", "message", "msg")) {
			String message = textField(info, key);
			if (StringUtils.hasText(message)) {
				return message;
			}
		}
		return fallback;
	}

	private static String textField(JsonNode node, String key) {
		if (node == null || !node.isObject()) {
			return "";
		}
		JsonNode val = node.get(key);
		if (val != null && val.isValueNode() && !val.isNull()) {
			return val.asText("").strip();
		}
		for (String childKey : List.of("data", "response")) {
			String nested = textField(objectChild(node, childKey), key);
			if (StringUtils.hasText(nested)) {
				return nested;
			}
		}
		return "";
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

		// 自定义人声：Suno 的 prompt 需为可唱歌词；与「创作描述」拆分后歌词在 params.lyrics。
		if (customMode && !instrumental && hasNonBlank(p, "lyrics")) {
			root.put("prompt", String.valueOf(p.get("lyrics")).strip());
		} else if (StringUtils.hasText(task.getPrompt())) {
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
		String base = null;
		if (p != null && p.get("callBackUrl") != null) {
			String fromTask = String.valueOf(p.get("callBackUrl")).strip();
			if (StringUtils.hasText(fromTask)) {
				base = fromTask;
			}
		}
		if (!StringUtils.hasText(base) && StringUtils.hasText(sunoApiProperties.getCallbackUrl())) {
			base = sunoApiProperties.getCallbackUrl().strip();
		}
		if (!StringUtils.hasText(base)) {
			return "https://example.invalid/melodify-no-http-callback";
		}
		return appendCallbackToken(base);
	}

	private String appendCallbackToken(String base) {
		if (!StringUtils.hasText(sunoApiProperties.getCallbackToken())) {
			return base;
		}
		if (base.contains("example.invalid")) {
			return base;
		}
		String enc = URLEncoder.encode(sunoApiProperties.getCallbackToken(), StandardCharsets.UTF_8);
		String sep = base.contains("?") ? "&" : "?";
		return base + sep + "token=" + enc;
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
