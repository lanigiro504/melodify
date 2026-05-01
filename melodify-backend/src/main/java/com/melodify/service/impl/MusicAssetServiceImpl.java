package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.MusicAsset;
import com.melodify.mapper.MusicAssetMapper;
import com.melodify.service.MusicAssetService;
import org.springframework.stereotype.Service;

@Service
public class MusicAssetServiceImpl extends ServiceImpl<MusicAssetMapper, MusicAsset> implements MusicAssetService {

	@Override
	public IPage<MusicAsset> pageForAdmin(Page<MusicAsset> page, Long userId, Integer isPublic) {
		LambdaQueryWrapper<MusicAsset> q = new LambdaQueryWrapper<>();
		if (userId != null) {
			q.eq(MusicAsset::getUserId, userId);
		}
		if (isPublic != null) {
			q.eq(MusicAsset::getIsPublic, isPublic);
		}
		q.orderByDesc(MusicAsset::getCreateTime);
		return page(page, q);
	}
}
