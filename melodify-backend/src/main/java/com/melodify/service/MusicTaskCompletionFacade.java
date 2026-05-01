package com.melodify.service;

import com.melodify.config.MusicGenerationProperties;
import com.melodify.constants.MusicTaskStatuses;
import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicTask;
import com.melodify.support.BizIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 异步 Worker 的事务边界：CAS 更新任务状态 + 写入成品占位；与 {@link MusicSimulatedGenerationRunner} 解耦，
 * 避免同类自调用导致的 {@link Transactional} 失效问题。
 */
@Component
@RequiredArgsConstructor
public class MusicTaskCompletionFacade {

	private final MusicTaskService musicTaskService;
	private final MusicAssetService musicAssetService;
	private final MusicGenerationProperties properties;

	@Transactional(rollbackFor = Exception.class)
	public void markSucceededAndPersistAsset(Long internalMusicTaskPk) {
		MusicTask task = musicTaskService.getById(internalMusicTaskPk);
		if (task == null || !Integer.valueOf(MusicTaskStatuses.GENERATING).equals(task.getStatus())) {
			return;
		}

		boolean transitioned = musicTaskService.lambdaUpdate()
				.eq(MusicTask::getId, internalMusicTaskPk)
				.eq(MusicTask::getStatus, MusicTaskStatuses.GENERATING)
				.set(MusicTask::getStatus, MusicTaskStatuses.SUCCEEDED)
				.set(MusicTask::getFinishedAt, LocalDateTime.now())
				.update();

		if (!transitioned) {
			return;
		}

		task = musicTaskService.getById(internalMusicTaskPk);

		MusicAsset asset = new MusicAsset();
		asset.setAssetId(BizIds.uuidCompact());
		asset.setTaskId(internalMusicTaskPk);
		asset.setUserId(task.getUserId());
		asset.setTitle(titleHint(task));
		asset.setFileUrl(properties.getPlaceholderAudioUrl());
		asset.setCoverUrl("");
		asset.setDurationSec(0);
		asset.setFormat("mp3");
		asset.setBitrateKbps(320);
		asset.setIsPublic(0);
		asset.setLicenseType("personal");
		asset.setStatus(1);

		musicAssetService.save(asset);
	}

	private static String titleHint(MusicTask t) {
		if (!StringUtils.hasText(t.getPrompt())) {
			return "Melodify 生成作品";
		}
		String p = t.getPrompt().strip();
		return p.length() <= 120 ? p : p.substring(0, 120) + "…";
	}
}
