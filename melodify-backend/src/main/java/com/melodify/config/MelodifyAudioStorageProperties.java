package com.melodify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可选：将远程 MP3 镜像到本地目录，并通过 {@code /api/media/audio} 对外提供播放地址。
 */
@Data
@ConfigurationProperties(prefix = "melodify.audio-storage")
public class MelodifyAudioStorageProperties {

	/** 启用后，仅在 URL 为 http(s) 时下载到本地 */
	private boolean enabled = false;

	/** 存储目录（相对路径则相对工作目录） */
	private String localDir = "data/melodify-audio";

	/** 写入 {@code music_asset.file_url} 的对外前缀，如 /api/media/audio */
	private String publicUriPrefix = "/api/media/audio";
}
