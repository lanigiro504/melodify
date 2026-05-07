package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.melodify.common.exception.BizException;
import com.melodify.config.MusicGenerationProperties;
import com.melodify.constants.MusicTaskStatuses;
import com.melodify.entity.MusicTask;
import com.melodify.entity.PointLog;
import com.melodify.entity.SysUser;
import com.melodify.integration.suno.MusicGenerationAsyncCoordinator;
import com.melodify.model.dto.MusicGenerateRequestDTO;
import com.melodify.model.vo.MusicGenerateSubmitVO;
import com.melodify.service.MusicGenerationService;
import com.melodify.service.MusicTaskService;
import com.melodify.service.PointLogService;
import com.melodify.service.SysUserService;
import com.melodify.support.BizIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成编排：计费与持久化在同一事务内，回调异步执行需在 {@code afterCommit} 之后触发。
 */
@Service
@RequiredArgsConstructor
public class MusicGenerationServiceImpl implements MusicGenerationService {

	private final SysUserService sysUserService;
	private final MusicTaskService musicTaskService;
	private final PointLogService pointLogService;
	private final MusicGenerationAsyncCoordinator musicGenerationAsyncCoordinator;
	private final MusicGenerationProperties musicGenerationProperties;

	/**
	 * 1) 原子扣积分（单行 UPDATE points &gt;= cost）并写积分流水；2) 落库 GENERATING 任务；
	 * 3) 注册 {@linkplain #registerAfterCommit}，仅在事务提交成功后再派发异步 Worker，避免出现「远端已接单但本地事务回滚」的不一致。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public MusicGenerateSubmitVO submitGeneration(Long userId, MusicGenerateRequestDTO dto) {
		int cost = Math.max(0, musicGenerationProperties.getGenerateCostPoints());

		if (cost > 0) {
			boolean deducted = SysUserAtomicPoints.deductIfEnough(sysUserService, userId, cost);
			if (!deducted) {
				throw new BizException(400, "积分不足");
			}
		}

		SysUser afterUser = sysUserService.getById(userId);
		if (afterUser == null) {
			throw new BizException(404, "用户不存在");
		}
		Integer pts = afterUser.getPoints();
		int balanceAfter = pts != null ? pts : 0;

		String businessTaskId = BizIds.uuidCompact();
		recordPointConsumptionIfNeeded(userId, cost, businessTaskId, balanceAfter);

		Map<String, Object> mergedParams = mergedParams(dto);
		LocalDateTime now = LocalDateTime.now();

		MusicTask task = new MusicTask();
		task.setTaskId(businessTaskId);
		task.setUserId(userId);
		task.setModelCode(dto.getModelCode().trim());
		task.setPrompt(StringUtils.hasText(dto.getPrompt()) ? dto.getPrompt().trim() : null);
		task.setParams(mergedParams.isEmpty() ? null : mergedParams);
		task.setStatus(MusicTaskStatuses.GENERATING);
		task.setErrorCode("");
		task.setErrorMessage("");
		task.setCostPoints(cost);
		task.setStartedAt(now);

		boolean saved = musicTaskService.save(task);
		if (!saved || task.getId() == null) {
			throw new BizException(500, "生成任务入库失败");
		}

		final Long taskPk = task.getId();

		registerAfterCommit(() -> musicGenerationAsyncCoordinator.dispatchAfterSubmit(taskPk));

		return new MusicGenerateSubmitVO(businessTaskId, task.getStatus(), cost);
	}

	private void recordPointConsumptionIfNeeded(Long userId, int cost, String businessTaskId, int balanceAfter) {
		if (cost <= 0) {
			return;
		}
		PointLog log = new PointLog();
		log.setLogId(BizIds.uuidCompact());
		log.setUserId(userId);
		log.setChangeType(2); // 消费 —— 对齐 docs/init.sql 注释
		log.setAmount(-cost);
		log.setBalance(balanceAfter);
		log.setBizType("generate");
		log.setBizId(businessTaskId);
		log.setRemark("AI 音乐生成扣费");

		boolean ok = pointLogService.save(log);
		if (!ok) {
			throw new BizException(500, "积分流水写入失败");
		}
	}

	/** Spring 在提交阶段回调：早于此时执行异步会与未提交的事务并发读，故必须挂在 afterCommit。 */
	private static void registerAfterCommit(Runnable runnable) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				runnable.run();
			}
		});
	}

	private static Map<String, Object> mergedParams(MusicGenerateRequestDTO dto) {
		Map<String, Object> params = dto.getParams() == null ? new HashMap<>() : new HashMap<>(dto.getParams());
		if (StringUtils.hasText(dto.getLyrics())) {
			params.put("lyrics", dto.getLyrics().trim());
		}
		return params;
	}

	static final class SysUserAtomicPoints {
		private SysUserAtomicPoints() {}

		/**
		 * 用带条件的单行 UPDATE（points &gt;= cost）实现「够不够扣」，避免先读后写竞态；
		 * 返回 false 表示余额不足或未更新到行。
		 */
		static boolean deductIfEnough(SysUserService sysUserService, Long userId, int cost) {
			LambdaUpdateWrapper<SysUser> uw =
					Wrappers.<SysUser>lambdaUpdate()
							.eq(SysUser::getId, userId)
							.ge(SysUser::getPoints, cost)
							.setSql("points = points - " + cost);
			return sysUserService.update(uw);
		}
	}
}
