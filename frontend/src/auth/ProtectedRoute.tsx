import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'
import type { Role } from '@/api/types'

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}

export function RoleRoute({ allow }: { allow: Role[] }) {
  const { user } = useAuth()

  if (!user || !allow.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
