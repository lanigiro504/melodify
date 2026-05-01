package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.common.exception.BizException;
import com.melodify.config.MelodifyUserProperties;
import com.melodify.entity.SysUser;
import com.melodify.mapper.SysUserMapper;
import com.melodify.model.dto.UserLoginDTO;
import com.melodify.model.dto.UserProfileDTO;
import com.melodify.model.dto.UserRegisterDTO;
import com.melodify.security.LegacyPasswordCodec;
import com.melodify.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 《系统用户》业务实现（注册登录、本人资料）。
 * <p>密码当前为兼容性 MD5，后续可与安全框架 {@code PasswordEncoder} 对齐并增加盐值。</p>
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

	private final MelodifyUserProperties melodifyUserProperties;

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
		user.setPassword(LegacyPasswordCodec.md5HexUtf8(rawPassword));
		user.setRoleId(DEFAULT_ROLE_ID);
		user.setStatus(DEFAULT_STATUS_NORMAL);
		// 首登即 0 会导致「单次扣费 generate-cost-points」永远无法通过，故注册赠送可配置额度
		user.setPoints(Math.max(0, melodifyUserProperties.getSignupBonusPoints()));
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

		String hashedInput = LegacyPasswordCodec.md5HexUtf8(rawPassword);
		String stored = user.getPassword();
		if (stored == null || !stored.equals(hashedInput)) {
			throw new BizException(401, "用户名或密码错误");
		}

		user.setPassword(null);
		return user;
	}

	/**
	 * 按需更新昵称/头像/联系方式：仅 dto 中非 {@code null} 的字段会写入数据库，便于前端部分更新。
	 */
	@Override
	public void updateSelfProfile(Long userId, UserProfileDTO dto) {
		if (!lambdaQuery().eq(SysUser::getId, userId).exists()) {
			throw new BizException(404, "用户不存在");
		}
		boolean any = dto.getNickname() != null
				|| dto.getAvatar() != null
				|| dto.getEmail() != null
				|| dto.getPhone() != null;
		if (!any) {
			return;
		}
		boolean ok = lambdaUpdate()
				.eq(SysUser::getId, userId)
				.set(dto.getNickname() != null, SysUser::getNickname, dto.getNickname())
				.set(dto.getAvatar() != null, SysUser::getAvatar, dto.getAvatar())
				.set(dto.getEmail() != null, SysUser::getEmail, dto.getEmail())
				.set(dto.getPhone() != null, SysUser::getPhone, dto.getPhone())
				.update();
		if (!ok) {
			throw new BizException(500, "资料更新失败，请稍后重试");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean adminCreate(SysUser user) {
		String username = normalize(user.getUsername());
		if (!StringUtils.hasText(username)) {
			throw new BizException(400, "用户名不能为空");
		}
		long exists = lambdaQuery().eq(SysUser::getUsername, username).count();
		if (exists > 0) {
			throw new BizException(409, "用户名已存在");
		}
		String rawPassword = normalize(user.getPassword());
		if (!StringUtils.hasText(rawPassword)) {
			throw new BizException(400, "初始密码不能为空");
		}

		user.setUsername(username);
		user.setPassword(LegacyPasswordCodec.md5HexUtf8(rawPassword));
		if (user.getNickname() != null) {
			user.setNickname(normalize(user.getNickname()));
		}
		if (user.getAvatar() != null) {
			user.setAvatar(normalize(user.getAvatar()));
		}
		if (user.getEmail() != null) {
			user.setEmail(normalize(user.getEmail()));
		}
		if (user.getPhone() != null) {
			user.setPhone(normalize(user.getPhone()));
		}
		if (user.getStatus() == null) {
			user.setStatus(DEFAULT_STATUS_NORMAL);
		}
		if (user.getRoleId() == null) {
			user.setRoleId(DEFAULT_ROLE_ID);
		}
		if (user.getPoints() == null) {
			user.setPoints(Math.max(0, melodifyUserProperties.getSignupBonusPoints()));
		}

		boolean ok = save(user);
		if (!ok) {
			throw new BizException(500, "保存用户失败");
		}
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean adminUpdate(Long id, SysUser body) {
		SysUser existing = getById(id);
		if (existing == null) {
			throw new BizException(404, "用户不存在");
		}

		String usernameNorm = normalize(body.getUsername());
		if (StringUtils.hasText(usernameNorm)) {
			long dup = lambdaQuery()
					.eq(SysUser::getUsername, usernameNorm)
					.ne(SysUser::getId, id)
					.count();
			if (dup > 0) {
				throw new BizException(409, "用户名已被占用");
			}
		}

		var uw = lambdaUpdate().eq(SysUser::getId, id);

		boolean any = false;
		if (StringUtils.hasText(usernameNorm)) {
			uw.set(SysUser::getUsername, usernameNorm);
			any = true;
		}
		if (body.getNickname() != null) {
			uw.set(SysUser::getNickname, normalize(body.getNickname()));
			any = true;
		}
		if (body.getAvatar() != null) {
			uw.set(SysUser::getAvatar, normalize(body.getAvatar()));
			any = true;
		}
		if (body.getEmail() != null) {
			uw.set(SysUser::getEmail, normalize(body.getEmail()));
			any = true;
		}
		if (body.getPhone() != null) {
			uw.set(SysUser::getPhone, normalize(body.getPhone()));
			any = true;
		}
		if (body.getPoints() != null) {
			uw.set(SysUser::getPoints, body.getPoints());
			any = true;
		}
		if (body.getRoleId() != null) {
			uw.set(SysUser::getRoleId, body.getRoleId());
			any = true;
		}
		if (body.getStatus() != null) {
			uw.set(SysUser::getStatus, body.getStatus());
			any = true;
		}
		String rawPassword = normalize(body.getPassword());
		if (StringUtils.hasText(rawPassword)) {
			uw.set(SysUser::getPassword, LegacyPasswordCodec.md5HexUtf8(rawPassword));
			any = true;
		}

		if (!any) {
			return true;
		}
		boolean ok = uw.update();
		if (!ok) {
			throw new BizException(500, "更新用户失败");
		}
		return true;
	}
}
