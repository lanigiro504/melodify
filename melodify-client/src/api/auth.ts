import type { Result, SysUser, UserLoginBody, UserRegisterBody } from '@/types/api'
import { http } from './http'

/**
 * 用户登录：POST /api/client/auth/login（baseURL 已含 /api 前缀时路径为 /client/auth/login）。
 * @returns 原始 Result，由上层 unwrapResult 或自行判断 code。
 */
export const login = async (payload: UserLoginBody): Promise<Result<SysUser>> => {
  const { data } = await http.post<Result<SysUser>>('/client/auth/login', payload)
  return data
}

/**
 * 用户注册：POST /api/client/auth/register。
 */
export const register = async (payload: UserRegisterBody): Promise<Result<null>> => {
  const { data } = await http.post<Result<null>>('/client/auth/register', payload)
  return data
}
