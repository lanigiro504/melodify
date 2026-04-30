package com.melodify.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.PointLog;
import com.melodify.mapper.PointLogMapper;
import com.melodify.service.PointLogService;
import org.springframework.stereotype.Service;

@Service
public class PointLogServiceImpl extends ServiceImpl<PointLogMapper, PointLog> implements PointLogService {
}
