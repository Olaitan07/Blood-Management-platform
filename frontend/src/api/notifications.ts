import { apiClient } from './client'
import type { ApiResponse, NotificationResponse } from './types'

export async function getAllNotifications(): Promise<NotificationResponse[]> {
  const { data } = await apiClient.get<ApiResponse<NotificationResponse[]>>('/notifications')
  return data.data ?? []
}

export async function getNotificationsForHospital(hospitalId: number): Promise<NotificationResponse[]> {
  const { data } = await apiClient.get<ApiResponse<NotificationResponse[]>>(
    `/notifications/hospital/${hospitalId}`,
  )
  return data.data ?? []
}

export async function getNotificationsForDonor(donorId: number): Promise<NotificationResponse[]> {
  const { data } = await apiClient.get<ApiResponse<NotificationResponse[]>>(`/notifications/donor/${donorId}`)
  return data.data ?? []
}

export async function markNotificationRead(id: number): Promise<NotificationResponse> {
  const { data } = await apiClient.put<ApiResponse<NotificationResponse>>(`/notifications/${id}/read`)
  return data.data as NotificationResponse
}

export async function markAllReadForHospital(hospitalId: number): Promise<void> {
  await apiClient.put(`/notifications/hospital/${hospitalId}/read-all`)
}

export async function markAllReadForDonor(donorId: number): Promise<void> {
  await apiClient.put(`/notifications/donor/${donorId}/read-all`)
}
