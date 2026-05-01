package com.melodify.security;

import com.melodify.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 访问令牌签发与解析（对称 HMAC-SHA）。
 * <p>{@code secret} 须与网关/多端部署一致；生产环境必须用环境变量覆盖默认值。</p>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtProperties jwtProperties;

	/**
	 * 为用户签发访问令牌，{@code subject} 存用户主键，自定义声明 {@link MelodifyUserPrincipal#CLAIM_ROLE_KEY}
	 * 存角色标识（与 {@code sys_role.role_key} 对齐）。
	 */
	public String createAccessToken(Long userId, String roleKey) {
		long nowMillis = System.currentTimeMillis();
		long expMillis = nowMillis + jwtProperties.getExpirationHours() * 3600_000L;
		SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim(MelodifyUserPrincipal.CLAIM_ROLE_KEY, roleKey == null ? "" : roleKey)
				.issuedAt(new Date(nowMillis))
				.expiration(new Date(expMillis))
				.signWith(key)
				.compact();
	}

	/**
	 * 验签并解析 JWT，异常由调用方（过滤器）决定是否返回 401 业务包装。
	 */
	public Claims parseClaims(String token) throws JwtException {
		SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * Claims → Spring Security 可用的 {@link MelodifyUserPrincipal}。
	 */
	public MelodifyUserPrincipal toPrincipal(Claims claims) {
		Long userId = Long.parseLong(claims.getSubject());
		String rk = claims.get(MelodifyUserPrincipal.CLAIM_ROLE_KEY, String.class);
		return new MelodifyUserPrincipal(userId, rk);
	}
}
