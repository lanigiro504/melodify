package com.melodify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.SysUser;
import com.melodify.model.dto.UserLoginDTO;
import com.melodify.model.dto.UserRegisterDTO;

public interface SysUserService extends IService<SysUser> {

	/**
	 * 用户注册：校验用户名唯一，密码 MD5 入库，默认普通角色（role_id = 2）。
	 *
	 * @param dto 注册入参
	 * @throws com.melodify.common.exception.BizException 用户名已存在等业务错误
	 */
	void register(UserRegisterDTO dto);

	/**
	 * 用户登录：校验账号状态与密码，返回用户信息且密码字段置空。
	 *
	 * @param dto 登录入参
	 * @return 不含 password 的用户实体
	 * @throws com.melodify.common.exception.BizException 认证失败或账号封禁
	 */
	SysUser login(UserLoginDTO dto);
}
