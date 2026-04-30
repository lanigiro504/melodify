package com.melodify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("point_log")
public class PointLog {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 日志编号，对应列 log_id
	 */
	private String logId;

	private Long userId;

	private Integer changeType;

	private Integer amount;

	private Integer balance;

	private String bizType;

	private String bizId;

	private String remark;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;
}
