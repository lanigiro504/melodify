package com.melodify.controller.callback;

import com.melodify.common.result.Result;
import com.melodify.model.dto.SimulatedPayNotifyDTO;
import com.melodify.model.vo.RechargeOrderVO;
import com.melodify.service.RechargeOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/simulated")
@RequiredArgsConstructor
public class SimulatedPaymentNotifyController {

	private final RechargeOrderService rechargeOrderService;

	@PostMapping("/notify")
	public Result<RechargeOrderVO> notify(@Valid @RequestBody SimulatedPayNotifyDTO dto) {
		return Result.success(rechargeOrderService.handleSimulatedNotify(dto));
	}
}
