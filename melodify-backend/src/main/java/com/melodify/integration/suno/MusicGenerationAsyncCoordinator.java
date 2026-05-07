package com.melodify.integration.suno;

import com.melodify.config.MusicGenerationProperties;
import com.melodify.service.MusicSimulatedGenerationRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 根据配置在「本地模拟」与「SunoAPI 网关（api.sunoapi.org）」之间二选一；
 * {@code provider=auto} 时仅在已配置 api-key 时走远端；
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

	/**
	 * 提交事务后的入口：按 provider + 密钥决定走真实 Suno 还是本地占位；
	 * auto 且无密钥时不抛错而是 WARN + 占位，便于本地开发与联调。
	 */
	public void dispatchAfterSubmit(Long internalMusicTaskPk) {
		boolean keyOk = sunoApiClient.isConfigured();
		String rawProv = resolveProviderRaw();

		boolean useSuno = resolveUseSuno(rawProv, keyOk);
		if (useSuno) {
			log.info("musicTask pk={} 走 SunoAPI（api.sunoapi.org）；试听 URL 将由远端回填，不再是占位 SoundHelix",
					internalMusicTaskPk);
			sunoGenerationRunner.completeAfterSubmit(internalMusicTaskPk);
			return;
		}

		if ("suno".equalsIgnoreCase(rawProv) && !keyOk) {
			log.warn("musicTask pk={} provider=suno 但未配置 melodify.suno.api-key / SUNO_API_KEY，回退占位音频",
					internalMusicTaskPk);
		} else if ("simulated".equalsIgnoreCase(rawProv)) {
			log.warn("musicTask pk={} provider=simulated ，使用占位试听（SoundHelix）",
					internalMusicTaskPk);
		} else {
			log.warn("musicTask pk={} provider=auto 但未检测到 Suno api-key ，使用占位试听；请到 application-local.yml 或 SUNO_API_KEY 配置密钥",
					internalMusicTaskPk);
		}
		simulatedGenerationRunner.completeAfterSubmit(internalMusicTaskPk);
	}

	/** 显式 simulated 永远不走路由；suno/auto/空未知 在有密钥时都走路由 Suno（suno 无密钥外层已处理）。 */
	private boolean resolveUseSuno(String rawProv, boolean keyOk) {
		if ("simulated".equalsIgnoreCase(rawProv)) {
			return false;
		}
		return keyOk;
	}

	private String resolveProviderRaw() {
		String p = musicGenerationProperties.getProvider();
		return p == null ? "" : p.trim();
	}
}
