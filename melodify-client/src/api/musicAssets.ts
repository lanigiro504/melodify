import type { MusicAsset } from '@/types/musicAsset'
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
