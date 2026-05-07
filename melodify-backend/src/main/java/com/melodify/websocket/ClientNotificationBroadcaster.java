package com.melodify.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodify.event.GenerationFinishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 向已建立 WebSocket 会话的 C 端用户推送 JSON 文本帧；同一用户多标签页对应多个 Session。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientNotificationBroadcaster {

	private final ObjectMapper objectMapper;

	private final ConcurrentHashMap<Long, CopyOnWriteArrayList<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

	public void register(long userId, WebSocketSession session) {
		CopyOnWriteArrayList<WebSocketSession> list = sessionsByUserId.computeIfAbsent(userId,
				k -> new CopyOnWriteArrayList<>());
		if (!list.contains(session)) {
			list.add(session);
		}
	}

	public void unregister(long userId, WebSocketSession session) {
		List<WebSocketSession> list = sessionsByUserId.get(userId);
		if (list == null) {
			return;
		}
		list.remove(session);
		if (list.isEmpty()) {
			sessionsByUserId.remove(userId, list);
		}
	}

	/**
	 * 将生成终态播报为结构化 JSON。
	 *
	 * @see MelodifyClientWebSocketHandler#TYPE_GENERATION_FINISHED
	 */
	public void broadcastGenerationFinished(GenerationFinishedEvent event) {
		try {
			String json = objectMapper.writeValueAsString(Map.of(
					"type", MelodifyClientWebSocketHandler.TYPE_GENERATION_FINISHED,
					"taskBizId", event.taskBizId() == null ? "" : event.taskBizId(),
					"success", event.success(),
					"titlePreview", event.titlePreview() == null ? "" : event.titlePreview(),
					"errorMessage", event.errorMessage() == null ? "" : event.errorMessage()));
			sendRawText(event.userId(), json);
		} catch (Exception ex) {
			log.warn("广播生成通知序列化失败 userId={} taskBizId={}", event.userId(), event.taskBizId(), ex);
		}
	}

	public void sendRawText(long userId, String utf8Payload) {
		List<WebSocketSession> list = sessionsByUserId.get(userId);
		if (list == null || list.isEmpty()) {
			return;
		}
		TextMessage tm = new TextMessage(utf8Payload);
		for (WebSocketSession s : List.copyOf(list)) {
			if (!s.isOpen()) {
				unregister(userId, s);
				continue;
			}
			try {
				synchronized (s) {
					s.sendMessage(tm);
				}
			} catch (IOException ex) {
				log.debug("推送 WebSocket 失败 userId={} session={}", userId, s.getId(), ex);
				unregister(userId, s);
			}
		}
	}
}
