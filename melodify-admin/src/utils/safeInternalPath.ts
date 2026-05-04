/**
 * 将 redirect 查询参数规范为安全的站内路径，防止开放重定向。
 */
export function safeInternalPath(raw: unknown): string {
  const path = Array.isArray(raw) ? raw[0] : raw
  if (typeof path !== 'string' || path === '') return '/'
  if (!path.startsWith('/') || path.startsWith('//')) return '/'
  return path
}
