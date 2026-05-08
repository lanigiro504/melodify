package com.melodify.model.vo;

import java.util.List;

/**
 * 管理端仪表盘趋势：按自然日聚合的序列 + 任务状态分布。
 *
 * @param dayLabels                    与下方各序列一一对应，格式 MM-dd
 * @param musicTasksCreatedPerDay      当日新建任务数（按 create_time）
 * @param musicTasksSucceededPerDay    当日成功数（按 finished_at，status=成功）
 * @param musicTasksFailedPerDay       当日失败数（按 finished_at，status=失败）
 * @param usersRegisteredPerDay        当日注册用户（sys_user.create_time，未逻辑删除）
 * @param rechargePaidOrdersPerDay     当日模拟支付已完成订单笔数（按 paid_at）
 * @param rechargePaidCentSumPerDay    当日已支付金额合计（分）
 * @param musicTaskStatusDistribution  全库任务按 status 聚合（快照）
 */
public record AdminDashboardTrendsVO(
		List<String> dayLabels,
		List<Long> musicTasksCreatedPerDay,
		List<Long> musicTasksSucceededPerDay,
		List<Long> musicTasksFailedPerDay,
		List<Long> usersRegisteredPerDay,
		List<Long> rechargePaidOrdersPerDay,
		List<Long> rechargePaidCentSumPerDay,
		List<MusicTaskStatusCountVO> musicTaskStatusDistribution) {}
