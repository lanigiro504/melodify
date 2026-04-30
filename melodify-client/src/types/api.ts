/**
 * 与用户端 API 请求体、响应体对齐的 TypeScript 类型（字段驼峰与 JSON 一致）。
 * API_SUCCESS_CODE：与后端 Result.CODE_SUCCESS 一致。
 */
export const API_SUCCESS_CODE = 200 as const

/** 对齐后端 {@link com.melodify.common.result.Result} */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 后端 sys_user 映射；登录接口成功后 password 应为 null */
export interface SysUser {
  id: number
  username: string
  password?: string | null
  nickname?: string | null
  avatar?: string | null
  email?: string | null
  phone?: string | null
  points?: number | null
  roleId?: number | null
  status?: number | null
  isDeleted?: number | null
  lastLoginIp?: string | null
  lastLoginTime?: string | null
  createTime?: string | null
  updateTime?: string | null
}

/** POST /api/client/auth/login 请求体 */
export interface UserLoginBody {
  username: string
  password: string
}

/** POST /api/client/auth/register 请求体 */
export interface UserRegisterBody {
  username: string
  password: string
}
