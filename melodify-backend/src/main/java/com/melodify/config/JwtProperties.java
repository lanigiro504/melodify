package com.melodify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 签发配置（绑定 {@code application.yml} 中前缀 {@code melodify.security.jwt}）。
 *
 * @see com.melodify.security.JwtService
 */
@Data
@ConfigurationProperties(prefix = "melodify.security.jwt")
public class JwtProperties {

	/** HMAC 对称密钥字节来源；长度不足时 JJWE 可能在启动期报错，线上务必改用强随机密钥。 */
	private String secret = "change-me";

	/** 访问令牌有效期（小时）。 */
	private int expirationHours = 12;
}
