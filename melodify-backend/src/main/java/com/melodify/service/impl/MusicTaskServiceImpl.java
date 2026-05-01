package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.MusicTask;
import com.melodify.mapper.MusicTaskMapper;
import com.melodify.service.MusicTaskService;
import org.springframework.stereotype.Service;

@Service
public class MusicTaskServiceImpl extends ServiceImpl<MusicTaskMapper, MusicTask> implements MusicTaskService {

	@Override
	public IPage<MusicTask> pageForAdmin(Page<MusicTask> page, Long userId, Integer status) {
		LambdaQueryWrapper<MusicTask> q = new LambdaQueryWrapper<>();
		if (userId != null) {
			q.eq(MusicTask::getUserId, userId);
		}
		if (status != null) {
			q.eq(MusicTask::getStatus, status);
		}
		q.orderByDesc(MusicTask::getCreateTime);
		return page(page, q);
	}
}
