import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import * as adminRolesApi from '@/api/adminRoles'
import type { SysUser } from '@/types/api'
import { ApiError, unwrapResult } from '@/utils/apiResult'
import {
  ADMIN_USER_SNAPSHOT_KEY,
  clearAdminCredentialStorage,
  getStoredToken,
  setStoredToken,
} from '@/utils/sessionCredentials'

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const currentUser = ref<SysUser | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed(() => {
    try {
      return currentUser.value != null && !!getStoredToken()
    } catch {
      return false
    }
  })

  const displayName = computed(() => {
    const u = currentUser.value
    if (!u) return ''
    return (u.nickname?.trim() || u.username?.trim() || '') as string
  })

  const initialize = async (): Promise<void> => {
    if (initialized.value) return
    try {
      const raw = sessionStorage.getItem(ADMIN_USER_SNAPSHOT_KEY)
      if (raw) {
        currentUser.value = JSON.parse(raw) as SysUser
      }
      if (!getStoredToken()) {
        currentUser.value = null
        sessionStorage.removeItem(ADMIN_USER_SNAPSHOT_KEY)
      }
    } catch {
      clearAdminCredentialStorage()
      currentUser.value = null
    } finally {
      initialized.value = true
    }
  }

  const persistSession = (user: SysUser, token: string): void => {
    currentUser.value = user
    sessionStorage.setItem(ADMIN_USER_SNAPSHOT_KEY, JSON.stringify(user))
    setStoredToken(token)
  }

  const logout = (): void => {
    currentUser.value = null
    clearAdminCredentialStorage()
  }

  const login = async (username: string, password: string): Promise<void> => {
    const body = unwrapResult(await authApi.login({ username, password }))
    persistSession(body.user, body.token)
    try {
      unwrapResult(await adminRolesApi.pageRoles(1, 1))
    } catch (e) {
      logout()
      if (e instanceof ApiError && e.code === 403) {
        throw new ApiError(403, '当前账号无权访问控制台')
      }
      throw e
    }
  }

  return {
    currentUser,
    initialized,
    isAuthenticated,
    displayName,
    initialize,
    login,
    logout,
  }
})
