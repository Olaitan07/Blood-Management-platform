// Consolidated from ~10 near-identical copies scattered across page
// components — one implementation, one test suite, instead of N untested
// duplicates that could silently drift apart.

export function formatDate(isoDate: string, month: 'short' | 'long' = 'short'): string {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString('en-US', { month, day: 'numeric', year: 'numeric' })
}

export function formatDateTime(isoDateTime: string): string {
  return new Date(isoDateTime).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export function formatRelativeTime(isoDateTime: string): string {
  const diffMs = Date.now() - new Date(isoDateTime).getTime()
  const minutes = Math.round(diffMs / 60_000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} hr${hours === 1 ? '' : 's'} ago`
  const days = Math.round(hours / 24)
  if (days === 1) return 'yesterday'
  return `${days} days ago`
}
