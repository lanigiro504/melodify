package com.melodify.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.melodify.constants.MusicTaskStatuses;
import com.melodify.entity.MusicTask;
import com.melodify.entity.RechargeOrder;
import com.melodify.mapper.AdminDashboardMapper;
import com.melodify.model.vo.AdminDashboardSummaryVO;
import com.melodify.model.vo.AdminDashboardTrendsVO;
import com.melodify.model.vo.MusicTaskStatusCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

	private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

	/** 仪表盘趋势最多查询天数，防止大区间拖慢库 */
	private static final int TRENDS_MAX_DAYS = 90;

	/** {@code recharge_order.status}：1 已支付 */
	private static final int RECHARGE_STATUS_PAID = 1;

	private final SysUserService sysUserService;
	private final MusicTaskService musicTaskService;
	private final RechargeOrderService rechargeOrderService;
	private final AdminDashboardMapper adminDashboardMapper;

	public AdminDashboardTrendsVO trends(int requestedDays) {
		int days = requestedDays;
		if (days < 1) {
			days = 1;
		}
		if (days > TRENDS_MAX_DAYS) {
			days = TRENDS_MAX_DAYS;
		}
		LocalDate end = LocalDate.now();
		LocalDate start = end.minusDays(days - 1L);
		LocalDateTime rangeStart = start.atStartOfDay();
		LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();

		Map<LocalDate, Long> createdMap = toDayCountMap(adminDashboardMapper.countMusicTasksCreatedByDay(rangeStart, rangeEndExclusive));
		Map<LocalDate, Long> okMap = toDayCountMap(adminDashboardMapper.countMusicTasksSucceededByDay(rangeStart, rangeEndExclusive));
		Map<LocalDate, Long> failMap = toDayCountMap(adminDashboardMapper.countMusicTasksFailedByDay(rangeStart, rangeEndExclusive));
		Map<LocalDate, Long> regMap = toDayCountMap(adminDashboardMapper.countUsersRegisteredByDay(rangeStart, rangeEndExclusive));
		Map<LocalDate, long[]> rechargeMap = toDayRechargeMap(
				adminDashboardMapper.rechargePaidAggByDay(rangeStart, rangeEndExclusive));

		List<String> labels = new ArrayList<>(days);
		List<Long> created = new ArrayList<>(days);
		List<Long> succeeded = new ArrayList<>(days);
		List<Long> failed = new ArrayList<>(days);
		List<Long> registered = new ArrayList<>(days);
		List<Long> paidOrders = new ArrayList<>(days);
		List<Long> paidCents = new ArrayList<>(days);
		for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
			labels.add(d.format(DAY_LABEL));
			created.add(createdMap.getOrDefault(d, 0L));
			succeeded.add(okMap.getOrDefault(d, 0L));
			failed.add(failMap.getOrDefault(d, 0L));
			registered.add(regMap.getOrDefault(d, 0L));
			long[] pr = rechargeMap.getOrDefault(d, new long[] {0L, 0L});
			paidOrders.add(pr[0]);
			paidCents.add(pr[1]);
		}

		List<MusicTaskStatusCountVO> distribution = new ArrayList<>();
		for (Map<String, Object> row : adminDashboardMapper.countMusicTasksGroupByStatus()) {
			int st = ((Number) row.get("s")).intValue();
			long cnt = ((Number) row.get("c")).longValue();
			distribution.add(new MusicTaskStatusCountVO(st, cnt));
		}

		return new AdminDashboardTrendsVO(
				labels,
				created,
				succeeded,
				failed,
				registered,
				paidOrders,
				paidCents,
				distribution);
	}

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
				yuanApprox);
	}

	private static Map<LocalDate, Long> toDayCountMap(List<Map<String, Object>> rows) {
		Map<LocalDate, Long> m = new LinkedHashMap<>();
		if (rows == null) {
			return m;
		}
		for (Map<String, Object> row : rows) {
			LocalDate d = toLocalDate(row.get("d"));
			if (d == null) {
				continue;
			}
			m.put(d, ((Number) row.get("c")).longValue());
		}
		return m;
	}

	private static Map<LocalDate, long[]> toDayRechargeMap(List<Map<String, Object>> rows) {
		Map<LocalDate, long[]> m = new LinkedHashMap<>();
		if (rows == null) {
			return m;
		}
		for (Map<String, Object> row : rows) {
			LocalDate d = toLocalDate(row.get("d"));
			if (d == null) {
				continue;
			}
			long cnt = ((Number) row.get("cnt")).longValue();
			long cents = toLongMoney(row.get("cents"));
			m.put(d, new long[] {cnt, cents});
		}
		return m;
	}

	private static long toLongMoney(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof BigDecimal) {
			return ((BigDecimal) v).longValue();
		}
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		return Long.parseLong(v.toString());
	}

	private static LocalDate toLocalDate(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof LocalDate) {
			return (LocalDate) o;
		}
		if (o instanceof java.sql.Date) {
			return ((java.sql.Date) o).toLocalDate();
		}
		if (o instanceof java.util.Date) {
			return new java.sql.Date(((java.util.Date) o).getTime()).toLocalDate();
		}
		String s = o.toString();
		if (s.length() >= 10) {
			return LocalDate.parse(s.substring(0, 10));
		}
		return null;
	}
}
