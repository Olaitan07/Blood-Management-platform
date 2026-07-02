type BadgeTone = 'neutral' | 'warning' | 'success' | 'danger' | 'info'

const TONE_STYLES: Record<BadgeTone, string> = {
  neutral: 'bg-gray-800 text-gray-300',
  warning: 'bg-amber-950 text-amber-400',
  success: 'bg-green-950 text-green-400',
  danger: 'bg-red-950 text-red-400',
  info: 'bg-blue-950 text-blue-400',
}

// Central place to map every status string used across modules (auth,
// transfer, notification, inventory) to a display label + tone, so each
// screen doesn't reinvent its own badge coloring. `icon` overrides the
// default dot when the distinction is safety-relevant (e.g. inventory
// expiry) and shouldn't rely on color alone.
const STATUS_MAP: Record<string, { label: string; tone: BadgeTone; icon?: string }> = {
  PENDING_APPROVAL: { label: 'Pending', tone: 'warning' },
  ACTIVE: { label: 'Active', tone: 'success' },
  SUSPENDED: { label: 'Suspended', tone: 'danger' },
  INACTIVE: { label: 'Inactive', tone: 'neutral' },
  AVAILABLE: { label: 'Available', tone: 'success', icon: '✓' },
  EXPIRING_SOON: { label: 'Expiring', tone: 'warning', icon: '⚠' },
  EXPIRED: { label: 'Expired', tone: 'danger', icon: '⛔' },
  NO_STOCK: { label: 'No stock', tone: 'neutral', icon: '—' },
}

export function StatusBadge({ status }: { status: string }) {
  const entry = STATUS_MAP[status] ?? { label: status, tone: 'neutral' as const }
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${TONE_STYLES[entry.tone]}`}
    >
      {entry.icon ? (
        <span aria-hidden="true">{entry.icon}</span>
      ) : (
        <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-current" />
      )}
      {entry.label}
    </span>
  )
}
