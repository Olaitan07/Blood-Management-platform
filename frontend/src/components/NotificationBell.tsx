import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useNotifications } from '@/hooks/useNotifications'
import { getNotificationLink } from '@/lib/notificationLinks'
import { NotificationEntry } from '@/components/NotificationEntry'
import type { NotificationResponse } from '@/api/types'

const PREVIEW_LIMIT = 8

export function NotificationBell() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const { notifications, unreadCount, isLoading, isError, refetch, markRead, canMarkAllRead, markAllRead } =
    useNotifications()

  useEffect(() => {
    if (!open) return
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onClickOutside)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  function handleEntryClick(notification: NotificationResponse) {
    if (!notification.read) markRead(notification.id)
    setOpen(false)
    const link = getNotificationLink(notification)
    if (link) navigate(link)
  }

  const preview = [...notifications]
    .sort((a, b) => b.sentAt.localeCompare(a.sentAt))
    .slice(0, PREVIEW_LIMIT)

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="relative rounded-lg p-2 text-gray-300 hover:bg-gray-800 hover:text-gray-100"
      >
        <span aria-hidden="true" className="text-lg">
          🔔
        </span>
        {unreadCount > 0 && (
          <span
            aria-hidden="true"
            className="absolute -right-0.5 -top-0.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white"
          >
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Notifications"
          className="absolute right-0 z-30 mt-2 w-80 overflow-hidden rounded-lg border border-gray-800 bg-gray-900 shadow-xl sm:w-96"
        >
          <div className="flex items-center justify-between border-b border-gray-800 px-4 py-3">
            <h2 className="text-sm font-semibold text-gray-100">Notifications</h2>
            {canMarkAllRead && unreadCount > 0 && (
              <button
                type="button"
                onClick={() => markAllRead()}
                className="text-xs font-medium text-brand-500 hover:underline"
              >
                Mark all read
              </button>
            )}
          </div>

          {isLoading && (
            <div className="animate-pulse space-y-2 p-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-10 rounded bg-gray-800" />
              ))}
            </div>
          )}

          {isError && (
            <div className="flex items-center justify-between px-4 py-4 text-sm">
              <span className="text-red-400">Couldn&apos;t load notifications.</span>
              <button
                type="button"
                aria-label="Retry loading notifications"
                onClick={() => refetch()}
                className="text-brand-500 hover:underline"
              >
                Retry
              </button>
            </div>
          )}

          {!isLoading && !isError && preview.length === 0 && (
            <p className="px-4 py-6 text-center text-sm text-gray-500">No notifications yet.</p>
          )}

          {!isLoading && !isError && preview.length > 0 && (
            <>
              <ul className="max-h-96 overflow-y-auto">
                {preview.map((n) => (
                  <li key={n.id} className="border-b border-gray-800 last:border-b-0">
                    <NotificationEntry notification={n} onClick={() => handleEntryClick(n)} />
                  </li>
                ))}
              </ul>
              <button
                type="button"
                onClick={() => {
                  setOpen(false)
                  navigate('/notifications')
                }}
                className="block w-full border-t border-gray-800 px-4 py-2.5 text-center text-sm font-medium text-brand-500 hover:bg-gray-800"
              >
                View all notifications
              </button>
            </>
          )}
        </div>
      )}
    </div>
  )
}
