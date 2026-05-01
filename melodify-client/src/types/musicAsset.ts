/** 与 `/api/client/music-assets` JSON 对齐 */
export interface MusicAsset {
  id: number
  assetId: string
  taskId: number
  userId: number
  title?: string | null
  fileUrl: string
  coverUrl?: string | null
  durationSec?: number | null
  format?: string | null
  bitrateKbps?: number | null
  isPublic?: number | null
  licenseType?: string | null
  status?: number | null
  createTime?: string | null
  updateTime?: string | null
}
