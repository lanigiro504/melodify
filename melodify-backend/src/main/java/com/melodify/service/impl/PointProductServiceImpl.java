package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.PointProduct;
import com.melodify.mapper.PointProductMapper;
import com.melodify.service.PointProductService;
import org.springframework.stereotype.Service;

@Service
public class PointProductServiceImpl extends ServiceImpl<PointProductMapper, PointProduct>
		implements PointProductService {

	@Override
	public IPage<PointProduct> pageForAdmin(Page<PointProduct> page, Integer status) {
		LambdaQueryWrapper<PointProduct> q = new LambdaQueryWrapper<>();
		if (status != null) {
			q.eq(PointProduct::getStatus, status);
		}
		q.orderByAsc(PointProduct::getSortOrder).orderByDesc(PointProduct::getId);
		return page(page, q);
	}
}
