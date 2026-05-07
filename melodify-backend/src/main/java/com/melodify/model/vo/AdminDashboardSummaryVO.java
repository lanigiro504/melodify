package com.melodify.model.vo;

import java.math.BigDecimal;

/** 控制台首页汇总指标（非实时大屏，分页列表查询仍可独立使用）。 */
public record AdminDashboardSummaryVO(
		long usersTotal,
		long musicTasksGenerating,
		long musicTasksTodayCreated,
		long musicTasksTodaySucceeded,
		long musicTasksTodayFailed,
		long rechargeOrdersPaidTodayCount,
		int rechargePaidTodayCentTotal,
		BigDecimal rechargePaidTodayYuanApprox,
		long publicAssetsTotal) {}
