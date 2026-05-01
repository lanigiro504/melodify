import type { PageRecords, RechargeOrder, Result } from '@/types/api'
import { http } from './http'

export const pageRechargeOrdersAdmin = async (
  current: number,
  size: number,
  options: { userId?: number | undefined; status?: number | undefined } = {},
): Promise<Result<PageRecords<RechargeOrder>>> => {
  const params: Record<string, number> = { current, size }
  if (typeof options.userId === 'number') params.userId = options.userId
  if (typeof options.status === 'number') params.status = options.status
  const { data } = await http.get<Result<PageRecords<RechargeOrder>>>('/admin/recharge-orders/page', {
    params,
  })
  return data
}
