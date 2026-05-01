import type { PageRecords, Result, SysUser } from '@/types/api'
import { http } from './http'

export const pageUsers = async (current: number, size: number): Promise<Result<PageRecords<SysUser>>> => {
  const { data } = await http.get<Result<PageRecords<SysUser>>>('/admin/users/page', {
    params: { current, size },
  })
  return data
}

export const createUser = async (body: Partial<SysUser>): Promise<Result<boolean>> => {
  const { data } = await http.post<Result<boolean>>('/admin/users', body)
  return data
}

export const updateUser = async (id: number, body: Partial<SysUser>): Promise<Result<boolean>> => {
  const { data } = await http.put<Result<boolean>>(`/admin/users/${id}`, body)
  return data
}

export const deleteUser = async (id: number): Promise<Result<boolean>> => {
  const { data } = await http.delete<Result<boolean>>(`/admin/users/${id}`)
  return data
}
