package com.melodify.security;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 与用户端存量逻辑一致的口令编码（当前为 MD5 UTF-8 十六进制小写）。
 * <p>
 * 集中在此便于后续升级为 BCrypt/Argon2 时只改一处接入；业务层勿直接散落 {@link DigestUtils} 调用。</p>
 */
public final class LegacyPasswordCodec {

	private LegacyPasswordCodec() {}

	public static String md5HexUtf8(String rawPlaintext) {
		if (rawPlaintext == null) {
			return DigestUtils.md5DigestAsHex("".getBytes(StandardCharsets.UTF_8));
		}
		return DigestUtils.md5DigestAsHex(rawPlaintext.getBytes(StandardCharsets.UTF_8));
	}
}
