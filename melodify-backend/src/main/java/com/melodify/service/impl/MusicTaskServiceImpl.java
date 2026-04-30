package com.melodify.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.MusicTask;
import com.melodify.mapper.MusicTaskMapper;
import com.melodify.service.MusicTaskService;
import org.springframework.stereotype.Service;

@Service
public class MusicTaskServiceImpl extends ServiceImpl<MusicTaskMapper, MusicTask> implements MusicTaskService {
}
