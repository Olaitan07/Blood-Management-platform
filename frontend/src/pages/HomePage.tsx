import { Navigate } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
import { PlaceholderPage } from './PlaceholderPage'

export function HomePage() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN') {
    return <Navigate to="/admin/users" replace />
  }

  if (user?.role === 'DONOR') {
    return <Navigate to="/donor" replace />
  }

  if (user?.role === 'OFFICER') {
    return <Navigate to="/inventory" replace />
  }

  if (user?.role === 'CLINICIAN') {
    return <Navigate to="/search" replace />
  }

  return (
    <PlaceholderPage
      title={`Welcome, ${user?.name ?? ''}`}
      description="This portal hasn't been built yet — it's coming in a later module."
    />
  )
}
