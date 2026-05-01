import type { MusicTask, PageRecords, Result } from '@/types/api'
import { http } from './http'

export const pageMusicTasksAdmin = async (
  current: number,
  size: number,
  options: { userId?: number | undefined; status?: number | undefined } = {},
): Promise<Result<PageRecords<MusicTask>>> => {
  const params: Record<string, number> = { current, size }
  if (typeof options.userId === 'number') params.userId = options.userId
  if (typeof options.status === 'number') params.status = options.status
  const { data } = await http.get<Result<PageRecords<MusicTask>>>('/admin/music-tasks/page', { params })
  return data
}
