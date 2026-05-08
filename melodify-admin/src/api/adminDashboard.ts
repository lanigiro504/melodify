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
}

export interface MusicTaskStatusCount {
  status: number
  count: number
}

export interface AdminDashboardTrends {
  dayLabels: string[]
  musicTasksCreatedPerDay: number[]
  musicTasksSucceededPerDay: number[]
  musicTasksFailedPerDay: number[]
  usersRegisteredPerDay: number[]
  rechargePaidOrdersPerDay: number[]
  rechargePaidCentSumPerDay: number[]
  musicTaskStatusDistribution: MusicTaskStatusCount[]
}

export const fetchDashboardSummary = async (): Promise<Result<AdminDashboardSummary>> => {
  const { data } = await http.get<Result<AdminDashboardSummary>>('/admin/dashboard/summary')
  return data
}

export const fetchDashboardTrends = async (days: number): Promise<Result<AdminDashboardTrends>> => {
  const { data } = await http.get<Result<AdminDashboardTrends>>('/admin/dashboard/trends', {
    params: { days },
  })
  return data
}
