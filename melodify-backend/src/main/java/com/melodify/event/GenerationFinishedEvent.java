package com.melodify.event;

/**
 * AI 生成任务在用户维度上的终态（成功写库成品 / 标记失败）。
 * <p>
 * 建议在事务提交后再推送给前端（{@code @TransactionalEventListener(AFTER_COMMIT)}），避免前端收到消息瞬间读库仍为旧状态。
 * </p>
 */
public record GenerationFinishedEvent(long userId, String taskBizId, boolean success, String titlePreview,
		String errorMessage) {}
