package com.melodify.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.PointLog;
import com.melodify.security.SecurityUtils;
import com.melodify.service.PointLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端积分流水只读分页；单笔详情校验归属。
 * {@code POST} 保留给管理员或对账脚本，写入应与余额变动同事务（见服务端内部实现）。
 */
@RestController
@RequestMapping("/api/client/point-logs")
@RequiredArgsConstructor
public class ClientPointLogController {

	private final PointLogService pointLogService;

	@GetMapping("/page")
	public Result<IPage<PointLog>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		Long userId = SecurityUtils.requireUserId();
		LambdaQueryWrapper<PointLog> wrapper = new LambdaQueryWrapper<PointLog>()
				.eq(PointLog::getUserId, userId)
				.orderByDesc(PointLog::getCreateTime);
		return Result.success(pointLogService.page(new Page<>(current, size), wrapper));
	}

	@GetMapping("/{id}")
	public Result<PointLog> getById(@PathVariable Long id) {
		PointLog row = pointLogService.getById(id);
		if (row == null) {
			throw new BizException(404, "记录不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(row.getUserId());
		return Result.success(row);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Result<Boolean> create(@RequestBody PointLog body) {
		return Result.success(pointLogService.save(body));
	}
}
