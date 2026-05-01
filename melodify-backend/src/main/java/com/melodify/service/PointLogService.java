package com.melodify.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.PointLog;

public interface PointLogService extends IService<PointLog> {

	/** 管理端分页：可选按用户、业务类型过滤。 */
	IPage<PointLog> pageForAdmin(Page<PointLog> page, Long userId, String bizType);
}
