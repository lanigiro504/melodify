package com.melodify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 生成相关占位配置（费用、异步模拟延迟），绑定前缀 {@code melodify.music-generation}。
 * <p>真实对接第三方时需增加模型单价、Webhook 密钥等字段。</p>
 */
@Data
@ConfigurationProperties(prefix = "melodify.music-generation")
public class MusicGenerationProperties {

	/** 每次生成消耗的积分（与余额原子扣减配合使用）。 */
	private int generateCostPoints = 10;

	/** 本地模拟异步耗时的毫秒数（仅开发/联调用）。 */
	private long simulateDelayMs = 2500;

	/**
	 * 模拟成功时写入 {@link com.melodify.entity.MusicAsset#getFileUrl()} 的占位地址；
	 * 可换成自有 CDN 片段或静默音频。
	 */
	private String placeholderAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
}
