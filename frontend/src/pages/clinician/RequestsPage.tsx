import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelTransfer, getMyRequests } from '@/api/transfers'
import { ApiError } from '@/api/client'
import { useHospitalNames } from '@/hooks/useHospitalNames'
import { DataTable, type DataTableColumn } from '@/components/DataTable'
import { StatusBadge } from '@/components/StatusBadge'
import { Pagination } from '@/components/Pagination'
import { Modal } from '@/components/Modal'
import { Button } from '@/components/Button'
import { useToast } from '@/components/Toast'
import { formatRelativeTime } from '@/lib/dateFormat'
import type { TransferResponse } from '@/api/types'

type Tab = 'ALL' | 'ACTIVE' | 'PAST'

const ACTIVE_STATUSES = ['PENDING', 'APPROVED']
const CANCELLABLE_STATUSES = ['PENDING', 'APPROVED']

export function RequestsPage() {
  const { show } = useToast()
  const queryClient = useQueryClient()
  const { nameFor } = useHospitalNames()

  const [tab, setTab] = useState<Tab>('ALL')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [cancelTarget, setCancelTarget] = useState<TransferResponse | null>(null)

  const requestsQuery = useQuery({ queryKey: ['transfers', 'my-requests'], queryFn: getMyRequests })

  const sorted = useMemo(() => {
    const rows = requestsQuery.data ?? []
    return [...rows].sort((a, b) => b.requestDate.localeCompare(a.requestDate))
  }, [requestsQuery.data])

  const filtered = useMemo(() => {
    if (tab === 'ALL') return sorted
    if (tab === 'ACTIVE') return sorted.filter((r) => ACTIVE_STATUSES.includes(r.status))
    return sorted.filter((r) => !ACTIVE_STATUSES.includes(r.status))
  }, [sorted, tab])

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize))
  const pageRows = filtered.slice((page - 1) * pageSize, page * pageSize)

  const cancelMutation = useMutation({
    mutationFn: cancelTransfer,
    onSuccess: (result) => {
      show(`Request to ${nameFor(result.sourceHospitalId)} cancelled.`, 'success')
      setCancelTarget(null)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
    },
    onError: (err: ApiError) => {
      show(err.message, 'error')
      setCancelTarget(null)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
    },
  })

  const columns: DataTableColumn<TransferResponse>[] = [
    { key: 'hospital', header: 'Hospital', render: (r) => nameFor(r.sourceHospitalId) },
    { key: 'bloodGroup', header: 'Blood group', render: (r) => r.bloodGroup },
    { key: 'quantity', header: 'Qty', render: (r) => r.quantity },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'requested', header: 'Requested', render: (r) => formatRelativeTime(r.requestDate) },
    {
      key: 'actions',
      header: '',
      className: 'text-right',
      render: (r) =>
        CANCELLABLE_STATUSES.includes(r.status) ? (
          <button
            type="button"
            onClick={() => setCancelTarget(r)}
            aria-label={`Cancel request to ${nameFor(r.sourceHospitalId)}`}
            className="text-sm font-medium text-red-400 hover:underline"
          >
            Cancel
          </button>
        ) : null,
    },
  ]

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-gray-100">Requests</h1>
      <p className="mt-1 text-sm text-gray-500">
        Every transfer request made by your hospital — not only the ones you personally submitted.
      </p>

      <div
        role="tablist"
        aria-label="Filter by request status"
        className="mb-4 mt-4 inline-flex rounded-lg border border-gray-700 p-1"
      >
        {(['ALL', 'ACTIVE', 'PAST'] as Tab[]).map((t) => (
          <button
            key={t}
            type="button"
            role="tab"
            aria-selected={tab === t}
            onClick={() => {
              setTab(t)
              setPage(1)
            }}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              tab === t ? 'bg-white text-gray-950' : 'text-gray-400 hover:text-gray-200'
            }`}
          >
            {t === 'ALL' ? 'All' : t === 'ACTIVE' ? 'Active' : 'Past'}
          </button>
        ))}
      </div>

      {!requestsQuery.isLoading && !requestsQuery.isError && sorted.length === 0 ? (
        <div className="rounded-lg border border-gray-800 px-4 py-12 text-center text-gray-400">
          <p>You haven&apos;t requested any transfers yet.</p>
          <Link to="/search" className="mt-3 inline-block text-sm font-medium text-brand-500 hover:underline">
            Search blood availability
          </Link>
        </div>
      ) : (
        <DataTable
          columns={columns}
          rows={pageRows}
          rowKey={(r) => r.id}
          isLoading={requestsQuery.isLoading}
          error={requestsQuery.isError ? (requestsQuery.error as ApiError).message : null}
          onRetry={() => requestsQuery.refetch()}
          emptyMessage={tab === 'ACTIVE' ? 'No active requests.' : 'No requests match this filter.'}
        />
      )}

      {filtered.length > 0 && (
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
      )}

      <Modal
        open={cancelTarget !== null}
        title="Cancel request?"
        onClose={() => setCancelTarget(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setCancelTarget(null)}>
              Keep request
            </Button>
            <Button
              variant="danger"
              isLoading={cancelMutation.isPending}
              onClick={() => cancelTarget && cancelMutation.mutate(cancelTarget.id)}
            >
              Cancel request
            </Button>
          </>
        }
      >
        <p className="text-sm text-gray-300">
          {cancelTarget?.status === 'APPROVED'
            ? `This request has already been approved — ${cancelTarget.quantity} units of ${cancelTarget.bloodGroup} are reserved at ${nameFor(cancelTarget.sourceHospitalId)}. Cancelling will release that reservation.`
            : "This request hasn't been approved yet — cancelling simply withdraws it."}
        </p>
      </Modal>
    </div>
  )
}
