package com.melodify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.melodify.entity.SysUser;
import com.melodify.model.dto.UserProfileDTO;
import com.melodify.model.dto.UserLoginDTO;
import com.melodify.model.dto.UserRegisterDTO;
import org.springframework.web.multipart.MultipartFile;

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

	/**
	 * 更新本人可写字段（昵称、头像、邮箱、手机）；不传或 null 的字段不修改。
	 */
	void updateSelfProfile(Long userId, UserProfileDTO dto);

	/**
	 * 本地上传头像并写入 {@code avatar}；返回对外访问路径。
	 */
	String uploadSelfAvatar(Long userId, MultipartFile file);

	/**
	 * 管理端新建用户：校验用户名唯一，明文密码 MD5 入库（与登录校验一致）。
	 */
	boolean adminCreate(SysUser user);

	/**
	 * 管理端更新用户：可改用户名/昵称/头像/联系方式/积分/角色/状态；密码非空时按 MD5 覆盖写入。
	 */
	boolean adminUpdate(Long id, SysUser body);
}
