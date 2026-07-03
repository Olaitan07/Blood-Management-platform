import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getAuditLog, type AuditFilters } from '@/api/audit'
import { ApiError } from '@/api/client'
import { Pagination } from '@/components/Pagination'
import { formatDateTime } from '@/lib/dateFormat'
import type { AuditEventType, AuditRecordResponse, AuditTargetType } from '@/api/types'

// Exhaustive, verified against every @ApplicationModuleListener in
// AuditEventListener — these are the only eventType/targetType values that
// can ever be written. "Inventory" deliberately isn't offered as a filter:
// the audit module has no dependency on inventory::events at all, so it
// could never match a single row.
const EVENT_TYPE_OPTIONS: { value: AuditEventType; label: string }[] = [
  { value: 'BloodTransferRequestedEvent', label: 'Transfer requested' },
  { value: 'BloodTransferApprovedEvent', label: 'Transfer approved' },
  { value: 'BloodTransferRejectedEvent', label: 'Transfer rejected' },
  { value: 'BloodTransferCompletedEvent', label: 'Transfer completed' },
  { value: 'BloodTransferCancelledEvent', label: 'Transfer cancelled' },
  { value: 'DonorRegisteredEvent', label: 'Donor registered' },
  { value: 'HospitalRegisteredEvent', label: 'Hospital registered' },
  { value: 'HospitalDeactivatedEvent', label: 'Hospital deactivated' },
  { value: 'UserRegisteredEvent', label: 'User registered' },
  { value: 'AdminAuditEvent', label: 'Admin action' },
]

const TARGET_TYPE_OPTIONS: { value: AuditTargetType; label: string }[] = [
  { value: 'TRANSFER', label: 'Transfer' },
  { value: 'DONOR', label: 'Donor' },
  { value: 'HOSPITAL', label: 'Hospital' },
  { value: 'USER', label: 'User' },
]

function eventLabel(eventType: string): string {
  return EVENT_TYPE_OPTIONS.find((o) => o.value === eventType)?.label ?? eventType
}

function targetLabel(targetType: string): string {
  return TARGET_TYPE_OPTIONS.find((o) => o.value === targetType)?.label ?? targetType
}

interface DraftFilters {
  eventType: AuditEventType | ''
  actor: string
  targetType: AuditTargetType | ''
  fromDate: string
  toDate: string
}

const EMPTY_DRAFT: DraftFilters = { eventType: '', actor: '', targetType: '', fromDate: '', toDate: '' }

function toApiFilters(draft: DraftFilters): AuditFilters {
  return {
    eventType: draft.eventType || undefined,
    actor: draft.actor.trim() || undefined,
    targetType: draft.targetType || undefined,
    from: draft.fromDate ? `${draft.fromDate}T00:00:00Z` : undefined,
    to: draft.toDate ? `${draft.toDate}T23:59:59Z` : undefined,
  }
}

function hasAnyFilter(filters: AuditFilters): boolean {
  return Object.values(filters).some((v) => v !== undefined)
}

export function AuditLogPage() {
  const [draft, setDraft] = useState<DraftFilters>(EMPTY_DRAFT)
  const [applied, setApplied] = useState<AuditFilters>({})
  const [dateError, setDateError] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [expanded, setExpanded] = useState<Set<number>>(new Set())

  const auditQuery = useQuery({
    queryKey: ['audit', applied, page, pageSize],
    queryFn: () => getAuditLog(applied, page - 1, pageSize),
  })

  function handleApply() {
    if (draft.fromDate && draft.toDate && draft.fromDate > draft.toDate) {
      setDateError('Start date must be before end date')
      return
    }
    setDateError(null)
    setApplied(toApiFilters(draft))
    setPage(1)
  }

  function toggleExpanded(id: number) {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const filtersActive = hasAnyFilter(applied)
  const rows = auditQuery.data?.content ?? []

  return (
    <div className="mx-auto max-w-5xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-gray-100">Audit log</h1>

      <div
        role="note"
        className="mt-4 flex items-start gap-2 rounded-lg border border-gray-800 bg-gray-900 px-4 py-3 text-sm text-gray-300"
      >
        <span aria-hidden="true">ⓘ</span>
        <span>Covers auth, hospital, donor, and transfer events. Inventory changes aren&apos;t included yet.</span>
      </div>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1">
          <label htmlFor="event-type-filter" className="text-xs text-gray-500">
            Event type
          </label>
          <select
            id="event-type-filter"
            value={draft.eventType}
            onChange={(e) => setDraft((d) => ({ ...d, eventType: e.target.value as AuditEventType | '' }))}
            className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-200"
          >
            <option value="">Event type: all</option>
            {EVENT_TYPE_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="actor-filter" className="text-xs text-gray-500">
            Actor (exact match)
          </label>
          <input
            id="actor-filter"
            type="text"
            placeholder="e.g. admin@blood.com"
            value={draft.actor}
            onChange={(e) => setDraft((d) => ({ ...d, actor: e.target.value }))}
            className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100 placeholder:text-gray-500"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="target-type-filter" className="text-xs text-gray-500">
            Target type
          </label>
          <select
            id="target-type-filter"
            value={draft.targetType}
            onChange={(e) => setDraft((d) => ({ ...d, targetType: e.target.value as AuditTargetType | '' }))}
            className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-200"
          >
            <option value="">Target type: all</option>
            {TARGET_TYPE_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="from-date" className="text-xs text-gray-500">
            From
          </label>
          <input
            id="from-date"
            type="date"
            value={draft.fromDate}
            onChange={(e) => setDraft((d) => ({ ...d, fromDate: e.target.value }))}
            className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="to-date" className="text-xs text-gray-500">
            To
          </label>
          <input
            id="to-date"
            type="date"
            value={draft.toDate}
            onChange={(e) => setDraft((d) => ({ ...d, toDate: e.target.value }))}
            className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100"
          />
        </div>

        <button
          type="button"
          onClick={handleApply}
          className="rounded-lg bg-white px-4 py-2 text-sm font-semibold text-gray-950 hover:bg-gray-200"
        >
          Apply
        </button>
      </div>
      {dateError && (
        <p role="alert" className="mt-2 text-sm text-red-400">
          {dateError}
        </p>
      )}

      <div className="mt-6 overflow-hidden rounded-lg border border-gray-800">
        {auditQuery.isLoading && (
          <div className="animate-pulse space-y-2 p-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-12 rounded bg-gray-900" />
            ))}
          </div>
        )}

        {auditQuery.isError && (
          <div className="px-4 py-10 text-center">
            <p className="text-red-400">
              Couldn&apos;t load the audit log.{' '}
              {(auditQuery.error as ApiError)?.message ? `(${(auditQuery.error as ApiError).message})` : ''}
            </p>
            <button
              type="button"
              onClick={() => auditQuery.refetch()}
              className="mt-2 text-sm font-medium text-brand-500 hover:underline"
            >
              Retry
            </button>
          </div>
        )}

        {!auditQuery.isLoading && !auditQuery.isError && rows.length === 0 && (
          <p className="px-4 py-12 text-center text-gray-400">
            {filtersActive
              ? 'No matching events. Try widening your date range or clearing filters.'
              : 'No audit events recorded yet.'}
          </p>
        )}

        {!auditQuery.isLoading && !auditQuery.isError && rows.length > 0 && (
          <table className="w-full text-sm">
            <thead className="bg-gray-900 text-left text-xs text-gray-500">
              <tr>
                <th className="px-4 py-3 font-medium">Event</th>
                <th className="px-4 py-3 font-medium">Actor</th>
                <th className="px-4 py-3 font-medium">Target</th>
                <th className="px-4 py-3 font-medium">Occurred</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800 bg-gray-950">
              {rows.map((record) => (
                <AuditRow
                  key={record.id}
                  record={record}
                  isExpanded={expanded.has(record.id)}
                  onToggle={() => toggleExpanded(record.id)}
                />
              ))}
            </tbody>
          </table>
        )}
      </div>

      {rows.length > 0 && auditQuery.data && (
        <Pagination
          page={page}
          pageCount={Math.max(1, auditQuery.data.totalPages)}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={(size) => {
            setPageSize(size)
            setPage(1)
          }}
        />
      )}
    </div>
  )
}

function AuditRow({
  record,
  isExpanded,
  onToggle,
}: {
  record: AuditRecordResponse
  isExpanded: boolean
  onToggle: () => void
}) {
  return (
    <>
      <tr className="cursor-pointer hover:bg-gray-900" onClick={onToggle}>
        <td className="px-4 py-3 font-medium text-gray-100">{eventLabel(record.eventType)}</td>
        <td className="px-4 py-3 text-gray-300">{record.actor}</td>
        <td className="px-4 py-3 text-gray-300">
          {targetLabel(record.targetType)} #{record.targetId}
        </td>
        <td className="px-4 py-3 text-gray-400">{formatDateTime(record.occurredAt)}</td>
        <td className="px-4 py-3 text-right">
          <button
            type="button"
            aria-expanded={isExpanded}
            aria-label={`${isExpanded ? 'Hide' : 'Show'} details for ${eventLabel(record.eventType)} event`}
            onClick={(e) => {
              e.stopPropagation()
              onToggle()
            }}
            className="text-gray-500 hover:text-gray-200"
          >
            <span aria-hidden="true" className={`inline-block transition-transform ${isExpanded ? 'rotate-180' : ''}`}>
              ▾
            </span>
          </button>
        </td>
      </tr>
      {isExpanded && (
        <tr className="bg-gray-900">
          <td colSpan={5} className="px-4 py-3">
            <p className="mb-1 text-xs font-medium text-gray-500">Payload</p>
            <pre className="max-h-48 overflow-auto whitespace-pre-wrap break-words rounded bg-gray-950 p-3 font-mono text-xs text-gray-300">
              {record.payload}
            </pre>
          </td>
        </tr>
      )}
    </>
  )
}
