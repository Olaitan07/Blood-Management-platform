import { apiClient } from './client'
import type { ApiResponse, DonationResponse, DonorRequest, DonorResponse, PageResponse } from './types'

export async function getMyDonorProfile(): Promise<DonorResponse> {
  const { data } = await apiClient.get<ApiResponse<DonorResponse>>('/donors/me')
  return data.data as DonorResponse
}

export async function registerDonor(payload: DonorRequest): Promise<DonorResponse> {
  const { data } = await apiClient.post<ApiResponse<DonorResponse>>('/donors', payload)
  return data.data as DonorResponse
}

// page is 0-based, matching Spring Data's Pageable convention.
export async function getDonationHistory(
  donorId: number,
  page: number,
  size: number,
): Promise<PageResponse<DonationResponse>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<DonationResponse>>>(
    `/donors/${donorId}/donations`,
    { params: { page, size } },
  )
  return data.data as PageResponse<DonationResponse>
}
