package com.melodify.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.config.MusicGenerationProperties;
import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicLike;
import com.melodify.entity.MusicTask;
import com.melodify.integration.suno.MusicSunoGenerationRunner;
import com.melodify.model.dto.MusicAssetPublicPatchDTO;
import com.melodify.model.vo.MusicAssetDetailVO;
import com.melodify.security.SecurityUtils;
import com.melodify.service.MusicAssetService;
import com.melodify.service.MusicLikeService;
import com.melodify.service.MusicTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
	private final MusicLikeService musicLikeService;
	private final MusicGenerationProperties musicGenerationProperties;
	private final MusicSunoGenerationRunner musicSunoGenerationRunner;

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
		MusicTask task = getOwnedTaskByBizId(biz, userId);
		LambdaQueryWrapper<MusicAsset> latestAssetQuery = latestAssetQuery(task.getId(), userId);
		MusicAsset asset = musicAssetService.getOne(latestAssetQuery, false);
		if (shouldTrySyncSunoAsset(task, asset)) {
			musicSunoGenerationRunner.trySyncCompletedAsset(task.getId());
			asset = musicAssetService.getOne(latestAssetQuery, false);
		}
		if (asset == null) {
			throw new BizException(404, "成品尚未就绪，请稍后重试");
		}
		return Result.success(asset);
	}

	private MusicTask getOwnedTaskByBizId(String biz, Long userId) {
		MusicTask task = musicTaskService.lambdaQuery()
				.eq(MusicTask::getTaskId, biz)
				.eq(MusicTask::getUserId, userId)
				.one();
		if (task == null) {
			throw new BizException(404, "任务不存在");
		}
		return task;
	}

	private static LambdaQueryWrapper<MusicAsset> latestAssetQuery(Long taskPk, Long userId) {
		return new LambdaQueryWrapper<MusicAsset>()
				.eq(MusicAsset::getTaskId, taskPk)
				.eq(MusicAsset::getUserId, userId)
				.orderByDesc(MusicAsset::getCreateTime)
				.last("LIMIT 1");
	}

	private boolean shouldTrySyncSunoAsset(MusicTask task, MusicAsset asset) {
		if (task == null || !StringUtils.hasText(task.getVendorTaskId())) {
			return false;
		}
		if (asset == null) {
			return true;
		}
		String placeholder = musicGenerationProperties.getPlaceholderAudioUrl();
		return StringUtils.hasText(placeholder) && placeholder.equals(asset.getFileUrl());
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
	public Result<MusicAssetDetailVO> getById(@PathVariable Long id) {
		MusicAsset asset = musicAssetService.getById(id);
		if (asset == null) {
			throw new BizException(404, "作品不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(asset.getUserId());
		MusicTask task = musicTaskService.getById(asset.getTaskId());
		Long userId = SecurityUtils.requireUserId();
		long likeCount = musicLikeService.lambdaQuery().eq(MusicLike::getAssetId, asset.getId()).count();
		boolean liked = musicLikeService.lambdaQuery()
				.eq(MusicLike::getAssetId, asset.getId())
				.eq(MusicLike::getUserId, userId)
				.exists();
		return Result.success(new MusicAssetDetailVO(asset, task, likeCount, liked));
	}

	@PostMapping("/{id}/like")
	public Result<Boolean> like(@PathVariable Long id) {
		MusicAsset asset = musicAssetService.getById(id);
		if (asset == null) {
			throw new BizException(404, "作品不存在");
		}
		Long userId = SecurityUtils.requireUserId();
		if (musicLikeService.lambdaQuery().eq(MusicLike::getAssetId, id).eq(MusicLike::getUserId, userId).exists()) {
			return Result.success(true);
		}
		MusicLike like = new MusicLike();
		like.setAssetId(id);
		like.setUserId(userId);
		return Result.success(musicLikeService.save(like));
	}

	@DeleteMapping("/{id}/like")
	public Result<Boolean> unlike(@PathVariable Long id) {
		Long userId = SecurityUtils.requireUserId();
		boolean removed = musicLikeService.lambdaUpdate()
				.eq(MusicLike::getAssetId, id)
				.eq(MusicLike::getUserId, userId)
				.remove();
		return Result.success(removed);
	}

	/** 联调用途：通常为异步回调或运维写入一条资产；终端用户应由生成流程写入。 */
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Result<Boolean> create(@RequestBody MusicAsset body) {
		return Result.success(musicAssetService.save(body));
	}

	@PatchMapping("/{id}/public")
	public Result<Boolean> patchPublic(@PathVariable Long id, @Valid @RequestBody MusicAssetPublicPatchDTO dto) {
		MusicAsset existing = musicAssetService.getById(id);
		if (existing == null) {
			throw new BizException(404, "作品不存在");
		}
		SecurityUtils.requireOwnershipOrAdmin(existing.getUserId());
		int v = dto.getIsPublic() != null && dto.getIsPublic() != 0 ? 1 : 0;
		return Result.success(musicAssetService.lambdaUpdate()
				.eq(MusicAsset::getId, id)
				.set(MusicAsset::getIsPublic, v)
				.update());
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
