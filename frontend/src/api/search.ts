import { apiClient } from './client'
import type { ApiResponse, BloodGroup, BloodSearchResponse } from './types'

// page is 0-based, matching the backend's convention.
export async function searchBlood(
  bloodGroup: BloodGroup,
  page: number,
  size: number,
): Promise<BloodSearchResponse> {
  const { data } = await apiClient.get<ApiResponse<BloodSearchResponse>>('/search/blood', {
    params: { bloodGroup, page, size },
  })
  return data.data as BloodSearchResponse
}
