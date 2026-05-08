package com.melodify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模拟支付回调 HMAC 配置（绑定 {@code melodify.payment.simulated}）。
 * <p>生产环境请用环境变量或密钥管理覆盖，禁止与线上真实支付网关密钥复用。</p>
 */
@Data
@ConfigurationProperties(prefix = "melodify.payment.simulated")
public class SimulatedPaymentProperties {

	/** HmacSHA256 密钥；需与生成 notify 时使用同一值。 */
	private String hmacSecret = "melodify-simulated-pay-secret";
}
