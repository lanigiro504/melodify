import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ApiError } from '@/utils/apiResult'

export const showSubmitError = (error: unknown, fallbackMessage: string): void => {
  if (error instanceof ApiError) {
    ElMessage.error(error.message)
    return
  }
  if (axios.isAxiosError(error)) {
    const body = error.response?.data
    const fromBody =
      typeof body === 'object' &&
      body !== null &&
      'message' in body &&
      typeof (body as { message: unknown }).message === 'string'
        ? (body as { message: string }).message.trim()
        : ''
    ElMessage.error(fromBody || error.message || '网络异常')
    return
  }
  ElMessage.error(fallbackMessage)
}
