import type { LoginResponse, Result, UserLoginBody, UserRegisterBody } from '@/types/api'
import { http } from './http'

/**
 * 用户登录：POST /api/client/auth/login（baseURL 已含 /api 前缀时路径为 /client/auth/login）。
 */
export const login = async (payload: UserLoginBody): Promise<Result<LoginResponse>> => {
  const { data } = await http.post<Result<LoginResponse>>('/client/auth/login', payload)
  return data
}

/**
 * 用户注册：POST /api/client/auth/register。
 */
export const register = async (payload: UserRegisterBody): Promise<Result<null>> => {
  const { data } = await http.post<Result<null>>('/client/auth/register', payload)
  return data
}
