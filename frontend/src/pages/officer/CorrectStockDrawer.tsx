import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateInventory } from '@/api/inventory'
import { ApiError } from '@/api/client'
import { Drawer } from '@/components/Drawer'
import { Button } from '@/components/Button'
import { useToast } from '@/components/Toast'
import type { InventoryResponse } from '@/api/types'

interface FieldErrors {
  units?: string
  reason?: string
  general?: string
}

export function CorrectStockDrawer({
  inventory,
  onClose,
}: {
  inventory: InventoryResponse | null
  onClose: () => void
}) {
  return (
    <Drawer open={inventory !== null} title={`Correct stock — ${inventory?.bloodGroup ?? ''}`} onClose={onClose}>
      {inventory && <CorrectStockForm key={inventory.id} inventory={inventory} onClose={onClose} />}
    </Drawer>
  )
}

function CorrectStockForm({
  inventory,
  onClose,
}: {
  inventory: InventoryResponse
  onClose: () => void
}) {
  const { show } = useToast()
  const queryClient = useQueryClient()
  const [units, setUnits] = useState(inventory.unitsAvailable)
  const [reason, setReason] = useState('')
  const [clientErrors, setClientErrors] = useState<FieldErrors>({})

  const mutation = useMutation({
    mutationFn: (payload: { units: number; reason: string }) => updateInventory(inventory.id, payload),
    onSuccess: (result) => {
      show(`${result.bloodGroup} stock corrected to ${result.unitsAvailable} units.`, 'success')
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      queryClient.invalidateQueries({ queryKey: ['inventory-audit', inventory.id] })
      onClose()
    },
    onError: (err: ApiError) => {
      if (err.status === 409 && /concurrently/i.test(err.message)) {
        show(err.message, 'error')
        queryClient.invalidateQueries({ queryKey: ['inventory'] })
        onClose()
      }
    },
  })

  const reservedFloorViolation =
    inventory.unitsReserved > 0 && units < inventory.unitsReserved
      ? `${inventory.unitsReserved} units are reserved for an approved transfer. You can't reduce stock below that.`
      : undefined

  const apiError = mutation.error as ApiError | undefined
  const isConcurrencyError = apiError?.status === 409 && /concurrently/i.test(apiError.message)
  const apiUnitsError =
    apiError && !isConcurrencyError ? (apiError.status === 409 ? apiError.message : undefined) : undefined
  const apiGeneralError =
    apiError && !isConcurrencyError && apiError.status !== 409 ? apiError.message : undefined

  // Priority: a failed-submit validation message, then the live reserved-floor
  // check (updates as they type), then whatever the backend actually said.
  const errors: FieldErrors = {
    units: clientErrors.units ?? reservedFloorViolation ?? apiUnitsError,
    reason: clientErrors.reason,
    general: apiGeneralError,
  }

  function validate(): FieldErrors {
    const next: FieldErrors = {}
    if (units < 0) next.units = 'Enter the corrected unit count'
    else if (reservedFloorViolation) next.units = reservedFloorViolation
    if (!reason.trim()) next.reason = 'Enter a reason for this correction'
    return next
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const validation = validate()
    if (Object.keys(validation).length > 0) {
      setClientErrors(validation)
      return
    }
    setClientErrors({})
    mutation.mutate({ units, reason: reason.trim() })
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
      {errors.general && (
        <div role="alert" className="rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
          {errors.general}
        </div>
      )}

      <div className="rounded-lg border border-gray-800 bg-gray-950 px-4 py-3 text-sm">
        <p className="text-gray-200">Current: {inventory.unitsAvailable} units</p>
        {inventory.unitsReserved > 0 && (
          <p className="mt-1 text-gray-400">{inventory.unitsReserved} reserved for transfer</p>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="new-count" className="text-sm font-medium text-gray-300">
          New count
        </label>
        <input
          id="new-count"
          type="number"
          inputMode="numeric"
          min={0}
          value={units}
          onChange={(e) => setUnits(Math.max(0, Number(e.target.value)))}
          aria-invalid={!!errors.units}
          className={`rounded-lg border bg-gray-900 px-3 py-2.5 text-gray-100 focus:outline-none focus:ring-2 focus:ring-brand-500 ${
            errors.units ? 'border-red-600' : 'border-gray-700'
          }`}
        />
        {errors.units && (
          <p role="alert" className="text-sm text-red-400">
            {errors.units}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="correction-reason" className="text-sm font-medium text-gray-300">
          Reason
        </label>
        <textarea
          id="correction-reason"
          rows={3}
          placeholder="e.g. 2 units damaged in handling"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={!!errors.reason}
          className={`rounded-lg border bg-gray-900 px-3 py-2.5 text-gray-100 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-500 ${
            errors.reason ? 'border-red-600' : 'border-gray-700'
          }`}
        />
        {errors.reason && (
          <p role="alert" className="text-sm text-red-400">
            {errors.reason}
          </p>
        )}
      </div>

      <div className="mt-2 flex justify-end gap-3 border-t border-gray-800 pt-4">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" isLoading={mutation.isPending}>
          Save
        </Button>
      </div>
    </form>
  )
}
