package com.melodify.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.MusicAsset;
import com.melodify.service.MusicAssetService;
import com.melodify.support.PagingNormalize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：音乐成品资产查询与运营筛选。
 */
@RestController
@RequestMapping("/api/admin/music-assets")
@RequiredArgsConstructor
public class AdminMusicAssetController {

	private final MusicAssetService musicAssetService;

	@GetMapping("/page")
	public Result<IPage<MusicAsset>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Integer isPublic) {
		return Result.success(
				musicAssetService.pageForAdmin(PagingNormalize.page(current, size), userId, isPublic));
	}

	@GetMapping("/{id}")
	public Result<MusicAsset> getById(@PathVariable Long id) {
		MusicAsset row = musicAssetService.getById(id);
		if (row == null) {
			throw new BizException(404, "作品不存在");
		}
		return Result.success(row);
	}
}
