/**
 * 从任务侧字段拼出用于展示的「静态歌词」：优先 params.lyrics，否则用 prompt。
 * 按行分割在播放器内按需进行。
 */
export function extractTrackLyrics(
  prompt?: string | null,
  params?: Record<string, unknown> | null,
): string | undefined {
  const fromParams = params?.lyrics
  if (typeof fromParams === 'string' && fromParams.trim()) {
    return fromParams.trim()
  }
  if (prompt?.trim()) {
    return prompt.trim()
  }
  return undefined
}

export function splitLyricLines(text: string): string[] {
  return text
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l.length > 0)
}
