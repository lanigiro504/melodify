package com.melodify.service;

import com.melodify.common.exception.BizException;
import com.melodify.config.MelodifyAvatarStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 将头像写入本地目录，生成可经 {@link com.melodify.controller.media.AvatarFileController} 访问的稳定文件名。
 */
@Component
@RequiredArgsConstructor
public class AvatarStorageService {

	private static final Pattern SAFE_BASENAME = Pattern.compile("^\\d+_[0-9a-fA-F\\-]{36}\\.(jpg|jpeg|png|webp)$");

	private final MelodifyAvatarStorageProperties properties;

	/**
	 * @return 写入 {@code sys_user.avatar} 的相对路径，如 {@code /api/media/avatar/1_xxx.webp}
	 */
	public String storeAvatarForUser(long userId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BizException(400, "请选择图片文件");
		}
		long max = Math.max(1, properties.getMaxBytes());
		if (file.getSize() > max) {
			throw new BizException(400, "图片过大，请压缩后重试（上限 " + max / 1024 + " KB）");
		}
		String extWithDot = resolveExtension(file);
		if (!StringUtils.hasText(extWithDot)) {
			throw new BizException(400, "仅支持 JPG、PNG、WebP");
		}

		String basename = userId + "_" + UUID.randomUUID() + extWithDot;
		Path baseDir = Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
		try {
			Files.createDirectories(baseDir);
			Path target = baseDir.resolve(basename).normalize();
			if (!target.startsWith(baseDir)) {
				throw new BizException(400, "非法文件名");
			}
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (BizException e) {
			throw e;
		} catch (Exception e) {
			throw new BizException(500, "头像保存失败：" + e.getMessage());
		}

		String prefix = normalizePrefix(properties.getPublicUriPrefix());
		return prefix + "/" + basename;
	}

	/** 若旧值为本服务托管路径，则删除对应磁盘文件（静默忽略错误）。 */
	public void deleteManagedAvatarFileIfPresent(String avatarUrl) {
		if (!StringUtils.hasText(avatarUrl)) {
			return;
		}
		String trimmed = avatarUrl.trim();
		String prefix = normalizePrefix(properties.getPublicUriPrefix());
		if (!trimmed.startsWith(prefix + "/")) {
			return;
		}
		String basename = trimmed.substring(prefix.length() + 1);
		if (!SAFE_BASENAME.matcher(basename).matches()) {
			return;
		}
		Path baseDir = Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
		Path target = baseDir.resolve(basename).normalize();
		if (!target.startsWith(baseDir)) {
			return;
		}
		try {
			Files.deleteIfExists(target);
		} catch (Exception ignored) {
			/* 清理失败不影响主流程 */
		}
	}

	private static String normalizePrefix(String publicUriPrefix) {
		String p = publicUriPrefix == null ? "/api/media/avatar" : publicUriPrefix.strip();
		if (!p.startsWith("/")) {
			p = "/" + p;
		}
		while (p.endsWith("/")) {
			p = p.substring(0, p.length() - 1);
		}
		return p;
	}

	private String resolveExtension(MultipartFile file) {
		String ct = Optional.ofNullable(file.getContentType()).map(s -> s.toLowerCase(Locale.ROOT)).orElse("");
		String fromCt = extFromContentType(ct);
		if (StringUtils.hasText(fromCt)) {
			return fromCt;
		}
		return extFromOriginalFilename(file.getOriginalFilename());
	}

	private static String extFromContentType(String ct) {
		if (!StringUtils.hasText(ct)) {
			return null;
		}
		if (ct.startsWith("image/jpeg") || ct.equals("image/jpg")) {
			return ".jpg";
		}
		if (ct.equals("image/png")) {
			return ".png";
		}
		if (ct.equals("image/webp")) {
			return ".webp";
		}
		return null;
	}

	private static String extFromOriginalFilename(String original) {
		if (!StringUtils.hasText(original) || !original.contains(".")) {
			return null;
		}
		String e = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		return switch (e) {
			case "jpg", "jpeg" -> ".jpg";
			case "png" -> ".png";
			case "webp" -> ".webp";
			default -> null;
		};
	}
}
