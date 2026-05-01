package com.melodify.controller.client;

import com.melodify.common.result.Result;
import com.melodify.entity.SysRole;
import com.melodify.entity.SysUser;
import com.melodify.model.dto.LoginResponseDTO;
import com.melodify.model.dto.UserLoginDTO;
import com.melodify.model.dto.UserRegisterDTO;
import com.melodify.security.JwtService;
import com.melodify.service.SysRoleService;
import com.melodify.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端认证：匿名可访问注册与登录，登录成功后签发 JWT。
 */
@RestController
@RequestMapping("/api/client/auth")
@RequiredArgsConstructor
public class ClientAuthController {

	private final SysUserService sysUserService;
	private final SysRoleService sysRoleService;
	private final JwtService jwtService;

	/** 注册默认角色 {@code role_id = 2}（普通用户），由 {@link SysUserService#register} 写入。 */
	@PostMapping("/register")
	public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
		sysUserService.register(dto);
		return Result.success();
	}

	/**
	 * 校验账号密码后返回访问令牌与用户快照；
	 * 前端应将 {@link LoginResponseDTO#getToken()} 置于 {@code Authorization: Bearer …}。
	 */
	@PostMapping("/login")
	public Result<LoginResponseDTO> login(@Valid @RequestBody UserLoginDTO dto) {
		SysUser user = sysUserService.login(dto);
		String roleKey = resolveRoleKey(user.getRoleId());
		String token = jwtService.createAccessToken(user.getId(), roleKey);
		return Result.success(new LoginResponseDTO(token, user));
	}

	/** 将用户表外键转换为 JWT 载荷中的 rk，缺省回落为 {@code common}。 */
	private String resolveRoleKey(Long roleId) {
		if (roleId == null) {
			return "common";
		}
		SysRole role = sysRoleService.getById(roleId);
		return role != null && role.getRoleKey() != null ? role.getRoleKey() : "common";
	}
}
