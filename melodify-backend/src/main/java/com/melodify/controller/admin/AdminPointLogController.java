package com.melodify.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.PointLog;
import com.melodify.service.PointLogService;
import com.melodify.support.PagingNormalize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：积分流水审计。
 */
@RestController
@RequestMapping("/api/admin/point-logs")
@RequiredArgsConstructor
public class AdminPointLogController {

	private final PointLogService pointLogService;

	@GetMapping("/page")
	public Result<IPage<PointLog>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) String bizType) {
		return Result.success(
				pointLogService.pageForAdmin(PagingNormalize.page(current, size), userId, bizType));
	}

	@GetMapping("/{id}")
	public Result<PointLog> getById(@PathVariable Long id) {
		PointLog row = pointLogService.getById(id);
		if (row == null) {
			throw new BizException(404, "记录不存在");
		}
		return Result.success(row);
	}
}
