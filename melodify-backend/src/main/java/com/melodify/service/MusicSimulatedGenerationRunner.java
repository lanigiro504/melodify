package com.melodify.service;

import com.melodify.config.MusicGenerationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步模拟「远端生成耗时」，具体落库收尾委托 {@link MusicTaskCompletionFacade} 以保证事务边界清晰。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicSimulatedGenerationRunner {

	private final MusicTaskCompletionFacade musicTaskCompletionFacade;
	private final MusicGenerationProperties musicGenerationProperties;

	/** 异步入口：在事务提交回调中触发即可。 */
	@Async("musicTaskExecutor")
	public void completeAfterSubmit(Long internalMusicTaskPk) {
		try {
			Thread.sleep(Math.max(0L, musicGenerationProperties.getSimulateDelayMs()));
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.warn("音乐生成模拟线程被中断 taskPk={}", internalMusicTaskPk);
			return;
		}
		musicTaskCompletionFacade.markSucceededAndPersistPlaceholderAsset(internalMusicTaskPk);
	}
}
