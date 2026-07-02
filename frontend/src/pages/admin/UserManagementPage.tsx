import { useMemo, useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { approveUser, changeUserRole, deactivateUser, listAllUsers, listPendingUsers } from '@/api/admin'
import { listHospitals } from '@/api/hospitals'
import { ApiError } from '@/api/client'
import { DataTable, type DataTableColumn } from '@/components/DataTable'
import { StatusBadge } from '@/components/StatusBadge'
import { KebabMenu, type KebabMenuItem } from '@/components/KebabMenu'
import { Pagination } from '@/components/Pagination'
import { Modal } from '@/components/Modal'
import { Button } from '@/components/Button'
import { Select } from '@/components/Select'
import { useToast } from '@/components/Toast'
import type { Role, UserResponse } from '@/api/types'

type Tab = 'pending' | 'all'

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'DONOR', label: 'Donor' },
  { value: 'CLINICIAN', label: 'Clinician' },
  { value: 'OFFICER', label: 'Officer' },
  { value: 'ADMIN', label: 'Admin' },
]

export function UserManagementPage() {
  const { show } = useToast()
  const queryClient = useQueryClient()

  const [tab, setTab] = useState<Tab>('pending')
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState<'ALL' | Role>('ALL')
  const [hospitalFilter, setHospitalFilter] = useState<'ALL' | number>('ALL')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [roleModalUser, setRoleModalUser] = useState<UserResponse | null>(null)
  const [deactivateTarget, setDeactivateTarget] = useState<UserResponse | null>(null)

  const usersQuery = useQuery({
    queryKey: ['admin-users', tab],
    queryFn: () => (tab === 'pending' ? listPendingUsers() : listAllUsers()),
  })

  const hospitalsQuery = useQuery({
    queryKey: ['hospitals', 'all'],
    queryFn: () => listHospitals(true),
  })

  const hospitalNameById = useMemo(() => {
    const map = new Map<number, string>()
    hospitalsQuery.data?.forEach((h) => map.set(h.id, h.name))
    return map
  }, [hospitalsQuery.data])

  const filtered = useMemo(() => {
    const users = usersQuery.data ?? []
    const term = search.trim().toLowerCase()
    return users.filter((u) => {
      if (term && !u.name.toLowerCase().includes(term) && !u.email.toLowerCase().includes(term)) {
        return false
      }
      if (roleFilter !== 'ALL' && u.role !== roleFilter) return false
      if (hospitalFilter !== 'ALL' && u.hospitalId !== hospitalFilter) return false
      return true
    })
  }, [usersQuery.data, search, roleFilter, hospitalFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize))
  const pageRows = filtered.slice((page - 1) * pageSize, page * pageSize)

  const approveMutation = useMutation({
    mutationFn: approveUser,
    onSuccess: (user) => {
      show(`${user.name} approved`, 'success')
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (err: ApiError) => show(err.message, 'error'),
  })

  const deactivateMutation = useMutation({
    mutationFn: deactivateUser,
    onSuccess: (user) => {
      show(`${user.name} deactivated`, 'success')
      setDeactivateTarget(null)
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (err: ApiError) => show(err.message, 'error'),
  })

  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: Role }) => changeUserRole(id, role),
    onSuccess: (user) => {
      show(`${user.name}'s role changed to ${user.role}`, 'success')
      setRoleModalUser(null)
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (err: ApiError) => show(err.message, 'error'),
  })

  const columns: DataTableColumn<UserResponse>[] = [
    { key: 'name', header: 'Name', render: (u) => <span className="font-medium text-gray-100">{u.name}</span> },
    { key: 'email', header: 'Email', render: (u) => u.email },
    { key: 'role', header: 'Role', render: (u) => titleCase(u.role) },
    {
      key: 'hospital',
      header: 'Hospital',
      render: (u) => (u.hospitalId ? hospitalNameById.get(u.hospitalId) ?? `#${u.hospitalId}` : '—'),
    },
    { key: 'status', header: 'Status', render: (u) => <StatusBadge status={u.status} /> },
    {
      key: 'actions',
      header: '',
      className: 'text-right',
      render: (u) => <RowActions user={u} />,
    },
  ]

  function RowActions({ user }: { user: UserResponse }) {
    const items: KebabMenuItem[] = []
    if (user.status === 'PENDING_APPROVAL') {
      items.push({ label: 'Approve', onClick: () => approveMutation.mutate(user.id) })
    }
    if (user.status === 'ACTIVE') {
      items.push({ label: 'Deactivate', tone: 'danger', onClick: () => setDeactivateTarget(user) })
    }
    items.push({ label: 'Change role', onClick: () => setRoleModalUser(user) })

    return (
      <div className="flex items-center justify-end gap-2">
        {user.status === 'PENDING_APPROVAL' && (
          <Button
            variant="secondary"
            size="sm"
            isLoading={approveMutation.isPending && approveMutation.variables === user.id}
            onClick={() => approveMutation.mutate(user.id)}
          >
            Approve
          </Button>
        )}
        <KebabMenu label={`Actions for ${user.name}`} items={items} />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-100">Users</h1>
        <div className="inline-flex rounded-lg border border-gray-700 p-1">
          <TabButton active={tab === 'pending'} onClick={() => { setTab('pending'); setPage(1) }}>
            Pending
          </TabButton>
          <TabButton active={tab === 'all'} onClick={() => { setTab('all'); setPage(1) }}>
            All
          </TabButton>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap gap-3">
        <input
          type="search"
          placeholder="Search users…"
          aria-label="Search users"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(1)
          }}
          className="min-w-[220px] flex-1 rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-100 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <select
          aria-label="Filter by role"
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value as 'ALL' | Role)
            setPage(1)
          }}
          className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-300"
        >
          <option value="ALL">Role: all</option>
          {ROLE_OPTIONS.map((r) => (
            <option key={r.value} value={r.value}>
              {r.label}
            </option>
          ))}
        </select>
        <select
          aria-label="Filter by hospital"
          value={hospitalFilter}
          onChange={(e) => {
            setHospitalFilter(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))
            setPage(1)
          }}
          className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-300"
        >
          <option value="ALL">Hospital: all</option>
          {hospitalsQuery.data?.map((h) => (
            <option key={h.id} value={h.id}>
              {h.name}
            </option>
          ))}
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={pageRows}
        rowKey={(u) => u.id}
        isLoading={usersQuery.isLoading}
        error={usersQuery.isError ? (usersQuery.error as ApiError).message : null}
        onRetry={() => usersQuery.refetch()}
        emptyMessage={
          tab === 'pending' ? 'No users are awaiting approval.' : 'No users match your filters.'
        }
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

      <RoleChangeModal
        key={roleModalUser?.id ?? 'none'}
        user={roleModalUser}
        onClose={() => setRoleModalUser(null)}
        onConfirm={(role) => roleModalUser && roleMutation.mutate({ id: roleModalUser.id, role })}
        isSubmitting={roleMutation.isPending}
      />

      <Modal
        open={deactivateTarget !== null}
        title="Deactivate user"
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
          {deactivateTarget?.name} will no longer be able to log in, and any active sessions will
          be rejected immediately. There is currently no way to reactivate a suspended account
          from this screen — the approve action only applies to accounts still awaiting approval.
        </p>
      </Modal>
    </div>
  )
}

function RoleChangeModal({
  user,
  onClose,
  onConfirm,
  isSubmitting,
}: {
  user: UserResponse | null
  onClose: () => void
  onConfirm: (role: Role) => void
  isSubmitting: boolean
}) {
  const [role, setRole] = useState<Role>(user?.role ?? 'CLINICIAN')

  return (
    <Modal
      open={user !== null}
      title={`Change role — ${user?.name ?? ''}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button isLoading={isSubmitting} onClick={() => onConfirm(role)}>
            Save
          </Button>
        </>
      }
    >
      <Select label="New role" value={role} onChange={(e) => setRole(e.target.value as Role)}>
        {ROLE_OPTIONS.map((r) => (
          <option key={r.value} value={r.value}>
            {r.label}
          </option>
        ))}
      </Select>
    </Modal>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
        active ? 'bg-white text-gray-950' : 'text-gray-400 hover:text-gray-200'
      }`}
    >
      {children}
    </button>
  )
}

function titleCase(role: Role): string {
  return role.charAt(0) + role.slice(1).toLowerCase()
}
