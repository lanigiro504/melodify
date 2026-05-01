package com.melodify.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.entity.SysUser;

import java.util.List;

/**
 * 系统用户对外展示时的敏感字段处理（如不返回口令摘要）。
 */
public final class SysUserSecrets {

	private SysUserSecrets() {}

	public static void maskPassword(SysUser user) {
		if (user != null) {
			user.setPassword(null);
		}
	}

	public static void maskPasswordBatch(Iterable<SysUser> rows) {
		if (rows == null) {
			return;
		}
		for (SysUser u : rows) {
			maskPassword(u);
		}
	}

	public static void maskPasswordPage(IPage<SysUser> page) {
		if (page == null || page.getRecords() == null) {
			return;
		}
		maskPasswordBatch(page.getRecords());
	}

	public static void maskPasswordList(List<SysUser> rows) {
		maskPasswordBatch(rows);
	}
}
