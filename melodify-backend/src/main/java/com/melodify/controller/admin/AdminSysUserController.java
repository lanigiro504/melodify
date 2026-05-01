package com.melodify.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.melodify.common.result.Result;
import com.melodify.entity.SysUser;
import com.melodify.service.SysUserService;
import com.melodify.support.PagingNormalize;
import com.melodify.support.SysUserSecrets;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：系统用户维护。路径前缀固定为 {@code /api/admin/**}。
 * <p>
 * 用于后台查询、创建、修改、禁用相关用户数据。密码字段应在业务层加密后再入库，勿明文存储。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminSysUserController {

	private final SysUserService sysUserService;

	/**
	 * 分页查询用户列表。
	 *
	 * @param current 当前页
	 * @param size    每页条数
	 */
	@GetMapping("/page")
	public Result<IPage<SysUser>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		IPage<SysUser> pg = sysUserService.page(PagingNormalize.page(current, size));
		SysUserSecrets.maskPasswordPage(pg);
		return Result.success(pg);
	}

	/**
	 * 根据主键查询用户详情。
	 */
	@GetMapping("/{id}")
	public Result<SysUser> getById(@PathVariable Long id) {
		SysUser u = sysUserService.getById(id);
		SysUserSecrets.maskPassword(u);
		return Result.success(u);
	}

	/**
	 * 新增用户。默认角色等字段可由调用方在 body 中指定；密码须事先按系统约定加密。
	 */
	@PostMapping
	public Result<Boolean> create(@RequestBody SysUser body) {
		return Result.success(sysUserService.adminCreate(body));
	}

	/**
	 * 按主键更新用户信息。
	 */
	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody SysUser body) {
		return Result.success(sysUserService.adminUpdate(id, body));
	}

	/**
	 * 删除用户（逻辑删除场景下仅标记 {@code is_deleted}）。
	 */
	@DeleteMapping("/{id}")
	public Result<Boolean> remove(@PathVariable Long id) {
		return Result.success(sysUserService.removeById(id));
	}
}
