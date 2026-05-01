/** 与后端 {@code MusicTaskStatuses} 一致 */
export const MUSIC_TASK_STATUS = {
  QUEUED: 0,
  GENERATING: 1,
  SUCCEEDED: 2,
  FAILED: 3,
  CANCELLED: 4,
} as const

export const musicTaskStatusLabel = (status: number | null | undefined): string => {
  switch (status) {
    case MUSIC_TASK_STATUS.QUEUED:
      return '排队'
    case MUSIC_TASK_STATUS.GENERATING:
      return '生成中'
    case MUSIC_TASK_STATUS.SUCCEEDED:
      return '已完成'
    case MUSIC_TASK_STATUS.FAILED:
      return '失败'
    case MUSIC_TASK_STATUS.CANCELLED:
      return '已取消'
    default:
      return '未知'
  }
}
