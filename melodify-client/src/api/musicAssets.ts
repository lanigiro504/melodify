import type { MusicAsset, MusicAssetDetail } from '@/types/musicAsset'
import type { Result } from '@/types/api'
import { http } from './http'

/**
 * 任务成功后按业务 {@code music_task.task_id} 拉取第一条成品音频。
 */
export const getMusicAssetByBusinessTask = async (
  taskBizId: string,
): Promise<Result<MusicAsset>> => {
  const encoded = encodeURIComponent(taskBizId.trim())
  const { data } = await http.get<Result<MusicAsset>>(
    `/client/music-assets/by-business-task/${encoded}`,
  )
  return data
}

export const getMusicAssetDetail = async (id: number): Promise<Result<MusicAssetDetail>> => {
  const { data } = await http.get<Result<MusicAssetDetail>>(`/client/music-assets/${id}`)
  return data
}

export const likeMusicAsset = async (id: number): Promise<Result<boolean>> => {
  const { data } = await http.post<Result<boolean>>(`/client/music-assets/${id}/like`)
  return data
}

export const unlikeMusicAsset = async (id: number): Promise<Result<boolean>> => {
  const { data } = await http.delete<Result<boolean>>(`/client/music-assets/${id}/like`)
  return data
}

export const patchMusicAssetPublic = async (
  id: number,
  isPublic: 0 | 1,
): Promise<Result<boolean>> => {
  const { data } = await http.patch<Result<boolean>>(`/client/music-assets/${id}/public`, {
    isPublic,
  })
  return data
}
