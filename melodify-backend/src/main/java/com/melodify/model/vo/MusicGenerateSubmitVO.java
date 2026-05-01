package com.melodify.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code POST /generate} 的即时应答：前端用 {@link #businessTaskId} 轮询，勿暴露数据库主键亦可。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicGenerateSubmitVO {

	/** 列 {@code music_task.task_id}。 */
	private String businessTaskId;

	/** 任务刚落库时的状态（通常为生成中）。 */
	private Integer initialStatus;

	/** 本次扣减积分数量，便于前端展示计费结果。 */
	private Integer costPoints;
}
