package com.melodify.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.PointProduct;
import com.melodify.mapper.PointProductMapper;
import com.melodify.service.PointProductService;
import org.springframework.stereotype.Service;

@Service
public class PointProductServiceImpl extends ServiceImpl<PointProductMapper, PointProduct>
		implements PointProductService {
}
