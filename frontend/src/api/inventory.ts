import { apiClient } from './client'
import type {
  AddInventoryRequest,
  ApiResponse,
  AuditLogResponse,
  InventoryResponse,
  UpdateInventoryRequest,
} from './types'

export async function listInventory(): Promise<InventoryResponse[]> {
  const { data } = await apiClient.get<ApiResponse<InventoryResponse[]>>('/inventory')
  return data.data ?? []
}

export async function addInventory(payload: AddInventoryRequest): Promise<InventoryResponse> {
  const { data } = await apiClient.post<ApiResponse<InventoryResponse>>('/inventory', payload)
  return data.data as InventoryResponse
}

export async function updateInventory(
  id: number,
  payload: UpdateInventoryRequest,
): Promise<InventoryResponse> {
  const { data } = await apiClient.put<ApiResponse<InventoryResponse>>(`/inventory/${id}`, payload)
  return data.data as InventoryResponse
}

// Unpaginated — the backend returns the full history for this inventory line.
export async function getInventoryAuditLog(id: number): Promise<AuditLogResponse[]> {
  const { data } = await apiClient.get<ApiResponse<AuditLogResponse[]>>(`/inventory/${id}/audit`)
  return data.data ?? []
}
