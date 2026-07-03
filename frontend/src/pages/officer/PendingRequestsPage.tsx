import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { approveTransfer, getPendingRequests, rejectTransfer } from '@/api/transfers'
import { ApiError } from '@/api/client'
import { useAuth } from '@/auth/AuthContext'
import { useHospitalNames } from '@/hooks/useHospitalNames'
import { getHospitalById } from '@/api/hospitals'
import { DataTable, type DataTableColumn } from '@/components/DataTable'
import { Modal } from '@/components/Modal'
import { Button } from '@/components/Button'
import { useToast } from '@/components/Toast'
import { formatRelativeTime } from '@/lib/dateFormat'
import type { TransferResponse } from '@/api/types'

export function PendingRequestsPage() {
  const { show } = useToast()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { nameFor } = useHospitalNames()

  const hospitalQuery = useQuery({
    queryKey: ['hospital', user?.hospitalId],
    queryFn: () => getHospitalById(user!.hospitalId as number),
    enabled: user?.hospitalId != null,
  })

  const [approveTarget, setApproveTarget] = useState<TransferResponse | null>(null)
  const [rejectTarget, setRejectTarget] = useState<TransferResponse | null>(null)

  const pendingQuery = useQuery({ queryKey: ['transfers', 'pending'], queryFn: getPendingRequests })

  const approveMutation = useMutation({
    mutationFn: approveTransfer,
    onSuccess: (result) => {
      show(`Approved — ${result.quantity} units of ${result.bloodGroup} reserved for ${nameFor(result.requestingHospitalId)}`, 'success')
      setApproveTarget(null)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
    },
    onError: (err: ApiError) => {
      show(err.message, 'error')
      setApproveTarget(null)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
    },
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => rejectTransfer(id, { reason }),
    onSuccess: (result) => {
      show(`Request from ${nameFor(result.requestingHospitalId)} rejected`, 'success')
      setRejectTarget(null)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
    },
  })

  const columns: DataTableColumn<TransferResponse>[] = [
    { key: 'hospital', header: 'Requesting hospital', render: (r) => nameFor(r.requestingHospitalId) },
    { key: 'bloodGroup', header: 'Blood group', render: (r) => r.bloodGroup },
    { key: 'quantity', header: 'Qty', render: (r) => r.quantity },
    { key: 'requested', header: 'Requested', render: (r) => formatRelativeTime(r.requestDate) },
    {
      key: 'actions',
      header: '',
      className: 'text-right',
      render: (r) => (
        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={() => setRejectTarget(r)}
            aria-label={`Reject request from ${nameFor(r.requestingHospitalId)}`}
            className="rounded-lg border border-gray-700 px-3 py-1.5 text-sm font-semibold text-gray-200 hover:bg-gray-800"
          >
            Reject
          </button>
          <button
            type="button"
            onClick={() => setApproveTarget(r)}
            aria-label={`Approve request from ${nameFor(r.requestingHospitalId)}`}
            className="rounded-lg bg-white px-3 py-1.5 text-sm font-semibold text-gray-950 hover:bg-gray-200"
          >
            Approve
          </button>
        </div>
      ),
    },
  ]

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-gray-100">
        Pending requests{hospitalQuery.data ? ` — ${hospitalQuery.data.name}` : ''}
      </h1>

      <div className="mt-6">
        <DataTable
          columns={columns}
          rows={pendingQuery.data ?? []}
          rowKey={(r) => r.id}
          isLoading={pendingQuery.isLoading}
          error={pendingQuery.isError ? (pendingQuery.error as ApiError).message : null}
          onRetry={() => pendingQuery.refetch()}
          emptyMessage="No pending requests right now."
        />
      </div>

      <Modal
        open={approveTarget !== null}
        title="Approve request?"
        onClose={() => setApproveTarget(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setApproveTarget(null)}>
              Cancel
            </Button>
            <Button
              isLoading={approveMutation.isPending}
              onClick={() => approveTarget && approveMutation.mutate(approveTarget.id)}
            >
              Approve
            </Button>
          </>
        }
      >
        <p className="text-sm text-gray-300">
          Approve request for {approveTarget?.quantity} units of {approveTarget?.bloodGroup}?
        </p>
      </Modal>

      <RejectModal target={rejectTarget} onClose={() => setRejectTarget(null)} onConfirm={(reason) => rejectTarget && rejectMutation.mutate({ id: rejectTarget.id, reason })} isSubmitting={rejectMutation.isPending} apiError={rejectMutation.error as ApiError | null} />
    </div>
  )
}

function RejectModal({
  target,
  onClose,
  onConfirm,
  isSubmitting,
  apiError,
}: {
  target: TransferResponse | null
  onClose: () => void
  onConfirm: (reason: string) => void
  isSubmitting: boolean
  apiError: ApiError | null
}) {
  const [reason, setReason] = useState('')
  const [clientError, setClientError] = useState<string | null>(null)

  function handleClose() {
    setReason('')
    setClientError(null)
    onClose()
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!reason.trim()) {
      setClientError('Enter a reason for rejecting this request')
      return
    }
    setClientError(null)
    onConfirm(reason.trim())
  }

  const error = clientError ?? apiError?.fieldErrors?.reason ?? (apiError && !apiError.fieldErrors ? apiError.message : undefined)

  return (
    <Modal open={target !== null} title="Reject request" onClose={handleClose}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
        <p className="text-sm text-gray-300">
          Reject the request for {target?.quantity} units of {target?.bloodGroup}?
        </p>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="reject-reason" className="text-sm font-medium text-gray-300">
            Reason
          </label>
          <textarea
            id="reject-reason"
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!error}
            className={`rounded-lg border bg-gray-900 px-3 py-2.5 text-gray-100 focus:outline-none focus:ring-2 focus:ring-brand-500 ${
              error ? 'border-red-600' : 'border-gray-700'
            }`}
          />
          {error && (
            <p role="alert" className="text-sm text-red-400">
              {error}
            </p>
          )}
        </div>
        <div className="mt-2 flex justify-end gap-3 border-t border-gray-800 pt-4">
          <Button type="button" variant="secondary" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" variant="danger" isLoading={isSubmitting}>
            Reject
          </Button>
        </div>
      </form>
    </Modal>
  )
}
