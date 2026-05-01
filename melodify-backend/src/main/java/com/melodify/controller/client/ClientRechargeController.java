package com.melodify.controller.client;

import com.melodify.common.result.Result;
import com.melodify.entity.PointProduct;
import com.melodify.model.dto.CreateRechargeOrderDTO;
import com.melodify.model.dto.SimulatedPayNotifyDTO;
import com.melodify.model.vo.RechargeOrderVO;
import com.melodify.security.SecurityUtils;
import com.melodify.service.PointProductService;
import com.melodify.service.RechargeOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/recharge")
@RequiredArgsConstructor
public class ClientRechargeController {

	private final PointProductService pointProductService;
	private final RechargeOrderService rechargeOrderService;

	@GetMapping("/products")
	public Result<List<PointProduct>> products() {
		List<PointProduct> rows = pointProductService.lambdaQuery()
				.eq(PointProduct::getStatus, 1)
				.orderByAsc(PointProduct::getSortOrder)
				.list();
		return Result.success(rows);
	}

	@PostMapping("/orders")
	public Result<RechargeOrderVO> createOrder(@Valid @RequestBody CreateRechargeOrderDTO dto) {
		return Result.success(rechargeOrderService.createOrder(SecurityUtils.requireUserId(), dto.getProductId()));
	}

	@GetMapping("/orders/{orderNo}/simulated-notify")
	public Result<SimulatedPayNotifyDTO> simulatedNotifyPayload(@PathVariable String orderNo) {
		return Result.success(rechargeOrderService.buildSimulatedNotify(orderNo, SecurityUtils.requireUserId()));
	}
}
