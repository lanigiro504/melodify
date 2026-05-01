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
@TableName(value = "payment_notify_log", autoResultMap = true)
public class PaymentNotifyLog {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String notifyId;

	private String orderNo;

	private String signature;

	@TableField(value = "payload", typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> payload;

	private Integer status;

	private String message;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;
}
