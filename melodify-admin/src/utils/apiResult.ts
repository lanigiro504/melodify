import type { Result } from '@/types/api'
import { API_SUCCESS_CODE } from '@/types/api'

export class ApiError extends Error {
  constructor(
    readonly code: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export const unwrapResult = <T>(result: Result<T>): T => {
  if (result.code !== API_SUCCESS_CODE) {
    throw new ApiError(result.code, result.message?.trim() || '请求失败')
  }
  return result.data
}
