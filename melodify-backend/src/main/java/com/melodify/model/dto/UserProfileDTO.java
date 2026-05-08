package com.melodify.model.dto;

import jakarta.validation.constraints.Size;

/**
 * 用户端「修改本人资料」允许的字段；不包含密码、角色、积分等敏感列，由服务层按主键更新。
 */
public class UserProfileDTO {

	@Size(max = 50)
	private String nickname;

	@Size(max = 255)
	private String avatar;

	@Size(max = 100)
	private String email;

	@Size(max = 20)
	private String phone;

	@Size(max = 20)
	private String qq;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getQq() {
		return qq;
	}

	public void setQq(String qq) {
		this.qq = qq;
	}
}
