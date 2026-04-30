package com.melodify.controller.client;

import com.melodify.common.result.Result;
import com.melodify.entity.SysUser;
import com.melodify.model.dto.UserLoginDTO;
import com.melodify.model.dto.UserRegisterDTO;
import com.melodify.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端认证：注册与登录。前缀符合规范 {@code /api/client/**}。
 * <p>
 * 密码暂使用 MD5（{@link org.springframework.util.DigestUtils#md5DigestAsHex}），后续可替换为加盐哈希与安全框架。
 * 业务异常由 {@link com.melodify.common.exception.GlobalExceptionHandler} 统一转换为 {@link Result}。
 * </p>
 */
@RestController
@RequestMapping("/api/client/auth")
@RequiredArgsConstructor
public class ClientAuthController {

	private final SysUserService sysUserService;

	/**
	 * 注册新用户，默认角色为普通用户（role_id = 2）。
	 */
	@PostMapping("/register")
	public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
		sysUserService.register(dto);
		return Result.success();
	}

	/**
	 * 登录成功返回用户信息；{@code password} 已置为 {@code null}，避免泄露。
	 */
	@PostMapping("/login")
	public Result<SysUser> login(@Valid @RequestBody UserLoginDTO dto) {
		return Result.success(sysUserService.login(dto));
	}
}
