import axios, { AxiosError } from 'axios'
import type { ApiResponse } from './types'

// Normalized shape every screen can rely on, regardless of whether the
// backend responded with the app's ApiResponse envelope (most errors) or
// Spring Security's default body (403s thrown by @PreAuthorize, which
// AuthGlobalExceptionHandler does not intercept).
export class ApiError extends Error {
  status: number
  fieldErrors?: Record<string, string>

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

let authToken: string | null = null

// Token lives in memory only (per spec) — never localStorage.
export function setAuthToken(token: string | null) {
  authToken = token
}

apiClient.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

let onUnauthorized: (() => void) | null = null
export function registerUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown> | undefined>) => {
    const status = error.response?.status ?? 0
    const body = error.response?.data

    // A previously-valid-looking token that now gets 401/403 means the
    // session was invalidated server-side (JwtAuthenticationFilter reloads
    // the user on every request) — treat as "log out now", not transient.
    if ((status === 401 || status === 403) && authToken) {
      onUnauthorized?.()
    }

    if (body && typeof body === 'object' && 'message' in body) {
      throw new ApiError(status, body.message ?? 'Request failed', body.errors)
    }

    if (status === 0) {
      throw new ApiError(0, 'Unable to reach the server. Check your connection and try again.')
    }

    // Unenveloped errors (e.g. @PreAuthorize 403s, Spring's default /error body).
    throw new ApiError(status, defaultMessageForStatus(status))
  },
)

function defaultMessageForStatus(status: number): string {
  switch (status) {
    case 403:
      return 'You do not have permission to perform this action.'
    case 404:
      return 'Not found.'
    case 500:
      return 'An unexpected error occurred.'
    default:
      return 'Something went wrong.'
  }
}
