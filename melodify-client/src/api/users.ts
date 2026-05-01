import type { Result, SysUser, UserProfileBody } from '@/types/api'
import { http } from './http'

export const getMe = async (): Promise<Result<SysUser>> => {
  const { data } = await http.get<Result<SysUser>>('/client/users/me')
  return data
}

export const updateMe = async (payload: UserProfileBody): Promise<Result<null>> => {
  const { data } = await http.put<Result<null>>('/client/users/me', payload)
  return data
}
