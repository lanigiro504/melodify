package com.melodify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 出站 REST 客户端（SunoAPI 等第三方）；连接/读超时略大于默认值以适配长尾请求。
 */
@Configuration
public class RestClientBeans {

	@Bean
	public RestClient.Builder outboundRestClientBuilder() {
		SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
		rf.setConnectTimeout(30_000);
		rf.setReadTimeout(120_000);
		return RestClient.builder().requestFactory(rf);
	}
}
