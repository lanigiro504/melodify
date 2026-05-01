package com.melodify.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.MusicTask;
import com.melodify.model.dto.MusicGenerateRequestDTO;
import com.melodify.model.vo.MusicGenerateSubmitVO;
import com.melodify.security.SecurityUtils;
import com.melodify.service.MusicGenerationService;
import com.melodify.service.MusicTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端音乐生成任务：
 * <ul>
 *   <li>正式提交请走 {@link #generate(MusicGenerateRequestDTO)}（扣积分 + 异步处理）</li>
 *   <li>原始 CRUD POST 仅限管理员调试，防止绕过积分计费</li>
 *   <li>轮询请用业务编号 {@code task_id} 查询 {@link #getByBusinessTaskId(String)}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/client/music-tasks")
@RequiredArgsConstructor
public class ClientMusicTaskController {

	private final MusicTaskService musicTaskService;
	private final MusicGenerationService musicGenerationService;

	/** 分页：仅当前登录用户名下任务，按创建时间倒序。 */
	@GetMapping("/page")
	public Result<IPage<MusicTask>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		Long userId = SecurityUtils.requireUserId();
		LambdaQueryWrapper<MusicTask> wrapper = new LambdaQueryWrapper<MusicTask>()
				.eq(MusicTask::getUserId, userId)
				.orderByDesc(MusicTask::getCreateTime);
		return Result.success(musicTaskService.page(new Page<>(current, size), wrapper));
	}

	/** 详情：校验任务归属（或管理员放行）。 */
	@GetMapping("/{id}")
	public Result<MusicTask> getById(@PathVariable Long id) {
		MusicTask task = musicTaskService.getById(id);
		if (task == null) {
			throw new BizException(404, "任务不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(task.getUserId());
		return Result.success(task);
	}

	/**
	 * 用户发起一次生成：单事务扣积分 + 写入任务占位，再在事务提交后异步模拟完成流水线。
	 */
	@PostMapping("/generate")
	public Result<MusicGenerateSubmitVO> generate(@Valid @RequestBody MusicGenerateRequestDTO dto) {
		Long userId = SecurityUtils.requireUserId();
		return Result.success(musicGenerationService.submitGeneration(userId, dto));
	}

	/**
	 * 供前端高频轮询：按列 {@code task_id}（非自增 id）返回任务快照。
	 */
	@GetMapping("/by-task-id/{taskId}")
	public Result<MusicTask> getByBusinessTaskId(@PathVariable String taskId) {
		if (!StringUtils.hasText(taskId)) {
			throw new BizException(400, "taskId 不能为空");
		}
		Long userId = SecurityUtils.requireUserId();
		MusicTask task = musicTaskService.lambdaQuery()
				.eq(MusicTask::getTaskId, taskId.trim())
				.eq(MusicTask::getUserId, userId)
				.one();
		if (task == null) {
			throw new BizException(404, "任务不存在");
		}
		return Result.success(task);
	}

	/** 手工插库仅限管理员（联调）；正常路径为 {@link #generate(MusicGenerateRequestDTO)}。 */
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Result<Boolean> create(@RequestBody MusicTask body) {
		if (!StringUtils.hasText(body.getTaskId())) {
			throw new BizException(400, "taskId 不能为空");
		}
		if (!StringUtils.hasText(body.getModelCode())) {
			throw new BizException(400, "modelCode 不能为空");
		}
		Long userId = body.getUserId() != null ? body.getUserId() : SecurityUtils.requireUserId();
		body.setUserId(userId);
		body.setId(null);
		return Result.success(musicTaskService.save(body));
	}

	/** 变更任务字段：写入方须为所有者或管理员。 */
	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody MusicTask body) {
		MusicTask existing = musicTaskService.getById(id);
		if (existing == null) {
			throw new BizException(404, "任务不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(existing.getUserId());
		body.setId(id);
		body.setUserId(existing.getUserId());
		return Result.success(musicTaskService.updateById(body));
	}

	/** 删除任务记录（物理删除或未启用逻辑删除时慎用）。 */
	@DeleteMapping("/{id}")
	public Result<Boolean> remove(@PathVariable Long id) {
		MusicTask existing = musicTaskService.getById(id);
		if (existing == null) {
			throw new BizException(404, "任务不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(existing.getUserId());
		return Result.success(musicTaskService.removeById(id));
	}
}
