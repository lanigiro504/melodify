import type { Result, SysUser, UserProfileBody } from '@/types/api'
import { getAuthorizationHeader, http } from './http'

export const getMe = async (): Promise<Result<SysUser>> => {
  const { data } = await http.get<Result<SysUser>>('/client/users/me')
  return data
}

export const updateMe = async (payload: UserProfileBody): Promise<Result<null>> => {
  const { data } = await http.put<Result<null>>('/client/users/me', payload)
  return data
}

export const uploadMyAvatar = async (file: File): Promise<Result<string>> => {
  const body = new FormData()
  body.append('file', file)
  const { data } = await http.post<Result<string>>('/client/users/me/avatar', body, {
    headers: {
      ...getAuthorizationHeader(),
    },
    transformRequest: [
      (payload, headers) => {
        if (payload instanceof FormData) {
          delete (headers as Record<string, unknown>)['Content-Type']
        }
        return payload
      },
    ],
  })
  return data
}
