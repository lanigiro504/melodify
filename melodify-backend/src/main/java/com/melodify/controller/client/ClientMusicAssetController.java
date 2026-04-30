package com.melodify.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.result.Result;
import com.melodify.entity.MusicAsset;
import com.melodify.service.MusicAssetService;
import lombok.RequiredArgsConstructor;
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
 * 用户端：生成音乐资产（成品音频与元数据）。路径前缀固定为 {@code /api/client/**}。
 * <p>
 * 一条 {@link com.melodify.entity.MusicTask} 成功完成后可对应一条或多条 {@link MusicAsset}（当前表结构为单任务关联，
 * 若未来一拖多，可扩展关联或在业务层拆表）。
 * </p>
 * <p>
 * 与任务接口相同：现阶段通过 {@code userId} 区分数据归属，后续应改为登录用户上下文。
 * </p>
 */
@RestController
@RequestMapping("/api/client/music-assets")
@RequiredArgsConstructor
public class ClientMusicAssetController {

	private final MusicAssetService musicAssetService;

	/**
	 * 分页查询某用户的音乐资产列表。
	 */
	@GetMapping("/page")
	public Result<IPage<MusicAsset>> page(
			@RequestParam Long userId,
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		LambdaQueryWrapper<MusicAsset> wrapper = new LambdaQueryWrapper<MusicAsset>()
				.eq(MusicAsset::getUserId, userId)
				.orderByDesc(MusicAsset::getCreateTime);
		return Result.success(musicAssetService.page(new Page<>(current, size), wrapper));
	}

	/**
	 * 根据主键查询资产配置。宜校验资源所属用户。
	 */
	@GetMapping("/{id}")
	public Result<MusicAsset> getById(@PathVariable Long id) {
		return Result.success(musicAssetService.getById(id));
	}

	/**
	 * 新增资产（通常在生成任务成功回调中写入）。{@code assetId} 为业务唯一编号，对应列 {@code asset_id}。
	 */
	@PostMapping
	public Result<Boolean> create(@RequestBody MusicAsset body) {
		return Result.success(musicAssetService.save(body));
	}

	/**
	 * 更新资产信息（标题、是否公开、授权类型等）。
	 */
	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody MusicAsset body) {
		body.setId(id);
		return Result.success(musicAssetService.updateById(body));
	}

	/**
	 * 删除资产记录。
	 */
	@DeleteMapping("/{id}")
	public Result<Boolean> remove(@PathVariable Long id) {
		return Result.success(musicAssetService.removeById(id));
	}
}
