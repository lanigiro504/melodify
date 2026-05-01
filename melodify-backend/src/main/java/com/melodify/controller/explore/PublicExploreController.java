package com.melodify.controller.explore;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.common.result.Result;
import com.melodify.model.vo.MusicExploreItemVO;
import com.melodify.service.PublicExploreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/explore")
@RequiredArgsConstructor
public class PublicExploreController {

	private final PublicExploreService publicExploreService;

	private static final long MAX_PAGE_SIZE = 48;

	@GetMapping("/assets")
	public Result<IPage<MusicExploreItemVO>> assets(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "12") long size) {
		long page = Math.max(1, current);
		long pageSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
		return Result.success(publicExploreService.pagePublicAssets(page, pageSize));
	}
}
