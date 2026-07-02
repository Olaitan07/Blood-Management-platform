import { Route, Routes } from 'react-router-dom'
import { AppShell } from '@/components/AppShell'
import { ProtectedRoute, RoleRoute } from '@/auth/ProtectedRoute'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { RegistrationSubmittedPage } from '@/pages/auth/RegistrationSubmittedPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { HomePage } from '@/pages/HomePage'
import { UserManagementPage } from '@/pages/admin/UserManagementPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/registration-submitted" element={<RegistrationSubmittedPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<HomePage />} />

          <Route element={<RoleRoute allow={['ADMIN']} />}>
            <Route path="/admin/users" element={<UserManagementPage />} />
          </Route>
        </Route>
      </Route>

      <Route
        path="*"
        element={
          <PlaceholderPage title="Page not found" description="That page doesn't exist." />
        }
      />
    </Routes>
  )
}
