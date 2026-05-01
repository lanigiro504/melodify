package com.melodify.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicTask;
import com.melodify.security.SecurityUtils;
import com.melodify.service.MusicAssetService;
import com.melodify.service.MusicTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/**
 * C 端音乐成品：分页与详情按本人过滤；服务端回调或补数据时的写入仅管理员可走 {@code POST}。
 */
@RestController
@RequestMapping("/api/client/music-assets")
@RequiredArgsConstructor
public class ClientMusicAssetController {

	private final MusicAssetService musicAssetService;
	private final MusicTaskService musicTaskService;

	/**
	 * 前端轮询到任务成功后，凭业务 {@code music_task.task_id} 获取最新一条成品（用于播放链接）。
	 */
	@GetMapping("/by-business-task/{taskBizId}")
	public Result<MusicAsset> getFirstByBusinessTask(@PathVariable String taskBizId) {
		String biz = taskBizId == null ? "" : taskBizId.trim();
		if (!StringUtils.hasText(biz)) {
			throw new BizException(400, "taskBizId 不能为空");
		}
		Long userId = SecurityUtils.requireUserId();
		MusicTask task = musicTaskService.lambdaQuery()
				.eq(MusicTask::getTaskId, biz)
				.eq(MusicTask::getUserId, userId)
				.one();
		if (task == null) {
			throw new BizException(404, "任务不存在");
		}
		LambdaQueryWrapper<MusicAsset> q = new LambdaQueryWrapper<MusicAsset>()
				.eq(MusicAsset::getTaskId, task.getId())
				.eq(MusicAsset::getUserId, userId)
				.orderByDesc(MusicAsset::getCreateTime)
				.last("LIMIT 1");
		MusicAsset asset = musicAssetService.getOne(q, false);
		if (asset == null) {
			throw new BizException(404, "成品尚未就绪，请稍后重试");
		}
		return Result.success(asset);
	}

	@GetMapping("/page")
	public Result<IPage<MusicAsset>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		Long userId = SecurityUtils.requireUserId();
		LambdaQueryWrapper<MusicAsset> wrapper = new LambdaQueryWrapper<MusicAsset>()
				.eq(MusicAsset::getUserId, userId)
				.orderByDesc(MusicAsset::getCreateTime);
		return Result.success(musicAssetService.page(new Page<>(current, size), wrapper));
	}

	@GetMapping("/{id}")
	public Result<MusicAsset> getById(@PathVariable Long id) {
		MusicAsset asset = musicAssetService.getById(id);
		if (asset == null) {
			throw new BizException(404, "作品不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(asset.getUserId());
		return Result.success(asset);
	}

	/** 联调用途：通常为异步回调或运维写入一条资产；终端用户应由生成流程写入。 */
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Result<Boolean> create(@RequestBody MusicAsset body) {
		return Result.success(musicAssetService.save(body));
	}

	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody MusicAsset body) {
		MusicAsset existing = musicAssetService.getById(id);
		if (existing == null) {
			throw new BizException(404, "作品不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(existing.getUserId());
		body.setId(id);
		body.setUserId(existing.getUserId());
		body.setTaskId(existing.getTaskId());
		return Result.success(musicAssetService.updateById(body));
	}

	@DeleteMapping("/{id}")
	public Result<Boolean> remove(@PathVariable Long id) {
		MusicAsset existing = musicAssetService.getById(id);
		if (existing == null) {
			throw new BizException(404, "作品不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(existing.getUserId());
		return Result.success(musicAssetService.removeById(id));
	}
}
