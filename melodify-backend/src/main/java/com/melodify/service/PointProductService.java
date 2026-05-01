package com.melodify.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.PointProduct;

public interface PointProductService extends IService<PointProduct> {

	IPage<PointProduct> pageForAdmin(Page<PointProduct> page, Integer status);
}
