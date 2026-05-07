package com.melodify.controller.admin;

import com.melodify.common.result.Result;
import com.melodify.model.vo.AdminDashboardSummaryVO;
import com.melodify.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

	private final AdminDashboardService adminDashboardService;

	@GetMapping("/summary")
	public Result<AdminDashboardSummaryVO> summary() {
		return Result.success(adminDashboardService.summary());
	}
}
