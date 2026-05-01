package com.melodify.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MusicAssetPublicPatchDTO {

	/** 0 仅自己可见；1 进入公开广场 */
	@NotNull
	private Integer isPublic;
}
