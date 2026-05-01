package com.melodify.model.vo;

import com.melodify.entity.RechargeOrder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class RechargeOrderVO {
	String orderNo;
	String productName;
	Integer points;
	Integer amountCent;
	Integer status;
	LocalDateTime paidAt;
	LocalDateTime createTime;

	public static RechargeOrderVO from(RechargeOrder order) {
		return new RechargeOrderVO(order.getOrderNo(), order.getProductName(), order.getPoints(),
				order.getAmountCent(), order.getStatus(), order.getPaidAt(), order.getCreateTime());
	}
}
