package com.melodify.support;

import java.util.UUID;

/** 去除连字符的 UUID，满足部分列 varchar(64) 长度且无业务可读性要求的外显编号生成。 */
public final class BizIds {

	private BizIds() {}

	public static String uuidCompact() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
