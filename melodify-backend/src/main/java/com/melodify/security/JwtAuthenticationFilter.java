package com.melodify.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodify.common.result.Result;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 从请求头读取 {@code Authorization: Bearer …}，校验 JWT 后放入 {@link SecurityContextHolder}。
 * <p>若首部存在 Bearer 但验签失败，直接写回 HTTP 200 + {@link Result} 且 {@code code=401}，
 * 与同项目 {@link com.melodify.common.exception.GlobalExceptionHandler} 的 JSON 约定一致，
 * 便于前端用统一 unwrap 解析业务码。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(auth) || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = auth.substring(7).trim();
		if (!StringUtils.hasText(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			MelodifyUserPrincipal principal = jwtService.toPrincipal(jwtService.parseClaims(token));
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (JwtException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
			response.setStatus(HttpServletResponse.SC_OK);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			String body = objectMapper.writeValueAsString(Result.error(
					org.springframework.http.HttpStatus.UNAUTHORIZED.value(),
					"令牌无效或已过期"));
			response.getWriter().write(body);
		}
	}
}
