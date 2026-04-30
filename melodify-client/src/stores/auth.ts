import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { SysUser, UserLoginBody, UserRegisterBody } from '@/types/api'
import { unwrapResult } from '@/utils/apiResult'

/** sessionStorage 中缓存当前用户 JSON 的键名（不含密码字段） */
const STORAGE_KEY = 'melodify:client:user'

/**
 * 用户端认证状态：与后端 /api/client/auth 对齐。
 * 持久化仅存用户信息；后续 JWT/Cookie 在 api/http.ts 扩展。
 */
export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<SysUser | null>(null)
  /** 是否已从 storage 完成首次 hydrate，供路由守卫使用 */
  const initialized = ref(false)

  const isAuthenticated = computed(() => currentUser.value != null)

  /** 顶栏与首页问候展示的优先级：nickname → username */
  const displayName = computed(() => {
    const u = currentUser.value
    if (!u) return ''
    const nick = u.nickname?.trim()
    const name = u.username?.trim()
    return nick || name || ''
  })

  /**
   * 应用启动或首次导航时调用：尝试从 sessionStorage 恢复会话。
   * 解析失败则清空本地缓存，避免脏数据阻塞路由。
   */
  const initialize = async () => {
    if (initialized.value) return
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY)
      if (raw) {
        currentUser.value = JSON.parse(raw) as SysUser
      }
    } catch {
      sessionStorage.removeItem(STORAGE_KEY)
      currentUser.value = null
    } finally {
      initialized.value = true
    }
  }

  /** 写入内存并持久化到 sessionStorage（刷新后仍可展示登录态） */
  const persistUser = (user: SysUser) => {
    currentUser.value = user
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  }

  /** 调用登录接口并更新本地用户快照 */
  const login = async (payload: UserLoginBody) => {
    const user = unwrapResult(await authApi.login(payload))
    persistUser(user)
  }

  /** 仅完成注册请求；成功后由页面跳转登录，此处不写用户信息 */
  const register = async (payload: UserRegisterBody) => {
    unwrapResult(await authApi.register(payload))
  }

  /** 清除内存与会话存储中的用户信息 */
  const logout = () => {
    currentUser.value = null
    sessionStorage.removeItem(STORAGE_KEY)
  }

  return {
    currentUser,
    initialized,
    isAuthenticated,
    displayName,
    initialize,
    login,
    register,
    logout,
  }
})
