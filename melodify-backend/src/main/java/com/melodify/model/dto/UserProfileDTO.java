package com.melodify.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户端「修改本人资料」允许的字段；不包含密码、角色、积分等敏感列，由服务层按主键更新。
 */
@Data
public class UserProfileDTO {

	@Size(max = 50)
	private String nickname;

	@Size(max = 255)
	private String avatar;

	@Size(max = 100)
	private String email;

	@Size(max = 20)
	private String phone;
}
