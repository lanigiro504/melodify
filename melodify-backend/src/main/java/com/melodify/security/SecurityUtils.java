package com.melodify.security;

import com.melodify.common.exception.BizException;
import com.melodify.common.exception.GlobalExceptionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 {@link SecurityContextHolder} 读取本次请求对应的 JWT 用户，供 Controller / Service 做数据归属校验。
 */
public final class SecurityUtils {

	private SecurityUtils() {}

	/**
	 * @return 已认证则返回主体；匿名或令牌未解析时为 {@code null}
	 */
	public static MelodifyUserPrincipal currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		if (authentication.getPrincipal() instanceof MelodifyUserPrincipal principal) {
			return principal;
		}
		return null;
	}

	/**
	 * @throws BizException HTTP 语义由异常码表达（默认 401），由全局处理器包装为 JSON
	 */
	public static MelodifyUserPrincipal requirePrincipal() {
		MelodifyUserPrincipal principal = currentPrincipal();
		if (principal == null) {
			throw new BizException(GlobalExceptionHandler.CODE_UNAUTHORIZED, "未登录");
		}
		return principal;
	}

	/** 当前登录用户数据库主键。 */
	public static Long requireUserId() {
		return requirePrincipal().getUserId();
	}

	/**
	 * 资源所有者与当前用户不一致时拒绝；{@code ROLE_ADMIN} 放行以便后台排障接口复用客户端路径的场景。
	 */
	public static void requireOwnershipOrAdmin(Long resourceOwnerUserId) {
		MelodifyUserPrincipal p = requirePrincipal();
		if (!p.isAdmin() && !p.getUserId().equals(resourceOwnerUserId)) {
			throw new BizException(403, "无权访问该资源");
		}
	}
}
