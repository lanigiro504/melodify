export const API_SUCCESS_CODE = 200 as const

export interface Result<T> {
  code: number
  message: string
  data: T
}

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
  lastLoginIp?: string | null
  lastLoginTime?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface SysRole {
  id: number
  roleName: string
  roleKey: string
  status?: number | null
  remark?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface LoginResponse {
  token: string
  user: SysUser
}

export interface UserLoginBody {
  username: string
  password: string
}

export interface PageRecords<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}
