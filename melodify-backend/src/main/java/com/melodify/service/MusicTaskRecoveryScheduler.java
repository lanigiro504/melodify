package com.melodify.service;

import com.melodify.config.SunoApiProperties;
import com.melodify.constants.MusicTaskStatuses;
import com.melodify.entity.MusicTask;
import com.melodify.integration.suno.MusicSunoGenerationRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 补偿：Worker 崩溃或轮询漏掉时，对已绑定 Suno vendor_task_id 且仍为「生成中」的任务拉取 record-info；
 * 超过 {@link SunoApiProperties#getMaxWaitMs()} 仍未结束则标记失败并退费。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MusicTaskRecoveryScheduler {

	private final MusicTaskService musicTaskService;
	private final MusicSunoGenerationRunner musicSunoGenerationRunner;
	private final MusicTaskCompletionFacade musicTaskCompletionFacade;
	private final GeneratePointsRefundService generatePointsRefundService;
	private final SunoApiProperties sunoApiProperties;

	@Scheduled(fixedDelayString = "${melodify.suno.recovery-fixed-delay-ms:120000}")
	public void recoverStuckSunoTasks() {
		LocalDateTime minAge = LocalDateTime.now().minusMinutes(1);
		long maxWait = Math.max(60_000L, sunoApiProperties.getMaxWaitMs());
		LocalDateTime failIfStartedBefore = LocalDateTime.now().minus(Duration.ofMillis(maxWait));

		List<MusicTask> candidates = musicTaskService.lambdaQuery()
				.eq(MusicTask::getStatus, MusicTaskStatuses.GENERATING)
				.isNotNull(MusicTask::getVendorTaskId)
				.isNotNull(MusicTask::getStartedAt)
				.lt(MusicTask::getStartedAt, minAge)
				.orderByAsc(MusicTask::getStartedAt)
				.last("LIMIT 40")
				.list();

		for (MusicTask t : candidates) {
			try {
				musicSunoGenerationRunner.trySyncCompletedAsset(t.getId());
				MusicTask again = musicTaskService.getById(t.getId());
				if (again == null) {
					continue;
				}
				if (!Integer.valueOf(MusicTaskStatuses.GENERATING).equals(again.getStatus())) {
					continue;
				}
				if (again.getStartedAt() != null && again.getStartedAt().isBefore(failIfStartedBefore)) {
					boolean moved = musicTaskCompletionFacade.markGenerationFailed(again.getId(), "SUNO_TIMEOUT",
							"任务恢复：已超过最大等待时间");
					if (moved) {
						generatePointsRefundService.refundGenerationCostIfConsumed(again);
					}
				}
			} catch (Exception ex) {
				log.warn("任务恢复调度异常 pk={}: {}", t.getId(), ex.toString());
			}
		}
	}
}
