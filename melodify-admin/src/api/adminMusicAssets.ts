import type { MusicAsset, PageRecords, Result } from '@/types/api'
import { http } from './http'

export const pageMusicAssetsAdmin = async (
  current: number,
  size: number,
  options: { userId?: number | undefined; isPublic?: number | undefined } = {},
): Promise<Result<PageRecords<MusicAsset>>> => {
  const params: Record<string, number> = { current, size }
  if (typeof options.userId === 'number') params.userId = options.userId
  if (typeof options.isPublic === 'number') params.isPublic = options.isPublic
  const { data } = await http.get<Result<PageRecords<MusicAsset>>>('/admin/music-assets/page', { params })
  return data
}
