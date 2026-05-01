package com.melodify.integration.suno;

import com.melodify.config.MusicGenerationProperties;
import com.melodify.service.MusicSimulatedGenerationRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 根据配置在「本地模拟」与「Suno 网关」之间二选一派发异步任务；
 * 未配置密钥时即使 provider=suno 也会自动回退模拟，避免静默失败。
 */
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
		if (wantsSuno && sunoApiClient.isConfigured()) {
			sunoGenerationRunner.completeAfterSubmit(internalMusicTaskPk);
		} else {
			simulatedGenerationRunner.completeAfterSubmit(internalMusicTaskPk);
		}
	}
}
