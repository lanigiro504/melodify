/**
 * music_task JSON 映射；状态枚举与后端 {@code MusicTaskStatuses} 一致。
 */
export const MUSIC_TASK_STATUS = {
  QUEUED: 0,
  GENERATING: 1,
  SUCCEEDED: 2,
  FAILED: 3,
  CANCELLED: 4,
} as const

export function musicTaskStatusText(status: number | null | undefined): string {
  switch (status) {
    case MUSIC_TASK_STATUS.QUEUED:
      return '排队中'
    case MUSIC_TASK_STATUS.GENERATING:
      return '生成中'
    case MUSIC_TASK_STATUS.SUCCEEDED:
      return '已完成'
    case MUSIC_TASK_STATUS.FAILED:
      return '失败'
    case MUSIC_TASK_STATUS.CANCELLED:
      return '已取消'
    default:
      return '未知状态'
  }
}

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
