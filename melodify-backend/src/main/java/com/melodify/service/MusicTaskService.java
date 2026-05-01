package com.melodify.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.MusicTask;

public interface MusicTaskService extends IService<MusicTask> {

	/** 管理端分页：可选按用户主键、任务状态过滤。 */
	IPage<MusicTask> pageForAdmin(Page<MusicTask> page, Long userId, Integer status);
}
