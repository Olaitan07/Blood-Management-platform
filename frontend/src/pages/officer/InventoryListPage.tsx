import { useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { listInventory } from '@/api/inventory'
import { getHospitalById } from '@/api/hospitals'
import { ApiError } from '@/api/client'
import { useAuth } from '@/auth/AuthContext'
import { DataTable, type DataTableColumn } from '@/components/DataTable'
import { StatusBadge } from '@/components/StatusBadge'
import { KebabMenu, type KebabMenuItem } from '@/components/KebabMenu'
import { Pagination } from '@/components/Pagination'
import { Button } from '@/components/Button'
import { AddInventoryDrawer } from './AddInventoryDrawer'
import { CorrectStockDrawer } from './CorrectStockDrawer'
import { BLOOD_GROUPS, type BloodGroup, type InventoryResponse } from '@/api/types'

type Tab = 'ALL' | 'EXPIRING_SOON' | 'EXPIRED'

// A hospital's blood-group line with no inventory row at all yet is real,
// useful information ("zero AB- on hand") — not an absence to hide. Only
// synthesized when the hospital has at least one real row already, so a
// genuinely brand-new hospital gets the big empty state instead of 8
// placeholder rows.
interface NoStockRow {
  synthetic: true
  bloodGroup: BloodGroup
}
type DisplayRow = InventoryResponse | NoStockRow

function isSynthetic(row: DisplayRow): row is NoStockRow {
  return 'synthetic' in row
}

export function InventoryListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [tab, setTab] = useState<Tab>('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [addDrawerOpen, setAddDrawerOpen] = useState(false)
  const [correctTarget, setCorrectTarget] = useState<InventoryResponse | null>(null)

  const inventoryQuery = useQuery({ queryKey: ['inventory'], queryFn: listInventory })

  const hospitalQuery = useQuery({
    queryKey: ['hospital', user?.hospitalId],
    queryFn: () => getHospitalById(user!.hospitalId as number),
    enabled: user?.hospitalId != null,
  })

  const realRows = inventoryQuery.data ?? []

  const displayRows = useMemo<DisplayRow[]>(() => {
    const rows = inventoryQuery.data ?? []
    if (rows.length === 0) return []
    const present = new Set(rows.map((r) => r.bloodGroup))
    const noStock: NoStockRow[] = BLOOD_GROUPS.filter((bg) => !present.has(bg)).map((bg) => ({
      synthetic: true,
      bloodGroup: bg,
    }))
    return [...rows, ...noStock]
  }, [inventoryQuery.data])

  const expiringSoonCount = realRows.filter((r) => r.status === 'EXPIRING_SOON').length
  const expiredCount = realRows.filter((r) => r.status === 'EXPIRED').length

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    return displayRows.filter((row) => {
      if (tab !== 'ALL' && (isSynthetic(row) || row.status !== tab)) return false
      if (term && !row.bloodGroup.toLowerCase().includes(term)) return false
      return true
    })
  }, [displayRows, tab, search])

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize))
  const pageRows = filtered.slice((page - 1) * pageSize, page * pageSize)

  const columns: DataTableColumn<DisplayRow>[] = [
    {
      key: 'bloodGroup',
      header: 'Blood group',
      render: (row) => <span className="font-medium text-gray-100">{row.bloodGroup}</span>,
    },
    { key: 'units', header: 'Units', render: (row) => (isSynthetic(row) ? 0 : row.unitsAvailable) },
    { key: 'reserved', header: 'Reserved', render: (row) => (isSynthetic(row) ? 0 : row.unitsReserved) },
    {
      key: 'expires',
      header: 'Expires',
      render: (row) => (isSynthetic(row) ? '—' : formatDate(row.expiryDate)),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={isSynthetic(row) ? 'NO_STOCK' : row.status} />,
    },
    {
      key: 'actions',
      header: '',
      className: 'text-right',
      render: (row) => (isSynthetic(row) ? null : <RowActions row={row} />),
    },
  ]

  function RowActions({ row }: { row: InventoryResponse }) {
    const items: KebabMenuItem[] = [
      { label: 'Correct stock', onClick: () => setCorrectTarget(row) },
      { label: 'View audit trail', onClick: () => navigate(`/inventory/${row.id}/audit`) },
    ]
    return (
      <div className="flex justify-end">
        <KebabMenu label={`Actions for ${row.bloodGroup}`} items={items} />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-6 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-100">
          Inventory{hospitalQuery.data ? ` — ${hospitalQuery.data.name}` : ''}
        </h1>
        <Button onClick={() => setAddDrawerOpen(true)}>+ Add units</Button>
      </div>

      <div role="tablist" aria-label="Filter by expiry status" className="mb-4 inline-flex rounded-lg border border-gray-700 p-1">
        <TabButton active={tab === 'ALL'} onClick={() => { setTab('ALL'); setPage(1) }}>
          All
        </TabButton>
        <TabButton active={tab === 'EXPIRING_SOON'} onClick={() => { setTab('EXPIRING_SOON'); setPage(1) }} tone="warning">
          Expiring soon ({expiringSoonCount})
        </TabButton>
        <TabButton active={tab === 'EXPIRED'} onClick={() => { setTab('EXPIRED'); setPage(1) }} tone="danger">
          Expired ({expiredCount})
        </TabButton>
      </div>

      <input
        type="search"
        placeholder="Search blood group…"
        aria-label="Search blood group"
        value={search}
        onChange={(e) => {
          setSearch(e.target.value)
          setPage(1)
        }}
        className="mb-4 w-full max-w-xs rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
      />

      {!inventoryQuery.isLoading && !inventoryQuery.isError && realRows.length === 0 ? (
        <div className="rounded-lg border border-gray-800 px-4 py-12 text-center text-gray-400">
          <p>No blood units recorded yet. Add your first units to get started.</p>
          <Button className="mt-4" onClick={() => setAddDrawerOpen(true)}>
            + Add units
          </Button>
        </div>
      ) : (
        <DataTable
          columns={columns}
          rows={pageRows}
          rowKey={(row) => (isSynthetic(row) ? `no-stock-${row.bloodGroup}` : row.id)}
          isLoading={inventoryQuery.isLoading}
          error={inventoryQuery.isError ? (inventoryQuery.error as ApiError).message : null}
          onRetry={() => inventoryQuery.refetch()}
          emptyMessage={
            tab === 'EXPIRING_SOON'
              ? 'Nothing expiring in the next 7 days.'
              : tab === 'EXPIRED'
                ? 'No expired stock.'
                : 'No blood groups match your search.'
          }
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

      <AddInventoryDrawer open={addDrawerOpen} onClose={() => setAddDrawerOpen(false)} />
      <CorrectStockDrawer inventory={correctTarget} onClose={() => setCorrectTarget(null)} />
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
  tone,
}: {
  active: boolean
  onClick: () => void
  children: ReactNode
  tone?: 'warning' | 'danger'
}) {
  const activeToneClass =
    tone === 'warning' ? 'text-amber-400' : tone === 'danger' ? 'text-red-400' : 'text-gray-400'
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
        active ? 'bg-white text-gray-950' : `${activeToneClass} hover:text-gray-200`
      }`}
    >
      {children}
    </button>
  )
}

function formatDate(isoDate: string): string {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}
