/**
 * 从任务侧字段提取可展示歌词。
 *
 * 注意：普通 prompt 多数是「创作描述」，不是歌词。这里仅在以下情况返回歌词：
 * - params.lyrics 明确存在；
 * - prompt 本身像 LRC 或多行歌词（兼容旧版「歌词写在 prompt」的任务）；
 * - 自定义人声模式下不再默认把整个 prompt 当歌词（避免创作描述误入播放器）。
 */
export function extractTrackLyrics(
  prompt?: string | null,
  params?: Record<string, unknown> | null,
): string | undefined {
  const fromParams = params?.lyrics
  if (typeof fromParams === 'string' && fromParams.trim()) {
    return fromParams.trim()
  }

  const text = prompt?.trim()
  if (!text) {
    return undefined
  }

  if (looksLikeTimedOrMultilineLyric(text)) {
    return text
  }

  const customMode = params?.customMode === true || params?.customMode === 'true'
  const instrumental = params?.instrumental === true || params?.instrumental === 'true'
  if (customMode && !instrumental) {
    // 新人声自定义：歌词在 params.lyrics；仅 prompt 的旧任务若多行/LRC 仍当作歌词。
    if (looksLikeTimedOrMultilineLyric(text)) {
      return text
    }
    return undefined
  }
  return undefined
}

export function splitLyricLines(text: string): string[] {
  return text
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l.length > 0)
}

function looksLikeTimedOrMultilineLyric(text: string): boolean {
  if (/\[\d{1,2}:\d{2}(?:\.\d{1,3})?\]/.test(text)) {
    return true
  }
  const lines = splitLyricLines(text)
  return lines.length >= 2
}
