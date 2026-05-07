package com.melodify.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册 C 端通知 WebSocket；与 REST 分路径，握手阶段不在 Spring Security 中复用 Bearer Header。
 *
 * @see MelodifyClientWebSocketHandler
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class MelodifyWebSocketConfig implements WebSocketConfigurer {

	static final String NOTIFICATION_PATH = "/ws/notifications";

	private final MelodifyClientWebSocketHandler melodifyClientWebSocketHandler;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(melodifyClientWebSocketHandler, NOTIFICATION_PATH)
				.setAllowedOriginPatterns("*");
	}
}
