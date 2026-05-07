import type { Result } from '@/types/api'
import { http } from './http'

export interface AdminDashboardSummary {
  usersTotal: number
  musicTasksGenerating: number
  musicTasksTodayCreated: number
  musicTasksTodaySucceeded: number
  musicTasksTodayFailed: number
  rechargeOrdersPaidTodayCount: number
  rechargePaidTodayCentTotal: number
  rechargePaidTodayYuanApprox: number
  publicAssetsTotal: number
}

export const fetchDashboardSummary = async (): Promise<Result<AdminDashboardSummary>> => {
  const { data } = await http.get<Result<AdminDashboardSummary>>('/admin/dashboard/summary')
  return data
}
