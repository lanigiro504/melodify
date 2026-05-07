package com.melodify.websocket;

import com.melodify.event.GenerationFinishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后再推送 WebSocket，避免客户端收到消息后仍读到旧任务状态。
 */
@Component
@RequiredArgsConstructor
public class GenerationFinishedWsListener {

	private final ClientNotificationBroadcaster broadcaster;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onGenerationFinishedAfterCommit(GenerationFinishedEvent event) {
		broadcaster.broadcastGenerationFinished(event);
	}
}
