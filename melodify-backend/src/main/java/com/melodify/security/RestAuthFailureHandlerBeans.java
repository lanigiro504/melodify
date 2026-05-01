package com.melodify.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodify.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 将 Spring Security 的「未登录 / 无权限」转成与业务一致的 HTTP 200 + {@link Result}
 * （业务码区分 401/403），保持与 {@link JwtAuthenticationFilter} 失败响应形态统一。
 */
@Configuration
@RequiredArgsConstructor
public class RestAuthFailureHandlerBeans {

	private final ObjectMapper objectMapper;

	@Bean
	public AuthenticationEntryPoint restAuthenticationEntryPoint() {
		return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
				-> writeJson(response, HttpStatus.UNAUTHORIZED.value(), "未登录或登录已失效");
	}

	@Bean
	public AccessDeniedHandler restAccessDeniedHandler() {
		return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
				-> writeJson(response, HttpStatus.FORBIDDEN.value(), "权限不足");
	}

	private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
		response.setStatus(HttpStatus.OK.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		String body = objectMapper.writeValueAsString(Result.error(code, message));
		response.getWriter().write(body);
	}
}
