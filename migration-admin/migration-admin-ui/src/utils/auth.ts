import type { AuthUserInfo } from '@/types'

const ACCESS_TOKEN_KEY = 'migration_admin_access_token'
const USER_INFO_KEY = 'migration_admin_user_info'

function safeParseUserInfo(raw: string | null): AuthUserInfo | null {
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AuthUserInfo
  } catch {
    return null
  }
}

export function getAccessToken(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.sessionStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
}

export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function clearAccessToken(): void {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function getStoredUserInfo(): AuthUserInfo | null {
  if (typeof window === 'undefined') {
    return null
  }
  return safeParseUserInfo(window.sessionStorage.getItem(USER_INFO_KEY))
}

export function setStoredUserInfo(userInfo: AuthUserInfo): void {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

export function clearStoredUserInfo(): void {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.removeItem(USER_INFO_KEY)
}

export function clearAuthSessionStorage(): void {
  clearAccessToken()
  clearStoredUserInfo()
}
