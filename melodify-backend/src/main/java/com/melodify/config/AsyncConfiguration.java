package com.melodify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 音乐生成等非关键路径异步任务线程池。
 * <p>{@code @Async(\"musicTaskExecutor\")} 与 Bean 名称保持一致。</p>
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

	@Bean(name = "musicTaskExecutor")
	public Executor musicTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(16);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("melodify-music-");
		executor.initialize();
		return executor;
	}
}
