/**
 * 与用户端 API 的请求/响应字段约定（驼峰命名与 melodify-backend JSON 一致）。
 * `API_SUCCESS_CODE` 等价于服务端 `Result.CODE_SUCCESS`。
 */
export const API_SUCCESS_CODE = 200 as const

/** 与 melodify-backend 统一封装结构 `Result<T>`（code/message/data）一致 */
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

/** 登录成功后 data：token + 用户快照（无 password） */
export interface LoginResponse {
  token: string
  user: SysUser
}

/**
 * 提交 AI 生成：扣积分后异步模拟完成；返回值中的 businessTaskId 用于轮询 `by-task-id`。
 */
export interface MusicGenerateSubmit {
  businessTaskId: string
  initialStatus: number
  costPoints: number
}

/** 对应后端 `MusicGenerateRequestDTO`。 */
export interface MusicGenerateBody {
  modelCode: string
  prompt?: string
  lyrics?: string
  params?: Record<string, unknown>
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
