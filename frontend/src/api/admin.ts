import { apiClient } from './client'
import type { ApiResponse, Role, UserResponse } from './types'

export async function listAllUsers(): Promise<UserResponse[]> {
  const { data } = await apiClient.get<ApiResponse<UserResponse[]>>('/admin/users')
  return data.data ?? []
}

export async function listPendingUsers(): Promise<UserResponse[]> {
  const { data } = await apiClient.get<ApiResponse<UserResponse[]>>('/admin/users/pending')
  return data.data ?? []
}

export async function approveUser(id: number): Promise<UserResponse> {
  const { data } = await apiClient.put<ApiResponse<UserResponse>>(`/admin/users/${id}/approve`)
  return data.data as UserResponse
}

export async function deactivateUser(id: number): Promise<UserResponse> {
  const { data } = await apiClient.put<ApiResponse<UserResponse>>(`/admin/users/${id}/deactivate`)
  return data.data as UserResponse
}

export async function changeUserRole(id: number, role: Role): Promise<UserResponse> {
  const { data } = await apiClient.put<ApiResponse<UserResponse>>(`/admin/users/${id}/role`, null, {
    params: { role },
  })
  return data.data as UserResponse
}
