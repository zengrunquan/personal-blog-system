import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from './types'
import { AppError } from './types'

let csrfToken = ''
const appContext = import.meta.env.DEV ? '' : import.meta.env.BASE_URL.replace(/\/$/, '')

export const http = axios.create({
  baseURL: `${appContext}/api`,
  withCredentials: true,
  timeout: 15_000,
  headers: { Accept: 'application/json' },
})

export function setCsrfToken(token: string): void {
  csrfToken = token
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const method = config.method?.toUpperCase()
  if (csrfToken && method && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    config.headers.set('X-CSRF-Token', csrfToken)
  }
  if (import.meta.env.DEV) {
    console.log(`[DEBUG] ${new Date().toISOString()} [http] ${method ?? 'GET'} ${config.url ?? ''}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const payload = error.response?.data
    if (payload && !payload.success) {
      if (error.response?.status === 401) window.dispatchEvent(new CustomEvent('session-expired'))
      return Promise.reject(
        new AppError(
          payload.error.message,
          payload.error.code,
          payload.error.fieldErrors,
          error.response?.status,
        ),
      )
    }
    const message =
      error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '无法连接服务器，请检查网络'
    return Promise.reject(
      new AppError(message, error.code ?? 'NETWORK_ERROR', undefined, error.status),
    )
  },
)

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const response = await request
    if (!response.data.success) {
      throw new AppError(
        response.data.error.message,
        response.data.error.code,
        response.data.error.fieldErrors,
      )
    }
    return response.data.data
  } catch (error) {
    if (import.meta.env.DEV) {
      console.error(`[DEBUG] ${new Date().toISOString()} [unwrap] 请求失败`, error)
    }
    throw error instanceof AppError ? error : new AppError('请求处理失败，请稍后重试')
  }
}
