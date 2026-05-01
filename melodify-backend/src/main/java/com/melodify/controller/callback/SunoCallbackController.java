package com.melodify.controller.callback;

import com.fasterxml.jackson.databind.JsonNode;
import com.melodify.config.SunoApiProperties;
import com.melodify.integration.suno.SunoCallbackProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/callback/suno")
@RequiredArgsConstructor
@Slf4j
public class SunoCallbackController {

	private final SunoCallbackProcessor sunoCallbackProcessor;
	private final SunoApiProperties sunoApiProperties;

	@PostMapping
	public ResponseEntity<Map<String, String>> receive(
			@RequestBody JsonNode body,
			@RequestParam(value = "token", required = false) String token) {
		if (StringUtils.hasText(sunoApiProperties.getCallbackToken())
				&& !sunoApiProperties.getCallbackToken().equals(token)) {
			log.warn("Suno 回调 token 校验失败");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "forbidden"));
		}
		sunoCallbackProcessor.processCallbackAsync(body);
		return ResponseEntity.ok(Map.of("status", "received"));
	}
}
