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
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicExploreService {

	public static final int KEYWORD_MAX_LEN = 128;

	private final MusicAssetService musicAssetService;
	private final MusicTaskService musicTaskService;
	private final MusicLikeService musicLikeService;

	/**
	 * @param keyword 可选；匹配资产标题或任务的创作描述 {@code prompt}。
	 * @param sortMode  {@code NEWEST}（默认）按发布时间；{@code LIKES} 按点赞数优先。
	 * @param optionalViewerUserId 请求若带有效 JWT（已登录），则返回每条 {@code liked}
	 */
	public IPage<MusicExploreItemVO> pagePublicAssets(long current, long size, String keyword,
			String sortMode, Long optionalViewerUserId) {
		LambdaQueryWrapper<MusicAsset> wrapper = new LambdaQueryWrapper<MusicAsset>()
				.eq(MusicAsset::getIsPublic, 1)
				.eq(MusicAsset::getStatus, 1);

		if (StringUtils.hasText(keyword)) {
			String rawKw = keyword.trim();
			final String kw = rawKw.length() > KEYWORD_MAX_LEN ? rawKw.substring(0, KEYWORD_MAX_LEN) : rawKw;
			List<MusicTask> tasks = musicTaskService.lambdaQuery()
					.like(MusicTask::getPrompt, kw)
					.select(MusicTask::getId)
					.list();
			Set<Long> promptTaskIds = tasks.stream().map(MusicTask::getId).collect(Collectors.toSet());
			wrapper.and(w -> {
				w.like(MusicAsset::getTitle, kw);
				if (!promptTaskIds.isEmpty()) {
					w.or().in(MusicAsset::getTaskId, promptTaskIds);
				}
			});
		}

		boolean likesSort = StringUtils.hasText(sortMode) && sortMode.trim().equalsIgnoreCase("LIKES");
		if (likesSort) {
			wrapper.last("ORDER BY (SELECT COUNT(1) FROM music_like ml WHERE ml.asset_id = music_asset.id) DESC, "
					+ "music_asset.create_time DESC");
		} else {
			wrapper.orderByDesc(MusicAsset::getCreateTime);
		}

		Page<MusicAsset> raw = musicAssetService.page(new Page<>(current, size), wrapper);
		List<MusicAsset> assets = raw.getRecords();

		Map<Long, MusicTask> taskByPk = loadTasks(assets);
		Map<Long, Long> likesByAsset = countLikes(assets);
		Set<Long> likedByViewer = likedAssetIdsByViewer(assets, optionalViewerUserId);

		List<MusicExploreItemVO> rows = assets.stream()
				.map(a -> MusicExploreItemVO.of(a, taskByPk.get(a.getTaskId()),
						likesByAsset.getOrDefault(a.getId(), 0L),
						likedByViewer.contains(a.getId())))
				.toList();

		Page<MusicExploreItemVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
		out.setRecords(rows);
		return out;
	}

	private Set<Long> likedAssetIdsByViewer(List<MusicAsset> assets, Long viewerUserId) {
		if (viewerUserId == null || assets.isEmpty()) {
			return Set.of();
		}
		List<Long> ids = assets.stream().map(MusicAsset::getId).toList();
		return musicLikeService.lambdaQuery()
				.in(MusicLike::getAssetId, ids)
				.eq(MusicLike::getUserId, viewerUserId)
				.list()
				.stream()
				.map(MusicLike::getAssetId)
				.collect(Collectors.toSet());
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
