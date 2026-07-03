import { apiClient } from './client'
import type { AuditEventType, AuditRecordResponse, AuditTargetType, PageResponse } from './types'

export interface AuditFilters {
  eventType?: AuditEventType
  actor?: string
  targetType?: AuditTargetType
  // ISO-8601 instants, e.g. "2026-01-01T00:00:00Z" — the backend uses
  // Instant.parse with no fallback and no clean error on a bad format, so the
  // caller must always send fully-formed instants, never a bare date string.
  from?: string
  to?: string
}

// page is 0-based, matching the backend's convention. size is silently
// clamped to 100 server-side if exceeded — no error, just a smaller page.
export async function getAuditLog(
  filters: AuditFilters,
  page: number,
  size: number,
): Promise<PageResponse<AuditRecordResponse>> {
  const { data } = await apiClient.get<PageResponse<AuditRecordResponse>>('/audit', {
    params: { ...filters, page, size },
  })
  return data
}
