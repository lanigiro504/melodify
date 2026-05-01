import axios from 'axios'
import { clearAdminCredentialStorage, getStoredToken } from '@/utils/sessionCredentials'
import { SilentSessionRedirect } from '@/utils/httpSilent'

export const getAuthorizationHeader = (): Record<string, string> => {
  const t = getStoredToken()
  return t ? { Authorization: `Bearer ${t}` } : {}
}

const baseURL =
  typeof import.meta.env.VITE_API_BASE_URL === 'string' ? import.meta.env.VITE_API_BASE_URL : ''

export const http = axios.create({
  baseURL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  Object.assign(config.headers, getAuthorizationHeader())
  return config
})

http.interceptors.response.use(
  (res) => {
    const d = res.data
    const method = String(res.config.method || '').toLowerCase()
    const url = String(res.config.url || '')
    const isLoginFailureContext = method === 'post' && url.includes('/client/auth/login')
    if (
      !isLoginFailureContext &&
      d &&
      typeof d === 'object' &&
      'code' in d &&
      typeof (d as { code: unknown }).code === 'number' &&
      (d as { code: number }).code === 401
    ) {
      clearAdminCredentialStorage()
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
