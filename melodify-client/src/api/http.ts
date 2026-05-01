import axios from 'axios'
import { getStoredToken } from '@/utils/sessionCredentials'
import { SilentSessionRedirect } from '@/utils/httpSilent'

/**
 * 将 sessionStorage 中的 JWT 附加为 Bearer，与后端 JwtAuthenticationFilter 对齐。
 */
export const getAuthorizationHeader = (): Record<string, string> => {
  const t = getStoredToken()
  return t ? { Authorization: `Bearer ${t}` } : {}
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

http.interceptors.response.use(
  (res) => {
    const d = res.data
    const method = String(res.config.method || '').toLowerCase()
    const url = String(res.config.url || '')
    const isAuthLoginPost = method === 'post' && url.includes('/client/auth/login')
    if (
      !isAuthLoginPost &&
      d &&
      typeof d === 'object' &&
      'code' in d &&
      typeof (d as { code: unknown }).code === 'number' &&
      (d as { code: number }).code === 401
    ) {
      void import('@/stores/auth').then(({ useAuthStore }) => {
        useAuthStore().logout()
      })
      void import('@/router').then(({ default: r }) => {
        if (r.currentRoute.value.path !== '/login') {
          void r.replace({ path: '/login', query: { redirect: r.currentRoute.value.fullPath } })
        }
      })
      return Promise.reject(new SilentSessionRedirect())
    }
    return res
  },
  (err) => Promise.reject(err),
)
