package com.melodify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("music_asset")
public class MusicAsset {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 资产编号，对应列 asset_id
	 */
	private String assetId;

	private Long taskId;

	private Long userId;

	private String title;

	private String fileUrl;

	private String coverUrl;

	private Integer durationSec;

	private String format;

	private Integer bitrateKbps;

	private Integer isPublic;

	private String licenseType;

	private Integer status;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;
}
