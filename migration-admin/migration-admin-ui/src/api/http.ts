import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'
import { clearAuthSessionStorage, getAccessToken } from '@/utils/auth'

interface RequestConfig extends AxiosRequestConfig {
  silent?: boolean
  skipAuth?: boolean
}

const SUCCESS_CODES = new Set([0, 200])
const UNAUTHORIZED_CODES = new Set([401, 403])

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

let isRedirectingToLogin = false

function normalizeBaseUrl(): string {
  const baseUrl = import.meta.env.BASE_URL || '/'
  if (baseUrl === '/') {
    return '/'
  }
  return baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
}

function stripBaseFromPath(pathname: string, baseUrl: string): string {
  if (baseUrl === '/') {
    return pathname
  }
  if (!pathname.startsWith(baseUrl)) {
    return pathname
  }

  const stripped = pathname.slice(baseUrl.length)
  return stripped.startsWith('/') ? stripped : `/${stripped}`
}

function redirectToLogin(): void {
  if (typeof window === 'undefined' || isRedirectingToLogin) {
    return
  }

  if (window.location.pathname.endsWith('/login')) {
    return
  }

  isRedirectingToLogin = true

  const baseUrl = normalizeBaseUrl()
  const currentPath = stripBaseFromPath(window.location.pathname, baseUrl)
  const redirect = encodeURIComponent(`${currentPath}${window.location.search}${window.location.hash}`)
  const loginPath = `${baseUrl}login`
  window.location.replace(`${loginPath}?redirect=${redirect}`)
}

function handleUnauthorized(): void {
  clearAuthSessionStorage()
  redirectToLogin()
}

function getErrorCode(error: unknown): number | undefined {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    if (typeof status === 'number') {
      return status
    }

    const data = error.response?.data as Partial<ApiResult<unknown>> | undefined
    if (typeof data?.code === 'number') {
      return data.code
    }
  }

  const maybeCode = (error as { code?: unknown })?.code
  if (typeof maybeCode === 'number') {
    return maybeCode
  }

  return undefined
}

httpClient.interceptors.request.use((config) => {
  if ((config as RequestConfig).skipAuth) {
    return config
  }

  const token = getAccessToken()
  if (!token) {
    return config
  }

  if (config.headers instanceof AxiosHeaders) {
    if (!config.headers.has('Authorization')) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
    return config
  }

  const headers = (config.headers ?? {}) as Record<string, string>
  if (!headers.Authorization && !headers.authorization) {
    headers.Authorization = `Bearer ${token}`
  }
  config.headers = new AxiosHeaders(headers)
  return config
})

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  return '请求失败，请稍后重试'
}

function parseAxiosError(error: AxiosError): string {
  const data = error.response?.data as Partial<ApiResult<unknown>> | undefined
  if (data?.message) {
    return data.message
  }
  return error.message || '网络请求异常'
}

export async function request<T>(config: RequestConfig): Promise<T> {
  try {
    const response = await httpClient.request<ApiResult<T>>(config)
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (!SUCCESS_CODES.has(body.code)) {
        const bizError = new Error(body.message || '请求失败') as Error & { code?: number }
        bizError.code = body.code
        throw bizError
      }
      return body.data
    }
    return response.data as unknown as T
  } catch (error) {
    const code = getErrorCode(error)
    if (UNAUTHORIZED_CODES.has(code ?? -1)) {
      handleUnauthorized()
    }

    const message = axios.isAxiosError(error) ? parseAxiosError(error) : getErrorMessage(error)
    if (!config.silent) {
      ElMessage.error(message)
    }
    throw error
  }
}

export function get<T>(url: string, config?: RequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'GET', url })
}

export function post<T>(url: string, data?: unknown, config?: RequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'POST', url, data })
}
