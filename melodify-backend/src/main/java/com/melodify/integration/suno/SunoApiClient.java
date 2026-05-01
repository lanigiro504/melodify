package com.melodify.integration.suno;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodify.config.SunoApiProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * SunoAPI（api.sunoapi.org）出站调用：避免注入全局 {@link RestClient.Builder} 与其它组件产生 IDE/容器歧义。
 */
@Component
@RequiredArgsConstructor
public class SunoApiClient {

	private final SunoApiProperties sunoApiProperties;
	private final ObjectMapper objectMapper;

	/** Suno 请求单独设置较长读超时，独立于其它 HTTP 出站组件。 */
	private final SimpleClientHttpRequestFactory sunoRequestFactory = buildRequestFactory();

	private static SimpleClientHttpRequestFactory buildRequestFactory() {
		SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
		rf.setConnectTimeout(30_000);
		rf.setReadTimeout(120_000);
		return rf;
	}

	private RestClient client() {
		return RestClient.builder()
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
		if (!isConfigured()) {
			throw new SunoApiException("SUNO_DISABLED", "未配置 melodify.suno.api-key");
		}
		String payload;
		try {
			payload = objectMapper.writeValueAsString(body);
		} catch (Exception ex) {
			throw new SunoApiException("SUNO_JSON", "序列化生成请求失败", ex);
		}
		String raw = client()
				.post()
				.uri("/generate")
				.contentType(MediaType.APPLICATION_JSON)
				.body(payload)
				.retrieve()
				.body(String.class);
		JsonNode envelope = parse(raw);
		int code = envelope.path("code").asInt(-1);
		if (code != 200) {
			String msg = envelope.path("msg").asText("Suno generate 调用失败");
			throw new SunoApiException(String.valueOf(code), msg);
		}
		JsonNode data = envelope.get("data");
		if (data == null || !data.hasNonNull("taskId")) {
			throw new SunoApiException("SUNO_PAYLOAD", "Suno generate 响应缺少 data.taskId");
		}
		return data.get("taskId").asText();
	}

	public JsonNode fetchGenerateRecord(String sunoTaskId) throws SunoApiException {
		if (!isConfigured()) {
			throw new SunoApiException("SUNO_DISABLED", "未配置 melodify.suno.api-key");
		}
		String raw = client()
				.get()
				.uri(uriBuilder ->
						uriBuilder.path("/generate/record-info").queryParam("taskId", sunoTaskId).build())
				.retrieve()
				.body(String.class);
		JsonNode envelope = parse(raw);
		int code = envelope.path("code").asInt(-1);
		if (code != 200) {
			throw new SunoApiException(String.valueOf(code),
					envelope.path("msg").asText("Suno record-info 调用失败"));
		}
		JsonNode data = envelope.get("data");
		return data != null ? data : objectMapper.createObjectNode();
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
