/**
 * 浏览器 sessionStorage 中的 JWT 读写工具；
 * Bearer 前缀在 {@code src/api/http.ts} 的请求拦截器中统一拼装。
 */
export const CLIENT_JWT_STORAGE_KEY = 'melodify:client:token'

/** 读取当前令牌；隐私模式或禁用存储时返回 null。 */
export function getStoredToken(): string | null {
  try {
    return sessionStorage.getItem(CLIENT_JWT_STORAGE_KEY)
  } catch {
    return null
  }
}

/**
 * @param token 传入 null / 空串表示登出清除
 */
export function setStoredToken(token: string | null): void {
  try {
    if (token == null || token === '') {
      sessionStorage.removeItem(CLIENT_JWT_STORAGE_KEY)
    } else {
      sessionStorage.setItem(CLIENT_JWT_STORAGE_KEY, token)
    }
  } catch {
    /* 存储配额已满或不可用：静默失败 */
  }
}
