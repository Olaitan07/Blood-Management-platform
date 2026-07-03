import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { completeTransfer, getMyRequests } from '@/api/transfers'
import { ApiError } from '@/api/client'
import { useAuth } from '@/auth/AuthContext'
import { useHospitalNames } from '@/hooks/useHospitalNames'
import { getHospitalById } from '@/api/hospitals'
import { DataTable, type DataTableColumn } from '@/components/DataTable'
import { Modal } from '@/components/Modal'
import { Button } from '@/components/Button'
import { NumberStepper } from '@/components/NumberStepper'
import { useToast } from '@/components/Toast'
import { formatRelativeTime } from '@/lib/dateFormat'
import type { TransferResponse } from '@/api/types'

export function IncomingTransfersPage() {
  const { user } = useAuth()
  const { nameFor } = useHospitalNames()
  const [receiptTarget, setReceiptTarget] = useState<TransferResponse | null>(null)

  const hospitalQuery = useQuery({
    queryKey: ['hospital', user?.hospitalId],
    queryFn: () => getHospitalById(user!.hospitalId as number),
    enabled: user?.hospitalId != null,
  })

  // No dedicated "approved, awaiting receipt" endpoint exists — my-requests
  // is hospital-wide across every status, so this filters to APPROVED only.
  const requestsQuery = useQuery({ queryKey: ['transfers', 'my-requests'], queryFn: getMyRequests })
  const incoming = useMemo(
    () => (requestsQuery.data ?? []).filter((r) => r.status === 'APPROVED'),
    [requestsQuery.data],
  )

  const columns: DataTableColumn<TransferResponse>[] = [
    { key: 'source', header: 'Source hospital', render: (r) => nameFor(r.sourceHospitalId) },
    { key: 'bloodGroup', header: 'Blood group', render: (r) => r.bloodGroup },
    { key: 'quantity', header: 'Approved qty', render: (r) => r.quantity },
    {
      key: 'approved',
      header: 'Approved',
      render: (r) => (r.approvalDate ? formatRelativeTime(r.approvalDate) : '—'),
    },
    {
      key: 'actions',
      header: '',
      className: 'text-right',
      render: (r) => (
        <button
          type="button"
          onClick={() => setReceiptTarget(r)}
          className="rounded-lg bg-white px-3 py-1.5 text-sm font-semibold text-gray-950 hover:bg-gray-200"
        >
          Record receipt
        </button>
      ),
    },
  ]

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-gray-100">
        Incoming transfers{hospitalQuery.data ? ` — ${hospitalQuery.data.name}` : ''}
      </h1>

      <div className="mt-6">
        <DataTable
          columns={columns}
          rows={incoming}
          rowKey={(r) => r.id}
          isLoading={requestsQuery.isLoading}
          error={requestsQuery.isError ? (requestsQuery.error as ApiError).message : null}
          onRetry={() => requestsQuery.refetch()}
          emptyMessage="No incoming transfers right now."
        />
      </div>

      <ConfirmReceiptModal target={receiptTarget} onClose={() => setReceiptTarget(null)} />
    </div>
  )
}

function ConfirmReceiptModal({
  target,
  onClose,
}: {
  target: TransferResponse | null
  onClose: () => void
}) {
  return (
    <Modal open={target !== null} title={`Confirm receipt — ${target?.bloodGroup ?? ''}`} onClose={onClose}>
      {target && <ConfirmReceiptForm key={target.id} target={target} onClose={onClose} />}
    </Modal>
  )
}

function ConfirmReceiptForm({ target, onClose }: { target: TransferResponse; onClose: () => void }) {
  const { show } = useToast()
  const { nameFor } = useHospitalNames()
  const queryClient = useQueryClient()
  const [unitsReceived, setUnitsReceived] = useState(target.quantity)

  const mutation = useMutation({
    mutationFn: () => completeTransfer(target.id, { unitsReceived }),
    onSuccess: (result) => {
      const received = result.unitsReceived ?? unitsReceived
      if (received >= target.quantity) {
        show(`Receipt confirmed — ${received} units of ${result.bloodGroup} added to your inventory.`, 'success')
      } else {
        show(
          `Receipt confirmed — ${received} of ${target.quantity} units received. The difference has been logged.`,
          'success',
        )
      }
      onClose()
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })

  const apiError = mutation.error as ApiError | null

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(e) => {
        e.preventDefault()
        mutation.mutate()
      }}
      noValidate
    >
      {apiError && (
        <div role="alert" className="rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
          {apiError.message}
        </div>
      )}

      <div className="rounded-lg border border-gray-800 bg-gray-950 px-4 py-3 text-sm">
        <p className="text-gray-400">From {nameFor(target.sourceHospitalId)}</p>
        <p className="font-semibold text-gray-100">Approved: {target.quantity} units</p>
      </div>

      <NumberStepper
        label="Units received"
        value={unitsReceived}
        onChange={setUnitsReceived}
        min={0}
        max={target.quantity}
      />
      {unitsReceived < target.quantity && (
        <p className="text-sm text-amber-400">
          Units not received won&apos;t be added to your available stock.
        </p>
      )}

      <div className="mt-2 flex justify-end gap-3 border-t border-gray-800 pt-4">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" isLoading={mutation.isPending}>
          Confirm
        </Button>
      </div>
    </form>
  )
}
