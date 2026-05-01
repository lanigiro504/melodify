package com.melodify.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.RechargeOrder;
import com.melodify.model.dto.SimulatedPayNotifyDTO;
import com.melodify.model.vo.RechargeOrderVO;

public interface RechargeOrderService extends IService<RechargeOrder> {

	/**
	 * 管理端分页：可选按用户、订单状态筛选。
	 */
	IPage<RechargeOrder> pageForAdmin(Page<RechargeOrder> page, Long userId, Integer status);

	RechargeOrderVO createOrder(Long userId, Long productId);

	SimulatedPayNotifyDTO buildSimulatedNotify(String orderNo, Long userId);

	RechargeOrderVO handleSimulatedNotify(SimulatedPayNotifyDTO dto);
}
