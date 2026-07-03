import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createTransfer } from '@/api/transfers'
import { ApiError } from '@/api/client'
import { Modal } from '@/components/Modal'
import { Button } from '@/components/Button'
import { NumberStepper } from '@/components/NumberStepper'
import { useToast } from '@/components/Toast'
import type { BloodSearchResult } from '@/api/types'

export function CreateTransferModal({
  target,
  onClose,
}: {
  target: BloodSearchResult | null
  onClose: () => void
}) {
  return (
    <Modal open={target !== null} title="Request blood transfer" onClose={onClose}>
      {target && <CreateTransferForm target={target} onClose={onClose} />}
    </Modal>
  )
}

function CreateTransferForm({ target, onClose }: { target: BloodSearchResult; onClose: () => void }) {
  const navigate = useNavigate()
  const { show } = useToast()
  const queryClient = useQueryClient()

  const [quantity, setQuantity] = useState(1)
  const [error, setError] = useState<string | null>(null)
  // Stable per open-session, not regenerated per click — that's what makes a
  // genuine double-click safe: both attempts carry the same key, so the
  // backend's existsByIdempotencyKey check catches the second one cleanly.
  const idempotencyKey = useRef(crypto.randomUUID())
  const hasSubmitted = useRef(false)

  useEffect(() => {
    idempotencyKey.current = crypto.randomUUID()
    hasSubmitted.current = false
    setQuantity(1)
    setError(null)
  }, [target.hospitalId, target.bloodGroup])

  const mutation = useMutation({
    mutationFn: createTransfer,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
      onClose()
      show(`Request sent to ${target.hospitalName}.`, 'success')
      navigate('/transfers/my-requests')
    },
    onError: (err: ApiError) => {
      hasSubmitted.current = false
      if (err.status === 409 && /duplicate request/i.test(err.message)) {
        onClose()
        show('This request may have already been sent — check My Requests.', 'info')
        navigate('/transfers/my-requests')
        return
      }
      setError(err.message)
    },
  })

  function handleSubmit() {
    if (hasSubmitted.current) return
    if (quantity < 1) {
      setError('Enter a quantity greater than zero')
      return
    }
    hasSubmitted.current = true
    setError(null)
    mutation.mutate({
      sourceHospitalId: target.hospitalId,
      bloodGroup: target.bloodGroup,
      quantity,
      idempotencyKey: idempotencyKey.current,
    })
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-lg border border-gray-800 bg-gray-950 px-4 py-3 text-sm">
        <p className="text-gray-400">From</p>
        <p className="font-semibold text-gray-100">{target.hospitalName}</p>
        <p className="mt-1 text-gray-400">
          {target.bloodGroup} · {target.availableUnits} units available
        </p>
      </div>

      {error && (
        <div role="alert" className="rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
          {error}
        </div>
      )}

      <NumberStepper label="Quantity needed" value={quantity} onChange={setQuantity} min={1} />

      <div className="mt-2 flex justify-end gap-3 border-t border-gray-800 pt-4">
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button onClick={handleSubmit} isLoading={mutation.isPending || hasSubmitted.current}>
          Send request
        </Button>
      </div>
    </div>
  )
}
