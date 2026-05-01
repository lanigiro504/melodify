package com.melodify.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 从 JWT Claims 复原的 Spring Security 主体，映射为 {@link org.springframework.security.core.userdetails.UserDetails}
 * 以兼容方法级注解（如 {@code @PreAuthorize}）。
 * <p>自定义 JWT 声明名 {@link #CLAIM_ROLE_KEY}（简称 rk）应与 {@code sys_role.role_key} 一致；
 * 仅当值为 {@code admin}（忽略大小写）时授予 {@code ROLE_ADMIN}。</p>
 */
@Getter
public class MelodifyUserPrincipal implements UserDetails {

	/** JWT 自定义声明字段名（payload key），取值与数据库 {@code sys_role.role_key} 对齐。 */
	public static final String CLAIM_ROLE_KEY = "rk";

	private final Long userId;
	private final String roleKey;

	private final Collection<? extends GrantedAuthority> authorities;

	public MelodifyUserPrincipal(Long userId, String roleKey) {
		this.userId = userId;
		this.roleKey = roleKey == null ? "" : roleKey;
		boolean admin = "admin".equalsIgnoreCase(this.roleKey);
		this.authorities = List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER"));
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return String.valueOf(userId);
	}

	public boolean isAdmin() {
		return getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
