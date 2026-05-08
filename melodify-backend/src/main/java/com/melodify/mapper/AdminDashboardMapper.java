package com.melodify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 控制台仪表盘聚合查询（按日分组，走索引列）。 */
@Mapper
public interface AdminDashboardMapper {

	@Select("""
			SELECT DATE(create_time) AS d, COUNT(*) AS c
			FROM music_task
			WHERE create_time >= #{start} AND create_time < #{endExclusive}
			GROUP BY DATE(create_time)
			ORDER BY d
			""")
	List<Map<String, Object>> countMusicTasksCreatedByDay(
			@Param("start") LocalDateTime start,
			@Param("endExclusive") LocalDateTime endExclusive);

	@Select("""
			SELECT DATE(finished_at) AS d, COUNT(*) AS c
			FROM music_task
			WHERE status = 2
			  AND finished_at IS NOT NULL
			  AND finished_at >= #{start} AND finished_at < #{endExclusive}
			GROUP BY DATE(finished_at)
			ORDER BY d
			""")
	List<Map<String, Object>> countMusicTasksSucceededByDay(
			@Param("start") LocalDateTime start,
			@Param("endExclusive") LocalDateTime endExclusive);

	@Select("""
			SELECT DATE(finished_at) AS d, COUNT(*) AS c
			FROM music_task
			WHERE status = 3
			  AND finished_at IS NOT NULL
			  AND finished_at >= #{start} AND finished_at < #{endExclusive}
			GROUP BY DATE(finished_at)
			ORDER BY d
			""")
	List<Map<String, Object>> countMusicTasksFailedByDay(
			@Param("start") LocalDateTime start,
			@Param("endExclusive") LocalDateTime endExclusive);

	@Select("""
			SELECT DATE(create_time) AS d, COUNT(*) AS c
			FROM sys_user
			WHERE is_deleted = 0
			  AND create_time >= #{start} AND create_time < #{endExclusive}
			GROUP BY DATE(create_time)
			ORDER BY d
			""")
	List<Map<String, Object>> countUsersRegisteredByDay(
			@Param("start") LocalDateTime start,
			@Param("endExclusive") LocalDateTime endExclusive);

	@Select("""
			SELECT DATE(paid_at) AS d,
			       COUNT(*) AS cnt,
			       COALESCE(SUM(amount_cent), 0) AS cents
			FROM recharge_order
			WHERE status = 1
			  AND paid_at IS NOT NULL
			  AND paid_at >= #{start} AND paid_at < #{endExclusive}
			GROUP BY DATE(paid_at)
			ORDER BY d
			""")
	List<Map<String, Object>> rechargePaidAggByDay(
			@Param("start") LocalDateTime start,
			@Param("endExclusive") LocalDateTime endExclusive);

	@Select("""
			SELECT status AS s, COUNT(*) AS c
			FROM music_task
			GROUP BY status
			ORDER BY status
			""")
	List<Map<String, Object>> countMusicTasksGroupByStatus();
}
