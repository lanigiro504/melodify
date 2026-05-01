package com.melodify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicLike;
import com.melodify.entity.MusicTask;
import com.melodify.model.vo.MusicExploreItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicExploreService {

	private final MusicAssetService musicAssetService;
	private final MusicTaskService musicTaskService;
	private final MusicLikeService musicLikeService;

	public IPage<MusicExploreItemVO> pagePublicAssets(long current, long size) {
		LambdaQueryWrapper<MusicAsset> wrapper = new LambdaQueryWrapper<MusicAsset>()
				.eq(MusicAsset::getIsPublic, 1)
				.eq(MusicAsset::getStatus, 1)
				.orderByDesc(MusicAsset::getCreateTime);
		Page<MusicAsset> raw = musicAssetService.page(new Page<>(current, size), wrapper);
		List<MusicAsset> assets = raw.getRecords();

		Map<Long, MusicTask> taskByPk = loadTasks(assets);
		Map<Long, Long> likesByAsset = countLikes(assets);

		List<MusicExploreItemVO> rows = assets.stream()
				.map(a -> MusicExploreItemVO.of(a, taskByPk.get(a.getTaskId()),
						likesByAsset.getOrDefault(a.getId(), 0L)))
				.toList();

		Page<MusicExploreItemVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
		out.setRecords(rows);
		return out;
	}

	private Map<Long, MusicTask> loadTasks(List<MusicAsset> assets) {
		Set<Long> taskPks = assets.stream().map(MusicAsset::getTaskId).collect(Collectors.toSet());
		if (taskPks.isEmpty()) {
			return Map.of();
		}
		return musicTaskService.listByIds(taskPks).stream()
				.collect(Collectors.toMap(MusicTask::getId, t -> t));
	}

	/** 当前页资产 id 一次 GROUP BY 统计点赞，避免逐条 count */
	private Map<Long, Long> countLikes(List<MusicAsset> assets) {
		List<Long> assetIds = assets.stream().map(MusicAsset::getId).toList();
		if (assetIds.isEmpty()) {
			return Map.of();
		}
		QueryWrapper<MusicLike> qw = new QueryWrapper<MusicLike>()
				.select("asset_id", "count(1) AS cnt")
				.in("asset_id", assetIds)
				.groupBy("asset_id");
		Map<Long, Long> out = new HashMap<>();
		for (Map<String, Object> row : musicLikeService.getBaseMapper().selectMaps(qw)) {
			long assetId = ((Number) row.get("asset_id")).longValue();
			out.put(assetId, ((Number) row.get("cnt")).longValue());
		}
		return out;
	}
}
