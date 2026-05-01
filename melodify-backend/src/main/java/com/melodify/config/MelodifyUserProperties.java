package com.melodify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * C 端用户相关运营参数：注册赠送积分等（正式充值上线后可调低或改为 0，由支付回调到账）。
 */
@Data
@ConfigurationProperties(prefix = "melodify.user")
public class MelodifyUserProperties {

	/**
	 * 新用户注册成功时写入的初始积分；需 ≥ 单次 {@code melodify.music-generation.generate-cost-points} 才能首次生成。
	 */
	private int signupBonusPoints = 300;
}
