import { get, post } from '@/api/http'
import type { AuthUserInfo, LoginResponse } from '@/types'

const AUTH_BASE_PATH = '/api/v1/auth'

export interface LoginPayload {
  username: string
  password: string
}

export const login = (data: LoginPayload): Promise<LoginResponse> =>
  post<LoginResponse>(`${AUTH_BASE_PATH}/login`, data, { skipAuth: true })

export const queryCurrentUser = (): Promise<AuthUserInfo> =>
  get<AuthUserInfo>(`${AUTH_BASE_PATH}/query_current_user`)

export const logout = (): Promise<void> => post<void>(`${AUTH_BASE_PATH}/logout`)
