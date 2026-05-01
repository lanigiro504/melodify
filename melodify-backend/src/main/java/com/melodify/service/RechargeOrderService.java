package com.melodify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.RechargeOrder;
import com.melodify.model.dto.SimulatedPayNotifyDTO;
import com.melodify.model.vo.RechargeOrderVO;

public interface RechargeOrderService extends IService<RechargeOrder> {

	RechargeOrderVO createOrder(Long userId, Long productId);

	SimulatedPayNotifyDTO buildSimulatedNotify(String orderNo, Long userId);

	RechargeOrderVO handleSimulatedNotify(SimulatedPayNotifyDTO dto);
}
