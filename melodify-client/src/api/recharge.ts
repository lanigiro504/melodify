import type { PointProduct, RechargeOrder, Result, SimulatedPayNotify } from '@/types/api'
import { http } from './http'

export const listPointProducts = async (): Promise<Result<PointProduct[]>> => {
  const { data } = await http.get<Result<PointProduct[]>>('/client/recharge/products')
  return data
}

export const createRechargeOrder = async (productId: number): Promise<Result<RechargeOrder>> => {
  const { data } = await http.post<Result<RechargeOrder>>('/client/recharge/orders', { productId })
  return data
}

export const buildSimulatedNotify = async (
  orderNo: string,
): Promise<Result<SimulatedPayNotify>> => {
  const encoded = encodeURIComponent(orderNo.trim())
  const { data } = await http.get<Result<SimulatedPayNotify>>(
    `/client/recharge/orders/${encoded}/simulated-notify`,
  )
  return data
}

export const notifySimulatedPay = async (
  payload: SimulatedPayNotify,
): Promise<Result<RechargeOrder>> => {
  const { data } = await http.post<Result<RechargeOrder>>('/payments/simulated/notify', payload)
  return data
}
