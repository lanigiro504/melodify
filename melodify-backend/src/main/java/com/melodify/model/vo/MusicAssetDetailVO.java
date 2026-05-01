package com.melodify.model.vo;

import com.melodify.entity.MusicAsset;
import com.melodify.entity.MusicTask;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Value
public class MusicAssetDetailVO {
	MusicAsset asset;
	MusicTask task;
	long likeCount;
	boolean liked;

	public String getPrompt() {
		return task != null ? task.getPrompt() : "";
	}

	public Map<String, Object> getParams() {
		return task != null ? task.getParams() : null;
	}

	public LocalDateTime getCreateTime() {
		return asset != null ? asset.getCreateTime() : null;
	}
}
