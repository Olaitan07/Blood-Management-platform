import { apiClient } from './client'
import type { ReportResult, ReportType } from './types'

// from/to are LocalDate ISO strings ("YYYY-MM-DD"), required by the backend —
// there's no defaultable/optional path here, unlike most other date filters
// in this app.
export async function generateReport(type: ReportType, from: string, to: string): Promise<ReportResult> {
  const { data } = await apiClient.get<ReportResult>(`/reports/${type}`, { params: { from, to } })
  return data
}

// The backend returns raw CSV text (Content-Type: text/csv), not a JSON
// envelope — axios needs responseType 'text' explicitly since the default
// 'json' parser would otherwise try (and fail) to JSON.parse it.
export async function exportReportCsv(type: ReportType, from: string, to: string): Promise<string> {
  const { data } = await apiClient.get<string>(`/reports/${type}/export/csv`, {
    params: { from, to },
    responseType: 'text',
  })
  return data
}

export function reportCsvFilename(type: ReportType, from: string, to: string): string {
  return `${type.toLowerCase()}_${from}_${to}.csv`
}
