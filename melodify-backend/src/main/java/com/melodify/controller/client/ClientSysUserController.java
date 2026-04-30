package com.melodify.controller.client;

import com.melodify.common.result.Result;
import com.melodify.entity.SysUser;
import com.melodify.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端：当前用户资料（对应 {@code sys_user} 表）。路径前缀固定为 {@code /api/client/**}。
 * <p>
 * 仅提供查看与更新个人资料能力，不包含注册登录、全量用户列表（列表归属管理端）。
 * 修改密码、重置密码等应单独设计接口并做好加密与频控。
 * </p>
 * <p>
 * 暂未接入认证时通过路径中的用户主键访问；接入后应改为 {@code GET /me} 等形式并从会话解析身份。
 * </p>
 */
@RestController
@RequestMapping("/api/client/users")
@RequiredArgsConstructor
public class ClientSysUserController {

	private final SysUserService sysUserService;

	/**
	 * 查询用户基本资料（昵称、头像、积分等）。敏感字段是否脱敏由前端与 DTO 演进时再行拆分。
	 */
	@GetMapping("/{id}")
	public Result<SysUser> getById(@PathVariable Long id) {
		return Result.success(sysUserService.getById(id));
	}

	/**
	 * 更新当前用户展示信息。调用方应只提交允许修改的字段；亦可通过专用 DTO 限制可写列。
	 */
	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody SysUser body) {
		body.setId(id);
		return Result.success(sysUserService.updateById(body));
	}
}
