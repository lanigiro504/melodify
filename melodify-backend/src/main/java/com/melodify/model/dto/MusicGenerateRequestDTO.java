package com.melodify.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 用户发起生成的入参（提示词、歌词、结构化参数等），映射到 {@link com.melodify.entity.MusicTask}。
 */
@Data
public class MusicGenerateRequestDTO {

	/** 模型/配方标识（如 melodify-v1），由运营或字典维护。 */
	@NotBlank
	@Size(max = 64)
	private String modelCode;

	/** 文本提示词；Suno 非自定义至多约 500 字，自定义高阶模型至多约 5000 字，此处放宽由前端按模式提示。 */
	@Size(max = 5000)
	private String prompt;

	/** 歌词全文或分段，最终会并入 {@code params} JSON 中的 {@code lyrics} 键以便扩展。 */
	@Size(max = 8000)
	private String lyrics;

	/**
	 * 其它结构化字段：曲风 / 心情 / 音色 BPM 等，直接持久化至 {@code music_task.params}。
	 */
	private Map<String, Object> params;
}
