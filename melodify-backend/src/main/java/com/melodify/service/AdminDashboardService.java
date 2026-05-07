package com.melodify.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.melodify.constants.MusicTaskStatuses;
import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicTask;
import com.melodify.entity.RechargeOrder;
import com.melodify.model.vo.AdminDashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

	/** {@code recharge_order.status}：1 已支付 */
	private static final int RECHARGE_STATUS_PAID = 1;

	private final SysUserService sysUserService;
	private final MusicTaskService musicTaskService;
	private final MusicAssetService musicAssetService;
	private final RechargeOrderService rechargeOrderService;

	public AdminDashboardSummaryVO summary() {
		LocalDate today = LocalDate.now();
		LocalDateTime dayStart = today.atStartOfDay();
		LocalDateTime nextDayStart = today.plusDays(1).atStartOfDay();

		long usersTotal = sysUserService.count();
		long generating = musicTaskService.lambdaQuery()
				.eq(MusicTask::getStatus, MusicTaskStatuses.GENERATING)
				.count();
		long createdToday = musicTaskService.lambdaQuery()
				.ge(MusicTask::getCreateTime, dayStart)
				.lt(MusicTask::getCreateTime, nextDayStart)
				.count();
		long succeededToday = musicTaskService.lambdaQuery()
				.eq(MusicTask::getStatus, MusicTaskStatuses.SUCCEEDED)
				.ge(MusicTask::getFinishedAt, dayStart)
				.lt(MusicTask::getFinishedAt, nextDayStart)
				.count();
		long failedToday = musicTaskService.lambdaQuery()
				.eq(MusicTask::getStatus, MusicTaskStatuses.FAILED)
				.ge(MusicTask::getFinishedAt, dayStart)
				.lt(MusicTask::getFinishedAt, nextDayStart)
				.count();

		QueryWrapper<RechargeOrder> paidTodayQw = new QueryWrapper<RechargeOrder>()
				.eq("status", RECHARGE_STATUS_PAID)
				.ge("paid_at", dayStart)
				.lt("paid_at", nextDayStart);
		long rechargePaidTodayOrders = rechargeOrderService.count(paidTodayQw);
		List<Map<String, Object>> sums = rechargeOrderService.getBaseMapper()
				.selectMaps(new QueryWrapper<RechargeOrder>()
						.select("COALESCE(SUM(amount_cent),0) AS s")
						.eq("status", RECHARGE_STATUS_PAID)
						.ge("paid_at", dayStart)
						.lt("paid_at", nextDayStart));

		long centSumRaw = sums.isEmpty() ? 0L : ((Number) sums.get(0).getOrDefault("s", 0L)).longValue();
		int centSum = centSumRaw > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) centSumRaw;

		long publicAssets = musicAssetService.lambdaQuery()
				.eq(MusicAsset::getIsPublic, 1)
				.eq(MusicAsset::getStatus, 1)
				.count();

		BigDecimal yuanApprox = BigDecimal.valueOf(centSum)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

		return new AdminDashboardSummaryVO(
				usersTotal,
				generating,
				createdToday,
				succeededToday,
				failedToday,
				rechargePaidTodayOrders,
				centSum,
				yuanApprox,
				publicAssets);
	}
}
