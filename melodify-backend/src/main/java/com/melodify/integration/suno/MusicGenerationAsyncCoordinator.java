package com.melodify.integration.suno;

import com.melodify.config.MusicGenerationProperties;
import com.melodify.service.MusicSimulatedGenerationRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 根据配置在「本地模拟」与「SunoAPI 网关（api.sunoapi.org）」之间二选一；
 * {@code provider=suno} 但未配置密钥时回退占位并输出 WARN，避免误判为已成功调远端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicGenerationAsyncCoordinator {

	private final MusicGenerationProperties musicGenerationProperties;
	private final SunoApiClient sunoApiClient;
	private final MusicSimulatedGenerationRunner simulatedGenerationRunner;
	private final MusicSunoGenerationRunner sunoGenerationRunner;

	public void dispatchAfterSubmit(Long internalMusicTaskPk) {
		boolean wantsSuno = "suno".equalsIgnoreCase(
				musicGenerationProperties.getProvider() == null
						? ""
						: musicGenerationProperties.getProvider().trim());
		boolean keyOk = sunoApiClient.isConfigured();
		if (wantsSuno && keyOk) {
			log.info("musicTask pk={} 走 SunoAPI（api.sunoapi.org）；记录一般在网关控制台，不等同官网 App 的创作列表",
					internalMusicTaskPk);
			sunoGenerationRunner.completeAfterSubmit(internalMusicTaskPk);
			return;
		}

		if (wantsSuno && !keyOk) {
			log.warn("musicTask pk={} provider=suno 但未配置 melodify.suno.api-key / SUNO_API_KEY，回退占位模拟音频",
					internalMusicTaskPk);
		} else {
			log.warn("musicTask pk={} melodify.music-generation.provider={}，使用本地 simulated 占位，不会产生远端 SunoAPI 记录；真生成请设为 suno 并配置密钥",
					internalMusicTaskPk,
					musicGenerationProperties.getProvider());
		}
		simulatedGenerationRunner.completeAfterSubmit(internalMusicTaskPk);
	}
}
