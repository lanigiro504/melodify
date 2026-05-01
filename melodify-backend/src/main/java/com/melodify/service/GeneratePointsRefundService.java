package com.melodify.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.melodify.entity.MusicTask;
import com.melodify.entity.PointLog;
import com.melodify.entity.SysUser;
import com.melodify.support.BizIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suno / 远端生成失败时将已扣积分按业务单号等额退回，避免重复退费（bizType + bizId 去重）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratePointsRefundService {

	private final PointLogService pointLogService;
	private final SysUserService sysUserService;

	@Transactional(rollbackFor = Exception.class)
	public void refundGenerationCostIfConsumed(MusicTask task) {
		Integer costObj = task.getCostPoints();
		int cost = costObj == null ? 0 : costObj;
		if (cost <= 0) {
			return;
		}
		String bizId = task.getTaskId();
		if (bizId == null || bizId.isEmpty()) {
			return;
		}
		long refunded = pointLogService.lambdaQuery()
				.eq(PointLog::getUserId, task.getUserId())
				.eq(PointLog::getBizType, "generate_refund")
				.eq(PointLog::getBizId, bizId)
				.count();
		if (refunded > 0) {
			return;
		}

		LambdaUpdateWrapper<SysUser> add = Wrappers.<SysUser>lambdaUpdate()
				.eq(SysUser::getId, task.getUserId())
				.setSql("points = points + " + cost);
		if (!sysUserService.update(add)) {
			log.error("退费加积分失败 userId={}, taskId={}", task.getUserId(), bizId);
			return;
		}
		SysUser after = sysUserService.getById(task.getUserId());

		PointLog logRow = new PointLog();
		logRow.setLogId(BizIds.uuidCompact());
		logRow.setUserId(task.getUserId());
		logRow.setChangeType(3); // 退款
		logRow.setAmount(cost);
		logRow.setBalance(after != null ? after.getPoints() : cost);
		logRow.setBizType("generate_refund");
		logRow.setBizId(bizId);
		logRow.setRemark("生成失败退费");
		pointLogService.save(logRow);
	}
}
