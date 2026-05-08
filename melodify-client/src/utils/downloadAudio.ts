import { resolvePlayableUrl } from '@/utils/audioUrl'

const INVALID_FILENAME_CHARS = /[/\\?%*:|"<>]/g

export function sanitizeDownloadBaseName(name: string, maxLen = 80): string {
  const t = (name ?? '').trim().replace(INVALID_FILENAME_CHARS, '_')
  return (t.slice(0, maxLen) || 'melodify-track').trim() || 'melodify-track'
}

function pickExtension(blob: Blob, fallback: string): string {
  const t = (blob.type || '').toLowerCase()
  if (t.includes('mpeg') || t.includes('mp3')) return 'mp3'
  if (t.includes('wav')) return 'wav'
  const f = (fallback ?? '').trim().replace(/^\./, '')
  return f || 'mp3'
}

function triggerBlobDownload(blob: Blob, filename: string) {
  const u = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = u
  a.download = filename
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(u), 2000)
}

/**
 * 按 fileUrl（与播放同源逻辑）触发本机下载。同源或允许 CORS 时用 Blob；否则新开标签页由浏览器直连。
 */
export async function downloadAudioByFileUrl(
  fileUrl: string | null | undefined,
  baseName: string,
  fallbackExt = 'mp3',
): Promise<void> {
  const resolved = resolvePlayableUrl(fileUrl)
  if (!resolved) throw new Error('missing file url')

  const safe = sanitizeDownloadBaseName(baseName)
  try {
    const res = await fetch(resolved, { mode: 'cors', credentials: 'omit' })
    if (!res.ok) throw new Error(`http ${res.status}`)
    const blob = await res.blob()
    const ext = pickExtension(blob, fallbackExt)
    triggerBlobDownload(blob, `${safe}.${ext}`)
  } catch {
    window.open(resolved, '_blank', 'noopener,noreferrer')
  }
}
