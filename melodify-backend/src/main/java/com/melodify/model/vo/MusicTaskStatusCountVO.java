package com.melodify.model.vo;

/** 管理端仪表盘：当前库内各状态任务数量（用于饼图）。 */
public record MusicTaskStatusCountVO(int status, long count) {}
