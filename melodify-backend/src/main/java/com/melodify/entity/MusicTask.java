package com.melodify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "music_task", autoResultMap = true)
public class MusicTask {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 任务编号（对外展示），对应列 task_id
	 */
	private String taskId;

	private Long userId;

	private String modelCode;

	private String prompt;

	@TableField(value = "params", typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> params;

	private Integer status;

	private String errorCode;

	private String errorMessage;

	private Integer costPoints;

	private LocalDateTime startedAt;

	private LocalDateTime finishedAt;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;
}
