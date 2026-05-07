package com.melodify.websocket;

import com.melodify.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Locale;

/**
 * C 端 WebSocket：{@code WS /ws/notifications?token=JWT}。<br/>
 * HTTP 升级为 WebSocket 时无法可靠携带自定义 Header（浏览器原生 API 不支持），因此在 query 中带 JWT，
 * Spring Security 链路对此路径 {@code permitAll}，在用户态由本类校验 token 并绑定会话。
 *
 * @see JwtService#parseClaims(String)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MelodifyClientWebSocketHandler extends TextWebSocketHandler {

	static final String TYPE_GENERATION_FINISHED = "GENERATION_FINISHED";

	private static final String ATTR_USER_ID = "MELODIFY_WS_USER_ID";

	private final JwtService jwtService;
	private final ClientNotificationBroadcaster broadcaster;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		Long userId = authenticate(session);
		if (userId == null) {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid_token"));
			return;
		}
		session.getAttributes().put(ATTR_USER_ID, userId);
		broadcaster.register(userId, session);
		log.debug("WebSocket 已连接 userId={} sessionId={}", userId, session.getId());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		/* 当前仅服务端推送，忽略客户端帧（可扩展为 ping/pong）。 */
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		Long userId = sessionAttrUserId(session);
		if (userId != null) {
			broadcaster.unregister(userId, session);
		}
	}

	private Long authenticate(WebSocketSession session) {
		URI uri = session.getUri();
		if (uri == null || !StringUtils.hasText(uri.getQuery())) {
			return null;
		}
		String token = extractBearerQueryParam(uri.getQuery());
		if (!StringUtils.hasText(token)) {
			return null;
		}
		try {
			return Long.parseLong(jwtService.parseClaims(token).getSubject());
		} catch (JwtException ex) {
			log.warn("WebSocket JWT 校验失败: {}", ex.getMessage());
			return null;
		} catch (RuntimeException ex) {
			log.warn("WebSocket JWT subject 无效: {}", ex.getMessage());
			return null;
		}
	}

	/**
	 * 解析形如 {@code token=xxx} 或 {@code a=1&token=xxx} 的 query；支持 {@code Bearer%20} 前缀。
	 */
	private static String extractBearerQueryParam(String rawQuery) {
		for (String part : rawQuery.split("&")) {
			int eq = part.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = decodeQuery(part.substring(0, eq)).trim().toLowerCase(Locale.ROOT);
			if ("token".equals(key)) {
				String v = decodeQuery(part.substring(eq + 1)).trim();
				if (v.length() >= 7 && v.regionMatches(true, 0, "Bearer ", 0, 7)) {
					return v.substring(7).trim();
				}
				return v;
			}
		}
		return "";
	}

	private static String decodeQuery(String s) {
		try {
			return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			return s;
		}
	}

	private static Long sessionAttrUserId(WebSocketSession session) {
		Object v = session.getAttributes().get(ATTR_USER_ID);
		return v instanceof Long l ? l : null;
	}
}
