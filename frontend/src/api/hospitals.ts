import { apiClient } from './client'
import type { ApiResponse, HospitalResponse } from './types'

export async function listHospitals(all = false): Promise<HospitalResponse[]> {
  const { data } = await apiClient.get<ApiResponse<HospitalResponse[]>>('/hospitals', {
    params: { all },
  })
  return data.data ?? []
}
