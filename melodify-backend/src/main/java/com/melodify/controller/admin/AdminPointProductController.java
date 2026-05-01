package com.melodify.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.common.exception.BizException;
import com.melodify.common.result.Result;
import com.melodify.entity.PointProduct;
import com.melodify.service.PointProductService;
import com.melodify.support.PagingNormalize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：积分商品维护（含下架商品可见性）。
 */
@RestController
@RequestMapping("/api/admin/point-products")
@RequiredArgsConstructor
public class AdminPointProductController {

	private final PointProductService pointProductService;

	@GetMapping("/page")
	public Result<IPage<PointProduct>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size,
			@RequestParam(required = false) Integer status) {
		return Result.success(
				pointProductService.pageForAdmin(PagingNormalize.page(current, size), status));
	}

	@GetMapping("/{id}")
	public Result<PointProduct> getById(@PathVariable Long id) {
		PointProduct row = pointProductService.getById(id);
		if (row == null) {
			throw new BizException(404, "商品不存在");
		}
		return Result.success(row);
	}

	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody PointProduct body) {
		body.setId(id);
		return Result.success(pointProductService.updateById(body));
	}
}
