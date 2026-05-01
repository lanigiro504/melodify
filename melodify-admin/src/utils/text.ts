/** 表格/卡片中截断过长文本展示 */
export const shortenText = (s: string | null | undefined, max: number): string => {
  const t = (s ?? '').trim()
  return t.length <= max ? t : `${t.slice(0, max)}…`
}
