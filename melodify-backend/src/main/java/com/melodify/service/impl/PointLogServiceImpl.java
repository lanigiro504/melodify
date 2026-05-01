package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.PointLog;
import com.melodify.mapper.PointLogMapper;
import com.melodify.service.PointLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointLogServiceImpl extends ServiceImpl<PointLogMapper, PointLog> implements PointLogService {

	@Override
	public IPage<PointLog> pageForAdmin(Page<PointLog> page, Long userId, String bizType) {
		LambdaQueryWrapper<PointLog> q = new LambdaQueryWrapper<>();
		if (userId != null) {
			q.eq(PointLog::getUserId, userId);
		}
		if (StringUtils.hasText(bizType)) {
			q.eq(PointLog::getBizType, bizType.trim());
		}
		q.orderByDesc(PointLog::getCreateTime);
		return page(page, q);
	}
}
