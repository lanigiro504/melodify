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

export interface MusicTask {
  id: number
  taskId: string
  vendorTaskId?: string | null
  userId: number
  modelCode: string
  prompt?: string | null
  params?: Record<string, unknown> | null
  status?: number | null
  errorCode?: string | null
  errorMessage?: string | null
  costPoints?: number | null
  startedAt?: string | null
  finishedAt?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface MusicAsset {
  id: number
  assetId: string
  taskId: number
  userId: number
  title?: string | null
  fileUrl?: string | null
  coverUrl?: string | null
  durationSec?: number | null
  format?: string | null
  bitrateKbps?: number | null
  isPublic?: number | null
  licenseType?: string | null
  status?: number | null
  createTime?: string | null
  updateTime?: string | null
}

export interface PointLog {
  id: number
  logId: string
  userId: number
  changeType?: number | null
  amount?: number | null
  balance?: number | null
  bizType?: string | null
  bizId?: string | null
  remark?: string | null
  createTime?: string | null
}

export interface RechargeOrder {
  id: number
  orderNo: string
  userId: number
  productId: number
  productName?: string | null
  points?: number | null
  amountCent?: number | null
  status?: number | null
  paidAt?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface PointProduct {
  id: number
  productCode: string
  productName: string
  points?: number | null
  priceCent?: number | null
  status?: number | null
  sortOrder?: number | null
  createTime?: string | null
  updateTime?: string | null
}
