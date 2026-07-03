import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getAllNotifications,
  getNotificationsForDonor,
  getNotificationsForHospital,
  markAllReadForDonor,
  markAllReadForHospital,
  markNotificationRead,
} from '@/api/notifications'
import { getMyDonorProfile } from '@/api/donors'
import { useAuth } from '@/auth/AuthContext'

// No websocket/push exists anywhere in the backend — this is genuinely
// polling-based near-real-time, not live delivery. 30s balances freshness
// against hammering a component that reopens frequently.
const POLL_INTERVAL_MS = 30_000

export function useNotifications() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const donorProfileQuery = useQuery({
    queryKey: ['donor', 'me'],
    queryFn: getMyDonorProfile,
    enabled: user?.role === 'DONOR',
    retry: false,
  })

  const scopeKind = user?.role === 'ADMIN' ? 'admin' : user?.role === 'DONOR' ? 'donor' : 'hospital'
  const scopeId = scopeKind === 'donor' ? donorProfileQuery.data?.id : user?.hospitalId

  const notificationsQuery = useQuery({
    queryKey: ['notifications', scopeKind, scopeId],
    queryFn: () => {
      if (scopeKind === 'admin') return getAllNotifications()
      if (scopeKind === 'donor') return getNotificationsForDonor(scopeId as number)
      return getNotificationsForHospital(scopeId as number)
    },
    enabled: scopeKind === 'admin' || scopeId != null,
    refetchInterval: POLL_INTERVAL_MS,
  })

  const markReadMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const markAllReadMutation = useMutation({
    mutationFn: () => {
      if (scopeKind === 'donor') return markAllReadForDonor(scopeId as number)
      if (scopeKind === 'hospital') return markAllReadForHospital(scopeId as number)
      return Promise.resolve()
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const notifications = notificationsQuery.data ?? []
  const unreadCount = notifications.filter((n) => !n.read).length

  return {
    notifications,
    unreadCount,
    isLoading: notificationsQuery.isLoading,
    isError: notificationsQuery.isError,
    refetch: notificationsQuery.refetch,
    markRead: markReadMutation.mutate,
    // Admins have no single owning hospital/donor to bulk-mark against today
    // (they'd need to pick one), so "mark all read" is hidden for that role.
    canMarkAllRead: scopeKind !== 'admin',
    markAllRead: markAllReadMutation.mutate,
  }
}
