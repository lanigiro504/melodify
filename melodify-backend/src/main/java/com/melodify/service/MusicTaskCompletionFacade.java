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
 * 异步 Worker 的事务边界：任务状态 CAS、成品入库；占位与远端 URL 共用一套逻辑。
 */
@Component
@RequiredArgsConstructor
public class MusicTaskCompletionFacade {

	private final MusicTaskService musicTaskService;
	private final MusicAssetService musicAssetService;
	private final MusicGenerationProperties properties;

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

		String finalTitle = StringUtils.hasText(title) ? title : titleHint(task);
		String finalUrl = StringUtils.hasText(audioUrl)
				? audioUrl.strip()
				: properties.getPlaceholderAudioUrl();

		asset.setTitle(finalTitle.length() <= 120 ? finalTitle : finalTitle.substring(0, 120) + "…");
		asset.setFileUrl(finalUrl);
		asset.setCoverUrl("");
		asset.setDurationSec(durationSec != null ? Math.max(durationSec, 0) : 0);
		asset.setFormat("mp3");
		asset.setBitrateKbps(320);
		asset.setIsPublic(0);
		asset.setLicenseType("personal");
		asset.setStatus(1);

		musicAssetService.save(asset);
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean markGenerationFailed(Long internalMusicTaskPk, String errorCode, String errorMessage) {
		String code = truncate(errorCode, 64);
		String msg = truncate(errorMessage, 500);
		return musicTaskService.lambdaUpdate()
				.eq(MusicTask::getId, internalMusicTaskPk)
				.eq(MusicTask::getStatus, MusicTaskStatuses.GENERATING)
				.set(MusicTask::getStatus, MusicTaskStatuses.FAILED)
				.set(MusicTask::getErrorCode, code != null ? code : "FAILED")
				.set(MusicTask::getErrorMessage, msg != null ? msg : "")
				.set(MusicTask::getFinishedAt, LocalDateTime.now())
				.update();
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
