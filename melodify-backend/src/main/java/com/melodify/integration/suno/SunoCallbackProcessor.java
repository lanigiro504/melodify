package com.melodify.integration.suno;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Suno 网关要求在 15s 内返回 200：HTTP 入口只做校验与入队，本类异步执行业务。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SunoCallbackProcessor {

	private final MusicSunoGenerationRunner musicSunoGenerationRunner;

	@Async("musicTaskExecutor")
	public void processCallbackAsync(JsonNode body) {
		try {
			musicSunoGenerationRunner.handleSunoHttpCallback(body);
		} catch (Exception ex) {
			log.error("Suno 异步回调处理异常", ex);
		}
	}
}
