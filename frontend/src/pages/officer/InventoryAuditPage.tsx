import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getInventoryAuditLog } from '@/api/inventory'
import { getHospitalById } from '@/api/hospitals'
import { useAuth } from '@/auth/AuthContext'
import { Pagination } from '@/components/Pagination'
import { formatDateTime } from '@/lib/dateFormat'
import type { AuditLogResponse } from '@/api/types'

export function InventoryAuditPage() {
  const { id } = useParams<{ id: string }>()
  const inventoryId = Number(id)
  const { user } = useAuth()
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  const auditQuery = useQuery({
    queryKey: ['inventory-audit', inventoryId],
    queryFn: () => getInventoryAuditLog(inventoryId),
  })

  const hospitalQuery = useQuery({
    queryKey: ['hospital', user?.hospitalId],
    queryFn: () => getHospitalById(user!.hospitalId as number),
    enabled: user?.hospitalId != null,
  })

  const bloodGroup = auditQuery.data?.[0]?.bloodGroup
  const pageCount = auditQuery.data ? Math.max(1, Math.ceil(auditQuery.data.length / pageSize)) : 1
  const pageEntries = auditQuery.data?.slice((page - 1) * pageSize, page * pageSize) ?? []

  return (
    <div className="mx-auto max-w-2xl px-6 py-8">
      <Link to="/inventory" className="text-sm text-brand-500 hover:underline">
        ← Back to inventory
      </Link>

      <h1 className="mt-4 text-xl font-semibold text-gray-100">
        Audit trail{bloodGroup ? ` — ${bloodGroup}` : ''}
        {hospitalQuery.data ? ` (${hospitalQuery.data.name})` : ''}
      </h1>

      <div className="mt-6">
        {auditQuery.isLoading && <TimelineSkeleton />}

        {auditQuery.isError && (
          <div className="rounded-lg border border-gray-800 px-4 py-8 text-center">
            <p className="text-red-400">Couldn&apos;t load the audit trail.</p>
            <button
              type="button"
              onClick={() => auditQuery.refetch()}
              className="mt-2 text-sm font-medium text-brand-500 hover:underline"
            >
              Retry
            </button>
          </div>
        )}

        {auditQuery.isSuccess && auditQuery.data.length === 0 && (
          <p className="text-gray-500">No history recorded for this blood group yet.</p>
        )}

        {auditQuery.isSuccess && auditQuery.data.length > 0 && (
          <>
            <ol className="border-l border-gray-800 pl-4">
              {pageEntries.map((entry) => (
                <TimelineEntry key={entry.id} entry={entry} />
              ))}
            </ol>
            <Pagination
              page={page}
              pageCount={pageCount}
              pageSize={pageSize}
              onPageChange={setPage}
              onPageSizeChange={(size) => {
                setPageSize(size)
                setPage(1)
              }}
            />
          </>
        )}
      </div>
    </div>
  )
}

function TimelineEntry({ entry }: { entry: AuditLogResponse }) {
  const delta = entry.newUnits - entry.oldUnits
  const deltaColor = delta > 0 ? 'text-green-400' : delta < 0 ? 'text-red-400' : 'text-gray-400'

  return (
    <li className="relative mb-6 last:mb-0">
      <span className="absolute -left-[21px] top-1.5 h-2.5 w-2.5 rounded-full bg-gray-600" aria-hidden="true" />
      <p className="text-sm text-gray-500">{formatDateTime(entry.changedAt)}</p>
      <p className="mt-0.5 text-sm">
        <span className={`font-semibold ${deltaColor}`}>
          {entry.oldUnits} → {entry.newUnits} units
        </span>{' '}
        <span className="text-gray-500">· By: {entry.changedBy}</span>
      </p>
      <p className="text-sm text-gray-300">{entry.reason}</p>
    </li>
  )
}

function TimelineSkeleton() {
  return (
    <div className="animate-pulse space-y-4 border-l border-gray-800 pl-4">
      {[1, 2, 3].map((i) => (
        <div key={i} className="space-y-2">
          <div className="h-3 w-32 rounded bg-gray-800" />
          <div className="h-4 w-48 rounded bg-gray-800" />
          <div className="h-3 w-64 rounded bg-gray-800" />
        </div>
      ))}
    </div>
  )
}
