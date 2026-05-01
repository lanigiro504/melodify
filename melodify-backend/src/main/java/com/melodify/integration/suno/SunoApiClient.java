package com.melodify.integration.suno;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodify.config.SunoApiProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * SunoAPI 出站调用：独立超时配置，单例 {@link RestClient}（配置在启动期稳定）。
 */
@Component
@RequiredArgsConstructor
public class SunoApiClient {

	private final SunoApiProperties sunoApiProperties;
	private final ObjectMapper objectMapper;

	private final SimpleClientHttpRequestFactory sunoRequestFactory = buildRequestFactory();
	private RestClient restClient;

	private static SimpleClientHttpRequestFactory buildRequestFactory() {
		SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
		rf.setConnectTimeout(30_000);
		rf.setReadTimeout(120_000);
		return rf;
	}

	@PostConstruct
	void initRestClient() {
		this.restClient = RestClient.builder()
				.requestFactory(sunoRequestFactory)
				.baseUrl(trimTrailingSlash(sunoApiProperties.getApiBaseUrl()))
				.defaultHeader(HttpHeaders.AUTHORIZATION, bearer())
				.build();
	}

	private String bearer() {
		String k = sunoApiProperties.getApiKey() == null ? "" : sunoApiProperties.getApiKey().trim();
		return "Bearer " + k;
	}

	public boolean isConfigured() {
		return StringUtils.hasText(sunoApiProperties.getApiKey());
	}

	public String postGenerate(JsonNode body) throws SunoApiException {
		requireConfigured();
		String payload = writeJson(body);
		String raw = restClient.post()
				.uri("/generate")
				.contentType(MediaType.APPLICATION_JSON)
				.body(payload)
				.retrieve()
				.body(String.class);
		JsonNode data = requireOkEnvelope(raw).get("data");
		if (data == null || !data.hasNonNull("taskId")) {
			throw new SunoApiException("SUNO_PAYLOAD", "Suno generate 响应缺少 data.taskId");
		}
		return data.get("taskId").asText();
	}

	public JsonNode fetchGenerateRecord(String sunoTaskId) throws SunoApiException {
		requireConfigured();
		String raw = restClient.get()
				.uri(uriBuilder ->
						uriBuilder.path("/generate/record-info").queryParam("taskId", sunoTaskId).build())
				.retrieve()
				.body(String.class);
		JsonNode data = requireOkEnvelope(raw).get("data");
		return data != null ? data : objectMapper.createObjectNode();
	}

	private void requireConfigured() throws SunoApiException {
		if (!isConfigured()) {
			throw new SunoApiException("SUNO_DISABLED", "未配置 melodify.suno.api-key");
		}
	}

	private String writeJson(JsonNode body) throws SunoApiException {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (Exception ex) {
			throw new SunoApiException("SUNO_JSON", "序列化生成请求失败", ex);
		}
	}

	/** code=200 时返回整段 envelope；否则抛错。 */
	private JsonNode requireOkEnvelope(String raw) throws SunoApiException {
		JsonNode envelope = parse(raw);
		int code = envelope.path("code").asInt(-1);
		if (code == 200) {
			return envelope;
		}
		String msg = envelope.path("msg").asText("Suno 调用失败");
		throw new SunoApiException(String.valueOf(code), msg);
	}

	private JsonNode parse(String raw) throws SunoApiException {
		try {
			return objectMapper.readTree(raw);
		} catch (Exception ex) {
			throw new SunoApiException("SUNO_JSON", "无法解析 Suno JSON 响应", ex);
		}
	}

	private static String trimTrailingSlash(String url) {
		if (url == null || url.isEmpty()) {
			return "";
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	@Getter
	public static final class SunoApiException extends Exception {
		private final String errorCode;

		public SunoApiException(String errorCode, String message) {
			super(message);
			this.errorCode = errorCode;
		}

		public SunoApiException(String errorCode, String message, Throwable cause) {
			super(message, cause);
			this.errorCode = errorCode;
		}
	}
}
