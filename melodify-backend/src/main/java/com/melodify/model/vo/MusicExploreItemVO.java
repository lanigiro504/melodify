package com.melodify.model.vo;

import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicTask;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class MusicExploreItemVO {
	Long id;
	String assetId;
	String title;
	String fileUrl;
	String coverUrl;
	Integer durationSec;
	String prompt;
	long likeCount;
	/** 当前请求用户是否已点赞；未登录或未带 JWT 时为 false */
	boolean liked;
	LocalDateTime createTime;

	public static MusicExploreItemVO of(MusicAsset a, MusicTask task, long likeCount, boolean liked) {
		return new MusicExploreItemVO(
				a.getId(),
				a.getAssetId(),
				a.getTitle(),
				a.getFileUrl(),
				a.getCoverUrl(),
				a.getDurationSec(),
				task != null ? task.getPrompt() : "",
				likeCount,
				liked,
				a.getCreateTime());
	}
}
