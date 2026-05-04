/** 列表/详情等处统一展示本地化日期时间（含日与时刻之间的可读间距） */
export function formatDateTimeZh(raw: string | null | undefined): string {
  if (raw == null) return '—'
  const s = String(raw).trim()
  if (!s) return '—'
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) {
    return s.replace('T', ' ').replace(/(\.\d+)?Z?$/, '')
  }
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day}\u3000${hh}:${mm}`
}
