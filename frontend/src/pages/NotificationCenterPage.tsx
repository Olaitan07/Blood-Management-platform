import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useNotifications } from '@/hooks/useNotifications'
import { getNotificationLink } from '@/lib/notificationLinks'
import { NotificationEntry } from '@/components/NotificationEntry'
import { Pagination } from '@/components/Pagination'
import type { NotificationResponse, NotificationType } from '@/api/types'

type ReadTab = 'ALL' | 'UNREAD'
type TypeFilter = 'ALL' | NotificationType

export function NotificationCenterPage() {
  const navigate = useNavigate()
  const { notifications, unreadCount, isLoading, isError, refetch, markRead, canMarkAllRead, markAllRead } =
    useNotifications()

  const [tab, setTab] = useState<ReadTab>('ALL')
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)

  const sorted = useMemo(
    () => [...notifications].sort((a, b) => b.sentAt.localeCompare(a.sentAt)),
    [notifications],
  )

  const filtered = useMemo(() => {
    return sorted
      .filter((n) => tab !== 'UNREAD' || !n.read)
      .filter((n) => typeFilter === 'ALL' || n.type === typeFilter)
  }, [sorted, tab, typeFilter])

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize))
  const pageEntries = filtered.slice((page - 1) * pageSize, page * pageSize)

  function handleEntryClick(notification: NotificationResponse) {
    if (!notification.read) markRead(notification.id)
    const link = getNotificationLink(notification)
    if (link) navigate(link)
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-100">Notifications</h1>
        {canMarkAllRead && unreadCount > 0 && (
          <button
            type="button"
            onClick={() => markAllRead()}
            className="text-sm font-medium text-brand-500 hover:underline"
          >
            Mark all read
          </button>
        )}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3">
        <div role="tablist" aria-label="Filter by read status" className="inline-flex rounded-lg border border-gray-700 p-1">
          {(['ALL', 'UNREAD'] as ReadTab[]).map((t) => (
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
              {t === 'ALL' ? 'All' : 'Unread'}
            </button>
          ))}
        </div>

        <select
          aria-label="Filter by type"
          value={typeFilter}
          onChange={(e) => {
            setTypeFilter(e.target.value as TypeFilter)
            setPage(1)
          }}
          className="rounded-lg border border-gray-700 bg-gray-900 px-3 py-2 text-sm text-gray-300"
        >
          <option value="ALL">Type: all</option>
          <option value="DONOR">Type: Donor</option>
          <option value="TRANSFER">Type: Transfer</option>
        </select>
      </div>

      <div className="mt-6 overflow-hidden rounded-lg border border-gray-800">
        {isLoading && (
          <div className="animate-pulse space-y-2 p-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-12 rounded bg-gray-900" />
            ))}
          </div>
        )}

        {isError && (
          <div className="px-4 py-10 text-center">
            <p className="text-red-400">Couldn&apos;t load notifications.</p>
            <button type="button" onClick={() => refetch()} className="mt-2 text-sm font-medium text-brand-500 hover:underline">
              Retry
            </button>
          </div>
        )}

        {!isLoading && !isError && sorted.length === 0 && (
          <p className="px-4 py-12 text-center text-gray-500">
            No notifications yet. You&apos;ll see updates here as they happen.
          </p>
        )}

        {!isLoading && !isError && sorted.length > 0 && filtered.length === 0 && (
          <p className="px-4 py-12 text-center text-gray-400">
            {tab === 'UNREAD' ? "You're all caught up." : 'No notifications match this filter.'}
          </p>
        )}

        {!isLoading && !isError && pageEntries.length > 0 && (
          <ul>
            {pageEntries.map((n) => (
              <li key={n.id} className="border-b border-gray-800 bg-gray-950 last:border-b-0">
                <NotificationEntry notification={n} onClick={() => handleEntryClick(n)} />
              </li>
            ))}
          </ul>
        )}
      </div>

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
    </div>
  )
}
