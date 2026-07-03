import { formatRelativeTime } from '@/lib/dateFormat'
import type { NotificationResponse } from '@/api/types'

// Shared between the bell dropdown and the full Notification Center so both
// surfaces render "the same data at different lengths," not two designs.
export function NotificationEntry({
  notification,
  onClick,
}: {
  notification: NotificationResponse
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-start gap-2 px-4 py-3 text-left hover:bg-gray-800"
    >
      {!notification.read && (
        <span aria-hidden="true" className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand-500" />
      )}
      <span className={notification.read ? 'ml-4' : ''}>
        <span className="sr-only">{notification.read ? '(read) ' : '(unread) '}</span>
        <p className="text-sm text-gray-100">{notification.message}</p>
        <p className="mt-0.5 text-xs text-gray-500">{formatRelativeTime(notification.sentAt)}</p>
      </span>
    </button>
  )
}
