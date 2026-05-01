import type { PageRecords, Result } from '@/types/api'
import { http } from './http'

export interface ExploreAssetItem {
  id: number
  assetId: string
  title?: string | null
  fileUrl: string
  coverUrl?: string | null
  durationSec?: number | null
  prompt?: string | null
  likeCount: number
  createTime?: string | null
}

export const pageExploreAssets = async (
  current = 1,
  size = 12,
): Promise<Result<PageRecords<ExploreAssetItem>>> => {
  const { data } = await http.get<Result<PageRecords<ExploreAssetItem>>>(
    '/public/explore/assets',
    { params: { current, size } },
  )
  return data
}
