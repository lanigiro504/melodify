package com.melodify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicLike;
import com.melodify.entity.MusicTask;
import com.melodify.model.vo.MusicExploreItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

		Set<Long> taskPks = raw.getRecords().stream().map(MusicAsset::getTaskId).collect(Collectors.toSet());
		Map<Long, MusicTask> taskMap = taskPks.isEmpty()
				? Map.of()
				: musicTaskService.listByIds(taskPks).stream().collect(Collectors.toMap(MusicTask::getId, t -> t));
		List<MusicExploreItemVO> rows = new ArrayList<>(raw.getRecords().size());
		for (MusicAsset a : raw.getRecords()) {
			long likes = musicLikeService.lambdaQuery().eq(MusicLike::getAssetId, a.getId()).count();
			rows.add(MusicExploreItemVO.of(a, taskMap.get(a.getTaskId()), likes));
		}
		Page<MusicExploreItemVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
		out.setRecords(rows);
		return out;
	}
}
