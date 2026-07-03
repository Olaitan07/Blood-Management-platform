import { useEffect, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { exportReportCsv, generateReport, reportCsvFilename } from '@/api/reports'
import { ApiError } from '@/api/client'
import { Button } from '@/components/Button'
import { useToast } from '@/components/Toast'
import { formatDate } from '@/lib/dateFormat'
import { DonorsResult, ExpiryWasteResult, StockLevelsResult, TransfersResult } from './reportRenderers'
import type { ReportType } from '@/api/types'

const REPORT_TYPES: { value: ReportType; label: string; icon: string }[] = [
  { value: 'STOCK_LEVELS', label: 'Stock levels', icon: '💧' },
  { value: 'TRANSFERS', label: 'Transfers', icon: '⇄' },
  { value: 'DONORS', label: 'Donors', icon: '👥' },
  { value: 'EXPIRY_WASTE', label: 'Expiry waste', icon: '⏱' },
]

const MAX_RANGE_DAYS = 365

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysBetween(from: string, to: string): number {
  return Math.round((new Date(to).getTime() - new Date(from).getTime()) / 86_400_000)
}

export function ReportsPage() {
  const { show } = useToast()
  const [type, setType] = useState<ReportType>('STOCK_LEVELS')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [isExporting, setIsExporting] = useState(false)

  // Verified against ReportServiceImpl: StockLevelReportGenerator always
  // reads the current live snapshot and ignores from/to entirely — there is
  // no historical stock-level tracking in the schema to query against. Rather
  // than let the date pickers imply they do something here, they're disabled
  // for this type and a fixed same-day range is sent (any valid pair works).
  const dateRangeApplies = type !== 'STOCK_LEVELS'

  const reportMutation = useMutation({
    mutationFn: () => {
      const from = dateRangeApplies ? fromDate : todayIso()
      const to = dateRangeApplies ? toDate : todayIso()
      return generateReport(type, from, to)
    },
  })

  useEffect(() => {
    // Switching type clears any previous result — a stale result under a new
    // type label would look like data for the wrong report.
    reportMutation.reset()
  }, [type]) // eslint-disable-line react-hooks/exhaustive-deps

  const days = dateRangeApplies && fromDate && toDate ? daysBetween(fromDate, toDate) : null
  const rangeBackwards = days !== null && days < 0
  const rangeTooLong = days !== null && days > MAX_RANGE_DAYS
  const canGenerate =
    !reportMutation.isPending && (!dateRangeApplies || (fromDate && toDate && !rangeBackwards && !rangeTooLong))

  async function handleExport() {
    if (!reportMutation.data) return
    setIsExporting(true)
    try {
      const from = dateRangeApplies ? fromDate : todayIso()
      const to = dateRangeApplies ? toDate : todayIso()
      const csv = await exportReportCsv(type, from, to)
      const filename = reportCsvFilename(type, from, to)
      const blob = new Blob([csv], { type: 'text/csv' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
      show(`${filename} downloaded`, 'success')
    } catch {
      show("Couldn't export CSV. Try again.", 'error')
    } finally {
      setIsExporting(false)
    }
  }

  const result = reportMutation.data
  const reportLabel = REPORT_TYPES.find((t) => t.value === type)?.label ?? type

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-gray-100">Reports</h1>

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-medium text-gray-400">Report type</h2>
        <div role="radiogroup" aria-label="Report type" className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {REPORT_TYPES.map((t) => (
            <button
              key={t.value}
              type="button"
              role="radio"
              aria-checked={type === t.value}
              disabled={reportMutation.isPending}
              onClick={() => setType(t.value)}
              className={`flex flex-col items-center gap-2 rounded-lg border px-4 py-5 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
                type === t.value
                  ? 'border-brand-500 bg-brand-950/30 text-gray-100'
                  : 'border-gray-800 text-gray-300 hover:border-gray-600'
              }`}
            >
              <span aria-hidden="true" className="text-xl">
                {t.icon}
              </span>
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-medium text-gray-400">Date range</h2>
        {dateRangeApplies ? (
          <>
            <div className="flex flex-wrap items-center gap-3">
              <input
                type="date"
                aria-label="From date"
                value={fromDate}
                disabled={reportMutation.isPending}
                onChange={(e) => setFromDate(e.target.value)}
                className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100"
              />
              <span className="text-gray-500">–</span>
              <input
                type="date"
                aria-label="To date"
                value={toDate}
                disabled={reportMutation.isPending}
                onChange={(e) => setToDate(e.target.value)}
                className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100"
              />
              {days !== null && (
                <span className={`text-sm ${rangeBackwards || rangeTooLong ? 'text-red-400' : 'text-gray-500'}`}>
                  {rangeBackwards ? '—' : `${days} days selected`} · max {MAX_RANGE_DAYS}
                </span>
              )}
            </div>
            {rangeBackwards && (
              <p role="alert" className="mt-2 text-sm text-red-400">
                Start date must be before end date
              </p>
            )}
            {!rangeBackwards && rangeTooLong && (
              <p role="alert" className="mt-2 text-sm text-red-400">
                That range is too long. Reports are limited to 365 days — try narrowing it.
              </p>
            )}
          </>
        ) : (
          <p className="text-sm text-gray-500">
            Stock levels reflect current inventory — the date range doesn&apos;t apply.
          </p>
        )}
      </div>

      {reportMutation.isError && (
        <div role="alert" className="mt-4 rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
          {(reportMutation.error as ApiError).message || "Couldn't generate the report. Try again."}
        </div>
      )}

      <div className="mt-6">
        <Button onClick={() => reportMutation.mutate()} disabled={!canGenerate} isLoading={reportMutation.isPending}>
          Generate report
        </Button>
      </div>

      {result && (
        <div className="mt-8 border-t border-gray-800 pt-6">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-100">
              {reportLabel}
              {dateRangeApplies ? ` — ${formatDate(result.from)}–${formatDate(result.to)}` : ''}
            </h2>
            <Button variant="secondary" onClick={handleExport} isLoading={isExporting}>
              ↓ Export CSV
            </Button>
          </div>

          {type === 'TRANSFERS' && <TransfersResult result={result} />}
          {type === 'DONORS' && <DonorsResult result={result} />}
          {type === 'STOCK_LEVELS' && <StockLevelsResult result={result} />}
          {type === 'EXPIRY_WASTE' && <ExpiryWasteResult result={result} />}
        </div>
      )}
    </div>
  )
}
