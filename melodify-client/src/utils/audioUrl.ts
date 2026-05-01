/**
 * 将后端的 fileUrl 转为浏览器可播放地址：绝对 http(s) 原样返回；相对路径拼 VITE_API_BASE_URL（与 axios 一致）。
 */
export function resolvePlayableUrl(url: string | null | undefined): string {
  const u = (url ?? '').trim()
  if (!u) return ''
  if (u.startsWith('http://') || u.startsWith('https://')) return u
  const base =
    typeof import.meta.env.VITE_API_BASE_URL === 'string' ? import.meta.env.VITE_API_BASE_URL : ''
  if (u.startsWith('/')) return `${base}${u}`
  return `${base}/${u}`
}
