package com.melodify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * C 端头像本地上传目录与限制；文件经 {@code GET /api/media/avatar/**} 匿名读取（仅允许安全文件名）。
 */
@Data
@ConfigurationProperties(prefix = "melodify.avatar-storage")
public class MelodifyAvatarStorageProperties {

	/** 存储根目录（相对路径则相对进程工作目录） */
	private String localDir = "data/melodify-avatars";

	/** 写入 {@code sys_user.avatar} 的对外路径前缀 */
	private String publicUriPrefix = "/api/media/avatar";

	/** 单文件最大字节数（默认 2MiB） */
	private long maxBytes = 2 * 1024 * 1024;
}
