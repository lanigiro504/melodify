package com.melodify.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.result.Result;
import com.melodify.entity.PointLog;
import com.melodify.service.PointLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端：积分变动日志。路径前缀固定为 {@code /api/client/**}。
 * <p>
 * 用于展示用户积分流水（充值、消费、退款等）。积分余额以 {@link com.melodify.entity.SysUser#getPoints()} 为准，
 * 流水表用于审计与对账；实际扣费逻辑应在事务中同时更新余额与插入一条日志。
 * </p>
 * <p>
 * 写入接口一般仅由服务端内部或管理端调用，此处暴露 {@code POST} 便于联调；上线时可收窄为内部服务或消息消费。
 * </p>
 */
@RestController
@RequestMapping("/api/client/point-logs")
@RequiredArgsConstructor
public class ClientPointLogController {

	private final PointLogService pointLogService;

	/**
	 * 分页查询某用户的积分流水，按创建时间倒序。
	 */
	@GetMapping("/page")
	public Result<IPage<PointLog>> page(
			@RequestParam Long userId,
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		LambdaQueryWrapper<PointLog> wrapper = new LambdaQueryWrapper<PointLog>()
				.eq(PointLog::getUserId, userId)
				.orderByDesc(PointLog::getCreateTime);
		return Result.success(pointLogService.page(new Page<>(current, size), wrapper));
	}

	/**
	 * 按主键查询单条流水。
	 */
	@GetMapping("/{id}")
	public Result<PointLog> getById(@PathVariable Long id) {
		return Result.success(pointLogService.getById(id));
	}

	/**
	 * 新增积分流水（联调/管理用）。生产环境应在与余额更新相同的事务内调用，避免不一致。
	 */
	@PostMapping
	public Result<Boolean> create(@RequestBody PointLog body) {
		return Result.success(pointLogService.save(body));
	}
}
