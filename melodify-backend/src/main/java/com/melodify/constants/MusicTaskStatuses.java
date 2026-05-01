package com.melodify.constants;

/**
 * {@code music_task.status} 与库表注释保持一致，便于与异步 Worker 对齐状态机迁移。
 */
public final class MusicTaskStatuses {

	private MusicTaskStatuses() {}

	/** 0：排队等待（预留队列消费场景） */
	public static final int QUEUED = 0;

	/** 1：生成中 */
	public static final int GENERATING = 1;

	/** 2：成功 */
	public static final int SUCCEEDED = 2;

	/** 3：失败 */
	public static final int FAILED = 3;

	/** 4：取消 */
	public static final int CANCELLED = 4;
}
