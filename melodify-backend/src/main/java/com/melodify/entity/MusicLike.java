package com.melodify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("music_like")
public class MusicLike {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long assetId;

	private Long userId;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;
}
