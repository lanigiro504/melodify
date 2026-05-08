/**
 * 将用户 {@code avatar} 字段解析为可用于 {@code img} / {@code background-image} 的地址：
 * 外链原样返回；相对路径（如 {@code /api/media/avatar/…}）在配置了绝对 {@code VITE_API_BASE_URL} 时拼到 API 域名下。
 */
export const avatarDisplayUrl = (raw: string | null | undefined): string => {
  const s = raw?.trim()
  if (!s) return ''
  if (/^https?:\/\//i.test(s)) return s
  const baseURL =
    typeof import.meta.env.VITE_API_BASE_URL === 'string' ? import.meta.env.VITE_API_BASE_URL : ''
  if (baseURL.startsWith('http') && s.startsWith('/')) {
    try {
      return new URL(s, new URL(baseURL).origin).toString()
    } catch {
      return s
    }
  }
  return s
}
