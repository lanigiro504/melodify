package com.melodify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SunoAPI 兼容网关（{@code api.sunoapi.org}）：Bearer + /generate + /generate/record-info 轮询。
 * <p>密钥请仅用环境变量注入，切勿提交仓库。</p>
 */
@Data
@ConfigurationProperties(prefix = "melodify.suno")
public class SunoApiProperties {

	/** 基础路径，示例文档为 {@code https://api.sunoapi.org/api/v1} */
	private String apiBaseUrl = "https://api.sunoapi.org/api/v1";

	/** Bearer Token，等价于前端 SunoAPI 构造函数的 apiKey */
	private String apiKey = "";

	/** 调用 record-info 的间隔毫秒数（官方示例约 30s，本地默认略短仍可配置） */
	private long pollIntervalMs = 15000;

	/** 单次生成最长等待时间（毫秒），超时后将任务标记失败并退费 */
	private long maxWaitMs = 600000;

	/**
	 * 回调 URL，必填写入 POST {@code /generate}；不配则使用不可路由占位（RFC 6761 {@code *.invalid}），完成态依赖轮询即可。
	 */
	private String callbackUrl = "";

	/** 当请求 JSON 未显式带 {@code model} 时回填（如官方示例 {@code V4_5}） */
	private String defaultModel = "V4_5";
}
