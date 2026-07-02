import { apiClient } from './client'
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, UserResponse } from './types'

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', payload)
  return data.data as AuthResponse
}

export async function register(payload: RegisterRequest): Promise<UserResponse> {
  const { data } = await apiClient.post<ApiResponse<UserResponse>>('/auth/register', payload)
  return data.data as UserResponse
}

export async function getCurrentUser(): Promise<UserResponse> {
  const { data } = await apiClient.get<ApiResponse<UserResponse>>('/auth/me')
  return data.data as UserResponse
}
