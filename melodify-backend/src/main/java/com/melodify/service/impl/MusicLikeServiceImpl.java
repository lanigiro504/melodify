package com.melodify.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.MusicLike;
import com.melodify.mapper.MusicLikeMapper;
import com.melodify.service.MusicLikeService;
import org.springframework.stereotype.Service;

@Service
public class MusicLikeServiceImpl extends ServiceImpl<MusicLikeMapper, MusicLike>
		implements MusicLikeService {
}
