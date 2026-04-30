import { API_SUCCESS_CODE, type Result } from '@/types/api'

/**
 * 业务错误：对应后端 Result 中 code !== 成功码 的场景，携带对外可展示的 message。
 */
export class ApiError extends Error {
  constructor(
    readonly code: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/**
 * 解析后端统一 Result：成功则返回 data；失败则抛出 ApiError。
 */
export const unwrapResult = <T>(result: Result<T>): T => {
  if (result.code !== API_SUCCESS_CODE) {
    throw new ApiError(result.code, result.message?.trim() || '请求失败')
  }
  return result.data
}
