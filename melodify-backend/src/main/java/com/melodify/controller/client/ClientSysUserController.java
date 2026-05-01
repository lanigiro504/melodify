package com.melodify.controller.client;

import com.melodify.common.result.Result;
import com.melodify.entity.SysUser;
import com.melodify.model.dto.UserProfileDTO;
import com.melodify.security.SecurityUtils;
import com.melodify.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户资料：仅从 JWT 解析身份，禁止使用路径伪造他人主键。
 */
@RestController
@RequestMapping("/api/client/users")
@RequiredArgsConstructor
public class ClientSysUserController {

	private final SysUserService sysUserService;

	/** 返回本人 {@link SysUser}，密码字段为 {@code null}。 */
	@GetMapping("/me")
	public Result<SysUser> me() {
		Long id = SecurityUtils.requireUserId();
		SysUser user = sysUserService.getById(id);
		if (user != null) {
			user.setPassword(null);
		}
		return Result.success(user);
	}

	/** 部分字段更新昵称、头像与联系方式（见 {@link UserProfileDTO}）。 */
	@PutMapping("/me")
	public Result<Void> updateMe(@Valid @RequestBody UserProfileDTO dto) {
		Long id = SecurityUtils.requireUserId();
		sysUserService.updateSelfProfile(id, dto);
		return Result.success();
	}
}
