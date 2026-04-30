package com.melodify.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.MusicAsset;
import com.melodify.mapper.MusicAssetMapper;
import com.melodify.service.MusicAssetService;
import org.springframework.stereotype.Service;

@Service
public class MusicAssetServiceImpl extends ServiceImpl<MusicAssetMapper, MusicAsset> implements MusicAssetService {
}
