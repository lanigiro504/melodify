/** 与 `/api/client/music-assets` JSON 对齐 */
import type { MusicTask } from './musicTask'

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

export interface MusicAssetDetail {
  asset: MusicAsset
  task?: MusicTask | null
  likeCount: number
  liked: boolean
  prompt?: string | null
  params?: Record<string, unknown> | null
  createTime?: string | null
}
