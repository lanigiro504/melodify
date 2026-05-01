import type { MusicTask } from '@/types/musicTask'
import type { MusicGenerateBody, MusicGenerateSubmit, PageRecords, Result } from '@/types/api'
import { http } from './http'

/**
 * `POST /api/client/music-tasks/generate`：需要先登录以携带 Bearer。
 */
export const submitMusicGenerate = async (
  payload: MusicGenerateBody,
): Promise<Result<MusicGenerateSubmit>> => {
  const { data } = await http.post<Result<MusicGenerateSubmit>>(
    '/client/music-tasks/generate',
    payload,
  )
  return data
}

/**
 * 轮询：`GET …/music-tasks/by-task-id/:taskId`。
 */
export const getMusicTaskByBusinessId = async (
  taskId: string,
): Promise<Result<MusicTask>> => {
  const encoded = encodeURIComponent(taskId.trim())
  const { data } = await http.get<Result<MusicTask>>(
    `/client/music-tasks/by-task-id/${encoded}`,
  )
  return data
}

/** 分页：本人任务，`current`/`size` 与后端默认值一致 */
export const pageMusicTasks = async (
  current = 1,
  size = 10,
): Promise<Result<PageRecords<MusicTask>>> => {
  const { data } = await http.get<Result<PageRecords<MusicTask>>>(
    '/client/music-tasks/page',
    {
      params: { current, size },
    },
  )
  return data
}
