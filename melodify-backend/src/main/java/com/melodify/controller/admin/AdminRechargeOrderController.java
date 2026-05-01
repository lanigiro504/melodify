package com.melodify.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.RechargeOrder;
import com.melodify.service.RechargeOrderService;
import com.melodify.support.PagingNormalize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：充值订单查询。
 */
@RestController
@RequestMapping("/api/admin/recharge-orders")
@RequiredArgsConstructor
public class AdminRechargeOrderController {

	private final RechargeOrderService rechargeOrderService;

	@GetMapping("/page")
	public Result<IPage<RechargeOrder>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Integer status) {
		return Result.success(
				rechargeOrderService.pageForAdmin(PagingNormalize.page(current, size), userId, status));
	}

	@GetMapping("/{id}")
	public Result<RechargeOrder> getById(@PathVariable Long id) {
		RechargeOrder row = rechargeOrderService.getById(id);
		if (row == null) {
			throw new BizException(404, "订单不存在");
		}
		return Result.success(row);
	}
}
