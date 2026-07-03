import { apiClient } from './client'
import type {
  ApiResponse,
  CompleteTransferRequest,
  CreateTransferRequest,
  RejectTransferRequest,
  TransferResponse,
} from './types'

export async function createTransfer(payload: CreateTransferRequest): Promise<TransferResponse> {
  const { data } = await apiClient.post<ApiResponse<TransferResponse>>('/transfers', payload)
  return data.data as TransferResponse
}

// Hospital-scoped, not personal — returns every request made by anyone at the
// caller's own hospital (as either requester or, separately, via /pending as
// source). See CLAUDE.md-adjacent notes: no requestedByUserId is exposed.
export async function getMyRequests(): Promise<TransferResponse[]> {
  const { data } = await apiClient.get<ApiResponse<TransferResponse[]>>('/transfers/my-requests')
  return data.data ?? []
}

export async function getPendingRequests(): Promise<TransferResponse[]> {
  const { data } = await apiClient.get<ApiResponse<TransferResponse[]>>('/transfers/pending')
  return data.data ?? []
}

export async function approveTransfer(id: number): Promise<TransferResponse> {
  const { data } = await apiClient.put<ApiResponse<TransferResponse>>(`/transfers/${id}/approve`)
  return data.data as TransferResponse
}

export async function rejectTransfer(id: number, payload: RejectTransferRequest): Promise<TransferResponse> {
  const { data } = await apiClient.put<ApiResponse<TransferResponse>>(`/transfers/${id}/reject`, payload)
  return data.data as TransferResponse
}

export async function completeTransfer(
  id: number,
  payload: CompleteTransferRequest,
): Promise<TransferResponse> {
  const { data } = await apiClient.put<ApiResponse<TransferResponse>>(`/transfers/${id}/complete`, payload)
  return data.data as TransferResponse
}

export async function cancelTransfer(id: number): Promise<TransferResponse> {
  const { data } = await apiClient.put<ApiResponse<TransferResponse>>(`/transfers/${id}/cancel`)
  return data.data as TransferResponse
}
