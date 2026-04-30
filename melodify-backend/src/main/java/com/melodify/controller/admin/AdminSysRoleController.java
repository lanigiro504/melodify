package com.melodify.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.result.Result;
import com.melodify.entity.SysRole;
import com.melodify.service.SysRoleService;
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
 * 管理端：系统角色维护。路径前缀固定为 {@code /api/admin/**}。
 * <p>
 * 提供角色的增删改查与分页列表，供后台运营或超级管理员使用。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminSysRoleController {

	private final SysRoleService sysRoleService;

	/**
	 * 分页查询角色列表。
	 *
	 * @param current 当前页，从 1 开始
	 * @param size    每页条数
	 */
	@GetMapping("/page")
	public Result<IPage<SysRole>> page(
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		return Result.success(sysRoleService.page(new Page<>(current, size)));
	}

	/**
	 * 根据主键查询单个角色。
	 */
	@GetMapping("/{id}")
	public Result<SysRole> getById(@PathVariable Long id) {
		return Result.success(sysRoleService.getById(id));
	}

	/**
	 * 新增角色。注意 {@code roleKey} 在库中唯一，重复将触发数据库约束异常。
	 */
	@PostMapping
	public Result<Boolean> create(@RequestBody SysRole body) {
		return Result.success(sysRoleService.save(body));
	}

	/**
	 * 全量更新角色（按主键）。仅传需要修改的字段时，建议后续改为 DTO + 部分更新接口。
	 */
	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody SysRole body) {
		body.setId(id);
		return Result.success(sysRoleService.updateById(body));
	}

	/**
	 * 按主键删除（若实体启用逻辑删除，则实际为更新 {@code is_deleted}）。
	 */
	@DeleteMapping("/{id}")
	public Result<Boolean> remove(@PathVariable Long id) {
		return Result.success(sysRoleService.removeById(id));
	}
}
