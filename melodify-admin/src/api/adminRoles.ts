import type { PageRecords, Result, SysRole } from '@/types/api'
import { http } from './http'

/** 可用于登录后校验是否具备 ROLE_ADMIN（能调通则说明 rk=admin）。 */
export const pageRoles = async (current: number, size: number): Promise<Result<PageRecords<SysRole>>> => {
  const { data } = await http.get<Result<PageRecords<SysRole>>>('/admin/roles/page', {
    params: { current, size },
  })
  return data
}

export const createRole = async (body: Partial<SysRole>): Promise<Result<boolean>> => {
  const { data } = await http.post<Result<boolean>>('/admin/roles', body)
  return data
}

export const updateRole = async (id: number, body: Partial<SysRole>): Promise<Result<boolean>> => {
  const { data } = await http.put<Result<boolean>>(`/admin/roles/${id}`, body)
  return data
}

export const deleteRole = async (id: number): Promise<Result<boolean>> => {
  const { data } = await http.delete<Result<boolean>>(`/admin/roles/${id}`)
  return data
}
