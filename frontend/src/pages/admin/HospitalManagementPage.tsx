import { useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createHospital, deactivateHospital, listHospitals } from '@/api/hospitals'
import { ApiError } from '@/api/client'
import { DataTable, type DataTableColumn } from '@/components/DataTable'
import { StatusBadge } from '@/components/StatusBadge'
import { KebabMenu, type KebabMenuItem } from '@/components/KebabMenu'
import { Pagination } from '@/components/Pagination'
import { Modal } from '@/components/Modal'
import { Drawer } from '@/components/Drawer'
import { Button } from '@/components/Button'
import { TextField } from '@/components/TextField'
import { useToast } from '@/components/Toast'
import type { HospitalRequest, HospitalResponse, HospitalStatus } from '@/api/types'

type StatusFilter = HospitalStatus | 'ALL'

const EMPTY_FORM: HospitalRequest = { name: '', address: '', city: '', state: '', contact: '' }

export function HospitalManagementPage() {
  const { show } = useToast()
  const queryClient = useQueryClient()

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ACTIVE')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [deactivateTarget, setDeactivateTarget] = useState<HospitalResponse | null>(null)

  // The list endpoint only distinguishes active-only vs. everything server-side
  // (no "inactive only" option) — fetch everything once and do all status/text
  // filtering client-side, same approach as the Admin Users screen.
  const hospitalsQuery = useQuery({
    queryKey: ['hospitals', 'all'],
    queryFn: () => listHospitals(true),
  })

  const filtered = useMemo(() => {
    const hospitals = hospitalsQuery.data ?? []
    const term = search.trim().toLowerCase()
    return hospitals.filter((h) => {
      if (statusFilter !== 'ALL' && h.status !== statusFilter) return false
      if (
        term &&
        !h.name.toLowerCase().includes(term) &&
        !h.city.toLowerCase().includes(term) &&
        !h.contact.toLowerCase().includes(term)
      ) {
        return false
      }
      return true
    })
  }, [hospitalsQuery.data, search, statusFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize))
  const pageRows = filtered.slice((page - 1) * pageSize, page * pageSize)

  const createMutation = useMutation({
    mutationFn: createHospital,
    onSuccess: (hospital) => {
      show(`${hospital.name} added`, 'success')
      setDrawerOpen(false)
      queryClient.invalidateQueries({ queryKey: ['hospitals'] })
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: deactivateHospital,
    onSuccess: (hospital) => {
      show(`${hospital.name} deactivated`, 'success')
      setDeactivateTarget(null)
      queryClient.invalidateQueries({ queryKey: ['hospitals'] })
    },
    onError: (err: ApiError) => show(err.message, 'error'),
  })

  const columns: DataTableColumn<HospitalResponse>[] = [
    { key: 'name', header: 'Name', render: (h) => <span className="font-medium text-gray-100">{h.name}</span> },
    { key: 'city', header: 'City', render: (h) => h.city },
    { key: 'state', header: 'State', render: (h) => h.state },
    { key: 'contact', header: 'Contact', render: (h) => h.contact },
    { key: 'status', header: 'Status', render: (h) => <StatusBadge status={h.status} /> },
    {
      key: 'actions',
      header: '',
      className: 'text-right',
      render: (h) => (h.status === 'ACTIVE' ? <RowActions hospital={h} /> : null),
    },
  ]

  function RowActions({ hospital }: { hospital: HospitalResponse }) {
    const items: KebabMenuItem[] = [
      { label: 'Deactivate', tone: 'danger', onClick: () => setDeactivateTarget(hospital) },
    ]
    return (
      <div className="flex justify-end">
        <KebabMenu label={`Actions for ${hospital.name}`} items={items} />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-100">Hospitals</h1>
        <Button onClick={() => setDrawerOpen(true)}>+ Add hospital</Button>
      </div>

      <div className="mb-4 flex flex-wrap gap-3">
        <input
          type="search"
          placeholder="Search hospitals…"
          aria-label="Search hospitals"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(1)
          }}
          className="min-w-[220px] flex-1 rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <select
          aria-label="Filter by status"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as StatusFilter)
            setPage(1)
          }}
          className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-300"
        >
          <option value="ACTIVE">Status: active</option>
          <option value="INACTIVE">Status: inactive</option>
          <option value="ALL">Status: all</option>
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={pageRows}
        rowKey={(h) => h.id}
        isLoading={hospitalsQuery.isLoading}
        error={hospitalsQuery.isError ? (hospitalsQuery.error as ApiError).message : null}
        onRetry={() => hospitalsQuery.refetch()}
        emptyMessage="No hospitals match your filters."
      />

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

      <AddHospitalDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSubmit={(payload) => createMutation.mutate(payload)}
        isSubmitting={createMutation.isPending}
        apiError={createMutation.error as ApiError | null}
      />

      <Modal
        open={deactivateTarget !== null}
        title={`Deactivate ${deactivateTarget?.name ?? ''}?`}
        onClose={() => setDeactivateTarget(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeactivateTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              isLoading={deactivateMutation.isPending}
              onClick={() => deactivateTarget && deactivateMutation.mutate(deactivateTarget.id)}
            >
              Deactivate
            </Button>
          </>
        }
      >
        <p className="text-sm text-gray-300">
          It will no longer appear in search or transfer requests, and its staff will lose access.
          Past records stay visible in reports.
        </p>
      </Modal>
    </div>
  )
}

interface FieldErrors {
  name?: string
  address?: string
  city?: string
  state?: string
  contact?: string
  general?: string
}

function mapApiError(err: ApiError | null): FieldErrors {
  if (!err) return {}
  if (err.fieldErrors) return { ...err.fieldErrors }
  if (err.status === 409) return { general: err.message }
  return { general: err.message }
}

function AddHospitalDrawer({
  open,
  onClose,
  onSubmit,
  isSubmitting,
  apiError,
}: {
  open: boolean
  onClose: () => void
  onSubmit: (payload: HospitalRequest) => void
  isSubmitting: boolean
  apiError: ApiError | null
}) {
  const [form, setForm] = useState<HospitalRequest>(EMPTY_FORM)
  const [clientErrors, setClientErrors] = useState<FieldErrors>({})

  const errors = { ...mapApiError(apiError), ...clientErrors }

  function update<K extends keyof HospitalRequest>(key: K, value: HospitalRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function validate(): FieldErrors {
    const next: FieldErrors = {}
    if (!form.name.trim()) next.name = 'Name is required'
    if (!form.address.trim()) next.address = 'Address is required'
    if (!form.city.trim()) next.city = 'City is required'
    if (!form.state.trim()) next.state = 'State is required'
    if (!form.contact.trim()) next.contact = 'Contact is required'
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
    onSubmit(form)
  }

  function handleClose() {
    setForm(EMPTY_FORM)
    setClientErrors({})
    onClose()
  }

  return (
    <Drawer open={open} title="Add hospital" onClose={handleClose}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
        {errors.general && (
          <div role="alert" className="rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
            {errors.general}
          </div>
        )}

        <TextField
          label="Name"
          placeholder="Hospital name"
          value={form.name}
          onChange={(e) => update('name', e.target.value)}
          error={errors.name}
          required
        />
        <TextField
          label="Address"
          placeholder="Street address"
          value={form.address}
          onChange={(e) => update('address', e.target.value)}
          error={errors.address}
          required
        />
        <div className="grid grid-cols-2 gap-4">
          <TextField
            label="City"
            placeholder="City"
            value={form.city}
            onChange={(e) => update('city', e.target.value)}
            error={errors.city}
            required
          />
          <TextField
            label="State"
            placeholder="State"
            value={form.state}
            onChange={(e) => update('state', e.target.value)}
            error={errors.state}
            required
          />
        </div>
        <TextField
          label="Contact"
          placeholder="(555) 010-0000"
          value={form.contact}
          onChange={(e) => update('contact', e.target.value)}
          error={errors.contact}
          required
        />

        <div className="mt-2 flex justify-end gap-3 border-t border-gray-800 pt-4">
          <Button type="button" variant="secondary" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" isLoading={isSubmitting}>
            Save
          </Button>
        </div>
      </form>
    </Drawer>
  )
}
