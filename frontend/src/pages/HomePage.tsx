import { Navigate } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
import { PlaceholderPage } from './PlaceholderPage'

export function HomePage() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN') {
    return <Navigate to="/admin/users" replace />
  }

  return (
    <PlaceholderPage
      title={`Welcome, ${user?.name ?? ''}`}
      description="This portal hasn't been built yet — it's coming in a later module."
    />
  )
}
