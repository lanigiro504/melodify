/**
 * 将路由 query.redirect 规范为安全的站内路径，防止开放重定向。
 * @param raw 一般为 string；兼容 Vue Router 将重复 query 解析为 string[] 的情况
 * @returns 以 / 开头的相对站内路径，非法输入则回退为 /
 */
export const safeInternalPath = (raw: unknown): string => {
  const path = Array.isArray(raw) ? raw[0] : raw
  if (typeof path !== 'string' || path === '') return '/'
  if (!path.startsWith('/') || path.startsWith('//')) return '/'
  return path
}
