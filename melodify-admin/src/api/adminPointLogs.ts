import type { PageRecords, PointLog, Result } from '@/types/api'
import { http } from './http'

export const pagePointLogsAdmin = async (
  current: number,
  size: number,
  options: { userId?: number | undefined; bizType?: string | undefined } = {},
): Promise<Result<PageRecords<PointLog>>> => {
  const params: Record<string, number | string> = { current, size }
  if (typeof options.userId === 'number') params.userId = options.userId
  const b = options.bizType?.trim()
  if (b) params.bizType = b
  const { data } = await http.get<Result<PageRecords<PointLog>>>('/admin/point-logs/page', { params })
  return data
}
