import type { PageRecords, PointLog, Result } from '@/types/api'
import { http } from './http'

export const pageMyPointLogs = async (
  current = 1,
  size = 10,
): Promise<Result<PageRecords<PointLog>>> => {
  const { data } = await http.get<Result<PageRecords<PointLog>>>('/client/point-logs/page', {
    params: { current, size },
  })
  return data
}
