package com.melodify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recharge_order")
public class RechargeOrder {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String orderNo;

	private Long userId;

	private Long productId;

	private String productName;

	private Integer points;

	private Integer amountCent;

	private Integer status;

	private LocalDateTime paidAt;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;
}
