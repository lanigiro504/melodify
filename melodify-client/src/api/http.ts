import axios from 'axios'

/**
 * 凭证注入点（后续任选其一即可对接后端）：
 * - JWT：在此返回 { Authorization: `Bearer ${token}` }
 * - HttpOnly Cookie：在 axios.create 上设置 withCredentials: true，并保持与后端同源或由网关注入 Cookie
 */
export const getAuthorizationHeader = (): Record<string, string> => {
  return {}
}

/** 开发环境通常为 /api，经 Vite 代理到后端；生产环境由构建变量注入网关前缀 */
const baseURL =
  typeof import.meta.env.VITE_API_BASE_URL === 'string'
    ? import.meta.env.VITE_API_BASE_URL
    : ''

export const http = axios.create({
  baseURL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  // 每个请求合并凭证头（JWT 等）；纯 Cookie 方案则主要靠 withCredentials
  const auth = getAuthorizationHeader()
  Object.assign(config.headers, auth)
  return config
})
