/** 与用户端 `/api/client/music-tasks/**` JSON 对齐的实体子集（分页与详情复用）。 */
export interface MusicTask {
  id: number
  taskId: string
  /** SunoAPI 返回的远端 taskId，用于 record-info（若有） */
  vendorTaskId?: string | null
  userId: number
  modelCode: string
  prompt?: string | null
  params?: Record<string, unknown> | null
  status?: number | null
  errorCode?: string | null
  errorMessage?: string | null
  costPoints?: number | null
  startedAt?: string | null
  finishedAt?: string | null
  createTime?: string | null
  updateTime?: string | null
}
