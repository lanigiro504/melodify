package com.melodify.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.MusicAsset;

public interface MusicAssetService extends IService<MusicAsset> {

	/** 管理端分页：可选按归属用户、公开标识过滤。 */
	IPage<MusicAsset> pageForAdmin(Page<MusicAsset> page, Long userId, Integer isPublic);
}
