package com.melodify.service.impl;

import com.melodify.config.MelodifyAudioStorageProperties;
import com.melodify.service.AudioMirrorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioMirrorServiceImpl implements AudioMirrorService {

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();

	private final MelodifyAudioStorageProperties props;

	/**
	 * 若启用本地镜像：GET 远端 MP3 写入 {@code local-dir}，返回 {@code public-uri-prefix} 下的可访问路径；
	 * 任一步失败或非 http(s) URL 则<strong>原样返回</strong> {@code remoteUrl}，不阻断业务。
	 */
	@Override
	public String mirrorRemoteToLocalIfEnabled(String remoteUrl, String assetBizId) {
		if (!props.isEnabled() || !StringUtils.hasText(remoteUrl) || !StringUtils.hasText(assetBizId)) {
			return remoteUrl;
		}
		String u = remoteUrl.strip();
		String lower = u.toLowerCase();
		if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
			return remoteUrl;
		}
		Path base = Paths.get(props.getLocalDir()).toAbsolutePath().normalize();
		try {
			Files.createDirectories(base);
			String fileName = sanitizeFileName(assetBizId);
			if (!StringUtils.hasText(fileName)) {
				return remoteUrl;
			}
			Path file = base.resolve(fileName + ".mp3").normalize();
			if (!file.startsWith(base)) {
				return remoteUrl;
			}
			HttpRequest req = HttpRequest.newBuilder(URI.create(u))
					.GET()
					.timeout(Duration.ofSeconds(120))
					.build();
			HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
			if (res.statusCode() / 100 != 2) {
				log.warn("音频镜像下载非 2xx: status={} url={}", res.statusCode(), u);
				return remoteUrl;
			}
			Files.write(file, res.body(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			String prefix = props.getPublicUriPrefix().strip();
			if (prefix.endsWith("/")) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
			return prefix + "/" + fileName + ".mp3";
		} catch (Exception ex) {
			log.warn("音频镜像失败，保留远端 URL: {}", ex.toString());
			return remoteUrl;
		}
	}

	/** 只允许 32 位小写十六进制（与 {@link com.melodify.support.BizIds} 生成的 assetId 一致），防路径穿越与非法文件名。 */
	private static String sanitizeFileName(String assetBizId) {
		String s = assetBizId.strip().toLowerCase();
		if (!s.matches("[a-f0-9]{32}")) {
			return "";
		}
		return s;
	}
}
