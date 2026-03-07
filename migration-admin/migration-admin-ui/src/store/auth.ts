import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login, logout, queryCurrentUser, type LoginPayload } from '@/api/auth'
import type { AuthUserInfo } from '@/types'
import {
  clearAuthSessionStorage,
  getAccessToken,
  getStoredUserInfo,
  setAccessToken,
  setStoredUserInfo,
} from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(getAccessToken())
  const userInfo = ref<AuthUserInfo | null>(getStoredUserInfo())
  // 页面刷新后强制向服务端校验一次登录态，即使本地已有 user_info。
  const profileChecked = ref(false)

  const isAuthenticated = computed(() => Boolean(accessToken.value))
  const displayName = computed(() => userInfo.value?.display_name || userInfo.value?.username || '管理员')

  async function signIn(payload: LoginPayload): Promise<void> {
    const result = await login(payload)
    accessToken.value = result.access_token
    userInfo.value = result.user_info
    profileChecked.value = true
    setAccessToken(result.access_token)
    setStoredUserInfo(result.user_info)
  }

  async function refreshCurrentUser(): Promise<void> {
    const result = await queryCurrentUser()
    userInfo.value = result
    profileChecked.value = true
    setStoredUserInfo(result)
  }

  async function signOut(): Promise<boolean> {
    let success = true
    try {
      await logout()
    } catch {
      success = false
    } finally {
      clearSession()
    }
    return success
  }

  function clearSession(): void {
    accessToken.value = ''
    userInfo.value = null
    profileChecked.value = false
    clearAuthSessionStorage()
  }

  return {
    accessToken,
    userInfo,
    profileChecked,
    isAuthenticated,
    displayName,
    signIn,
    refreshCurrentUser,
    signOut,
    clearSession,
  }
})
