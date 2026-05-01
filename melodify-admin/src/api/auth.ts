import type { LoginResponse, Result, UserLoginBody } from '@/types/api'
import { http } from './http'

export const login = async (payload: UserLoginBody): Promise<Result<LoginResponse>> => {
  const { data } = await http.post<Result<LoginResponse>>('/client/auth/login', payload)
  return data
}
