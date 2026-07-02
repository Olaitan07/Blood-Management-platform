import { apiClient } from './client'
import type { ApiResponse, HospitalRequest, HospitalResponse } from './types'

export async function listHospitals(all = false): Promise<HospitalResponse[]> {
  const { data } = await apiClient.get<ApiResponse<HospitalResponse[]>>('/hospitals', {
    params: { all },
  })
  return data.data ?? []
}

export async function getHospitalById(id: number): Promise<HospitalResponse> {
  const { data } = await apiClient.get<ApiResponse<HospitalResponse>>(`/hospitals/${id}`)
  return data.data as HospitalResponse
}

export async function createHospital(payload: HospitalRequest): Promise<HospitalResponse> {
  const { data } = await apiClient.post<ApiResponse<HospitalResponse>>('/hospitals', payload)
  return data.data as HospitalResponse
}

export async function deactivateHospital(id: number): Promise<HospitalResponse> {
  const { data } = await apiClient.put<ApiResponse<HospitalResponse>>(`/hospitals/${id}/deactivate`)
  return data.data as HospitalResponse
}
