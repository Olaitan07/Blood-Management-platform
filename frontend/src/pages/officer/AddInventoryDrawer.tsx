import { useMemo, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addInventory } from '@/api/inventory'
import { ApiError } from '@/api/client'
import { Drawer } from '@/components/Drawer'
import { Button } from '@/components/Button'
import { Select } from '@/components/Select'
import { NumberStepper } from '@/components/NumberStepper'
import { TextField } from '@/components/TextField'
import { useToast } from '@/components/Toast'
import { BLOOD_GROUPS, type BloodGroup } from '@/api/types'

const WHOLE_BLOOD_MAX_SHELF_DAYS = 42

interface FormState {
  bloodGroup: BloodGroup | ''
  units: number
  expiryDate: string
}

const EMPTY_FORM: FormState = { bloodGroup: '', units: 1, expiryDate: '' }

function daysUntil(dateStr: string): number | null {
  if (!dateStr) return null
  const today = new Date()
  const todayUtc = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate())
  const [y, m, d] = dateStr.split('-').map(Number)
  const targetUtc = Date.UTC(y, m - 1, d)
  return Math.round((targetUtc - todayUtc) / (1000 * 60 * 60 * 24))
}

interface FieldErrors {
  bloodGroup?: string
  units?: string
  expiryDate?: string
  general?: string
}

export function AddInventoryDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { show } = useToast()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [addAnother, setAddAnother] = useState(false)
  const [clientErrors, setClientErrors] = useState<FieldErrors>({})
  const bloodGroupRef = useRef<HTMLSelectElement>(null)

  const daysUntilExpiry = useMemo(() => daysUntil(form.expiryDate), [form.expiryDate])
  const showShelfLifeWarning = daysUntilExpiry !== null && daysUntilExpiry > WHOLE_BLOOD_MAX_SHELF_DAYS

  const mutation = useMutation({
    mutationFn: addInventory,
    onSuccess: (result) => {
      show(`${form.units} units of ${result.bloodGroup} added`, 'success')
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      if (addAnother) {
        setForm(EMPTY_FORM)
        setClientErrors({})
        bloodGroupRef.current?.focus()
      } else {
        handleClose()
      }
    },
  })

  const apiErrors: FieldErrors = mutation.error
    ? mapApiError(mutation.error as ApiError)
    : {}
  const errors = { ...apiErrors, ...clientErrors }

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function validate(): FieldErrors {
    const next: FieldErrors = {}
    if (!form.bloodGroup) next.bloodGroup = 'Select a blood group'
    if (!form.units || form.units < 1) next.units = 'Enter a positive number of units'
    if (!form.expiryDate) {
      next.expiryDate = 'Enter an expiry date'
    } else if (daysUntilExpiry !== null && daysUntilExpiry <= 0) {
      next.expiryDate = "Expiry date can't be in the past"
    }
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
    mutation.mutate({
      bloodGroup: form.bloodGroup as BloodGroup,
      units: form.units,
      expiryDate: form.expiryDate,
      confirmShelfLife: showShelfLifeWarning,
    })
  }

  function handleClose() {
    setForm(EMPTY_FORM)
    setAddAnother(false)
    setClientErrors({})
    onClose()
  }

  return (
    <Drawer open={open} title="Add blood units" onClose={handleClose}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
        {errors.general && (
          <div role="alert" className="rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
            {errors.general}
          </div>
        )}

        <Select
          ref={bloodGroupRef}
          label="Blood group"
          value={form.bloodGroup}
          onChange={(e) => update('bloodGroup', e.target.value as BloodGroup)}
          error={errors.bloodGroup}
          autoFocus
        >
          <option value="">Select blood group</option>
          {BLOOD_GROUPS.map((bg) => (
            <option key={bg} value={bg}>
              {bg}
            </option>
          ))}
        </Select>

        <NumberStepper
          label="Units"
          value={form.units}
          onChange={(v) => update('units', v)}
          min={1}
          error={errors.units}
        />

        <div>
          <TextField
            label="Expiry date"
            type="date"
            value={form.expiryDate}
            onChange={(e) => update('expiryDate', e.target.value)}
            error={errors.expiryDate}
            required
          />
          {showShelfLifeWarning && !errors.expiryDate && (
            <div
              role="alert"
              className="mt-2 flex items-start gap-2 rounded-lg border border-amber-700 bg-amber-950 px-3 py-2.5 text-sm text-amber-300"
            >
              <span aria-hidden="true">⚠</span>
              <span>That&apos;s an unusually long shelf life for whole blood. Add anyway?</span>
            </div>
          )}
        </div>

        <label className="flex items-center gap-2 text-sm text-gray-300">
          <input
            type="checkbox"
            checked={addAnother}
            onChange={(e) => setAddAnother(e.target.checked)}
            className="h-4 w-4 rounded border-gray-700 bg-gray-900"
          />
          Add another after saving
        </label>

        <div className="mt-2 flex justify-end gap-3 border-t border-gray-800 pt-4">
          <Button type="button" variant="secondary" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" isLoading={mutation.isPending}>
            Add units
          </Button>
        </div>
      </form>
    </Drawer>
  )
}

function mapApiError(err: ApiError): FieldErrors {
  if (err.fieldErrors) return { ...err.fieldErrors }
  return { general: err.message }
}
