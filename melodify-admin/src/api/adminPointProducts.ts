import type { PageRecords, PointProduct, Result } from '@/types/api'
import { http } from './http'

export const pagePointProductsAdmin = async (
  current: number,
  size: number,
  options: { status?: number | undefined } = {},
): Promise<Result<PageRecords<PointProduct>>> => {
  const params: Record<string, number> = { current, size }
  if (typeof options.status === 'number') params.status = options.status
  const { data } = await http.get<Result<PageRecords<PointProduct>>>('/admin/point-products/page', { params })
  return data
}

export const updatePointProductAdmin = async (
  id: number,
  body: Partial<PointProduct>,
): Promise<Result<boolean>> => {
  const { data } = await http.put<Result<boolean>>(`/admin/point-products/${id}`, body)
  return data
}
