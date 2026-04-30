package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.common.exception.BizException;
import com.melodify.entity.SysUser;
import com.melodify.mapper.SysUserMapper;
import com.melodify.model.dto.UserLoginDTO;
import com.melodify.model.dto.UserRegisterDTO;
import com.melodify.service.SysUserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

	private static final long DEFAULT_ROLE_ID = 2L;
	private static final int DEFAULT_STATUS_NORMAL = 1;
	private static final int STATUS_BANNED = 0;

	private static String normalize(String text) {
		return text == null ? "" : text.strip();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(UserRegisterDTO dto) {
		String username = normalize(dto.getUsername());
		String rawPassword = normalize(dto.getPassword());
		if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
			throw new BizException(400, "用户名或密码不能为空");
		}

		long exists = lambdaQuery().eq(SysUser::getUsername, username).count();
		if (exists > 0) {
			throw new BizException(409, "用户名已存在");
		}

		SysUser user = new SysUser();
		user.setUsername(username);
		user.setPassword(DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8)));
		user.setRoleId(DEFAULT_ROLE_ID);
		user.setStatus(DEFAULT_STATUS_NORMAL);
		user.setPoints(0);
		user.setAvatar("");
		user.setEmail("");
		user.setPhone("");

		try {
			boolean ok = save(user);
			if (!ok) {
				throw new BizException(500, "注册失败，请稍后重试");
			}
		} catch (DataIntegrityViolationException e) {
			throw new BizException(409, "用户名已存在");
		}
	}

	@Override
	public SysUser login(UserLoginDTO dto) {
		String username = normalize(dto.getUsername());
		String rawPassword = normalize(dto.getPassword());
		if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
			throw new BizException(400, "用户名或密码不能为空");
		}

		SysUser user = getOne(
				Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username),
				false);
		if (user == null) {
			throw new BizException(401, "用户名或密码错误");
		}
		if (user.getStatus() != null && user.getStatus() == STATUS_BANNED) {
			throw new BizException(403, "账号已被封禁");
		}

		String hashedInput = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
		String stored = user.getPassword();
		if (stored == null || !stored.equals(hashedInput)) {
			throw new BizException(401, "用户名或密码错误");
		}

		user.setPassword(null);
		return user;
	}
}
