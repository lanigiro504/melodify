/**
 * 后台管理端与用户端隔离的 JWT 存储键名，避免同域同时调试时互相覆盖。
 */
export const ADMIN_JWT_STORAGE_KEY = 'melodify:admin:token'

/** 读取当前令牌；不可用场景返回 null。 */
export function getStoredToken(): string | null {
  try {
    return sessionStorage.getItem(ADMIN_JWT_STORAGE_KEY)
  } catch {
    return null
  }
}

/** @param token 传入 null 或空串表示清除 */
export const setStoredToken = (token: string | null): void => {
  try {
    if (token == null || token === '') {
      sessionStorage.removeItem(ADMIN_JWT_STORAGE_KEY)
    } else {
      sessionStorage.setItem(ADMIN_JWT_STORAGE_KEY, token)
    }
  } catch {
    /* 存储不可用 */
  }
}

/** 登出时清理凭证（供 http 拦截器复用） */
export const clearAdminCredentialStorage = (): void => {
  try {
    sessionStorage.removeItem(ADMIN_JWT_STORAGE_KEY)
    sessionStorage.removeItem(ADMIN_USER_SNAPSHOT_KEY)
  } catch {
    /* noop */
  }
}

export const ADMIN_USER_SNAPSHOT_KEY = 'melodify:admin:user'
