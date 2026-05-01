package com.melodify.service;

/**
 * 将可访问的远程音频镜像到本地（可选），并返回应写入 {@code music_asset.file_url} 的最终播放地址。
 */
public interface AudioMirrorService {

	String mirrorRemoteToLocalIfEnabled(String remoteUrl, String assetBizId);
}
