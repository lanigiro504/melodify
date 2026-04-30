package com.melodify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.melodify.entity.MusicTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MusicTaskMapper extends BaseMapper<MusicTask> {
}
