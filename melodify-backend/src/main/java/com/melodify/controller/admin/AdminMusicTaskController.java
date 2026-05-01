package com.melodify.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.MusicTask;
import com.melodify.service.MusicTaskService;
import com.melodify.support.PagingNormalize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：全站音乐生成任务查询（不过滤 userId）。
 */
@RestController
@RequestMapping("/api/admin/music-tasks")
@RequiredArgsConstructor
public class AdminMusicTaskController {

	private final MusicTaskService musicTaskService;

	@GetMapping("/page")
	public Result<IPage<MusicTask>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Integer status) {
		return Result.success(
				musicTaskService.pageForAdmin(PagingNormalize.page(current, size), userId, status));
	}

	@GetMapping("/{id}")
	public Result<MusicTask> getById(@PathVariable Long id) {
		MusicTask row = musicTaskService.getById(id);
		if (row == null) {
			throw new BizException(404, "任务不存在");
		}
		return Result.success(row);
	}
}
