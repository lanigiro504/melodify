package com.melodify.support;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数规整：校正页码/条数上限，避免一次拉取过大损坏数据库与网关。
 */
public final class PagingNormalize {

	private PagingNormalize() {}

	public static final int DEFAULT_PAGE_SIZE = 10;
	public static final int MAX_PAGE_SIZE = 100;

	public static <T> Page<T> page(long current, long size) {
		long c = current < 1 ? 1 : current;
		long s = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return new Page<>(c, s);
	}
}
