package com.melodify.service;

import com.melodify.config.MusicGenerationProperties;
import com.melodify.constants.MusicTaskStatuses;
import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicTask;
import com.melodify.event.GenerationFinishedEvent;
import com.melodify.support.BizIds;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 异步 Worker 的事务边界：任务状态 CAS、成品入库；占位与远端 URL 共用一套逻辑。
 */
@Component
@RequiredArgsConstructor
public class MusicTaskCompletionFacade {

	private final MusicTaskService musicTaskService;
	private final MusicAssetService musicAssetService;
	private final MusicGenerationProperties properties;
	private final AudioMirrorService audioMirrorService;
	private final ApplicationEventPublisher applicationEventPublisher;

	/** 模拟通路：占位音频链接。 */
	@Transactional(rollbackFor = Exception.class)
	public void markSucceededAndPersistPlaceholderAsset(Long internalMusicTaskPk) {
		markSucceededAndPersistAsset(internalMusicTaskPk, null,
				properties.getPlaceholderAudioUrl(), null);
	}

	/** Suno 成功：写入真实远端 URL（及可选时长/标题）。 */
	@Transactional(rollbackFor = Exception.class)
	public void markSucceededAndPersistAsset(Long internalMusicTaskPk, String title,
			String audioUrl,
			Integer durationSec) {
		MusicTask task = musicTaskService.getById(internalMusicTaskPk);
		if (task == null || !Integer.valueOf(MusicTaskStatuses.GENERATING).equals(task.getStatus())) {
			return;
		}

		/* 与其它异步路径争抢同一任务时，仅一例能 status=GENERATING→SUCCEEDED */
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
		if (task == null) {
			return;
		}
		String finalTitle = StringUtils.hasText(title) ? title : titleHint(task);
		String finalUrl = StringUtils.hasText(audioUrl)
				? audioUrl.strip()
				: properties.getPlaceholderAudioUrl();
		MusicAsset asset = newAsset(internalMusicTaskPk, task.getUserId(), finalTitle, finalUrl, durationSec);
		asset.setFileUrl(audioMirrorService.mirrorRemoteToLocalIfEnabled(finalUrl, asset.getAssetId()));
		musicAssetService.save(asset);
		String bizId = task.getTaskId() != null ? task.getTaskId() : "";
		applicationEventPublisher.publishEvent(new GenerationFinishedEvent(task.getUserId(), bizId, true, finalTitle, null));
	}

	/**
	 * Suno 管理端已有成品时的补偿同步：允许把已成功但仍是占位音频的资产替换为真实 URL。
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean syncSucceededAssetFromSuno(Long internalMusicTaskPk, String title,
			String audioUrl,
			Integer durationSec) {
		if (!StringUtils.hasText(audioUrl)) {
			return false;
		}
		MusicTask task = musicTaskService.getById(internalMusicTaskPk);
		if (task == null) {
			return false;
		}
		Integer status = task.getStatus();
		boolean canSync = Integer.valueOf(MusicTaskStatuses.GENERATING).equals(status)
				|| Integer.valueOf(MusicTaskStatuses.SUCCEEDED).equals(status);
		if (!canSync) {
			return false;
		}
		if (Integer.valueOf(MusicTaskStatuses.GENERATING).equals(status)) {
			musicTaskService.lambdaUpdate()
					.eq(MusicTask::getId, internalMusicTaskPk)
					.eq(MusicTask::getStatus, MusicTaskStatuses.GENERATING)
					.set(MusicTask::getStatus, MusicTaskStatuses.SUCCEEDED)
					.set(MusicTask::getFinishedAt, LocalDateTime.now())
					.update();
			task = musicTaskService.getById(internalMusicTaskPk);
			if (task == null) {
				return false;
			}
		}

		String finalTitle = StringUtils.hasText(title) ? title : titleHint(task);
		String finalUrl = audioUrl.strip();
		int finalDuration = durationSec != null ? Math.max(durationSec, 0) : 0;
		MusicAsset latest = musicAssetService.lambdaQuery()
				.eq(MusicAsset::getTaskId, internalMusicTaskPk)
				.eq(MusicAsset::getUserId, task.getUserId())
				.orderByDesc(MusicAsset::getCreateTime)
				.last("LIMIT 1")
				.one();
		if (latest != null) {
			applyAssetPayload(latest, finalTitle, finalUrl, finalDuration);
			latest.setFileUrl(audioMirrorService.mirrorRemoteToLocalIfEnabled(finalUrl, latest.getAssetId()));
			return musicAssetService.updateById(latest);
		}

		MusicAsset created = newAsset(internalMusicTaskPk, task.getUserId(), finalTitle, finalUrl, finalDuration);
		created.setFileUrl(audioMirrorService.mirrorRemoteToLocalIfEnabled(finalUrl, created.getAssetId()));
		return musicAssetService.save(created);
	}

	private MusicAsset newAsset(Long taskPk, Long userId, String title, String fileUrl, Integer durationSec) {
		MusicAsset asset = new MusicAsset();
		asset.setAssetId(BizIds.uuidCompact());
		asset.setTaskId(taskPk);
		asset.setUserId(userId);
		asset.setCoverUrl("");
		asset.setBitrateKbps(320);
		asset.setIsPublic(0);
		asset.setLicenseType("personal");
		applyAssetPayload(asset, title, fileUrl, durationSec != null ? Math.max(durationSec, 0) : 0);
		return asset;
	}

	private static void applyAssetPayload(MusicAsset asset, String title, String fileUrl, int durationSec) {
		asset.setTitle(truncate(title, 120));
		asset.setFileUrl(fileUrl);
		asset.setDurationSec(Math.max(durationSec, 0));
		asset.setFormat("mp3");
		asset.setStatus(1);
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean markGenerationFailed(Long internalMusicTaskPk, String errorCode, String errorMessage) {
		MusicTask before = musicTaskService.getById(internalMusicTaskPk);
		if (before == null) {
			return false;
		}
		String code = truncate(errorCode, 64);
		String msg = truncate(errorMessage, 500);
		boolean updated = musicTaskService.lambdaUpdate()
				.eq(MusicTask::getId, internalMusicTaskPk)
				.eq(MusicTask::getStatus, MusicTaskStatuses.GENERATING)
				.set(MusicTask::getStatus, MusicTaskStatuses.FAILED)
				.set(MusicTask::getErrorCode, code != null ? code : "FAILED")
				.set(MusicTask::getErrorMessage, msg != null ? msg : "")
				.set(MusicTask::getFinishedAt, LocalDateTime.now())
				.update();
		if (!updated) {
			return false;
		}
		String bizId = before.getTaskId() != null ? before.getTaskId() : "";
		applicationEventPublisher.publishEvent(new GenerationFinishedEvent(before.getUserId(), bizId, false,
				null,
				msg));
		return true;
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max);
	}

	private static String titleHint(MusicTask t) {
		if (!StringUtils.hasText(t.getPrompt())) {
			return "Melodify 生成作品";
		}
		String p = t.getPrompt().strip();
		return p.length() <= 120 ? p : p.substring(0, 120) + "…";
	}
}
