import { Route, Routes } from 'react-router-dom'
import { AppShell } from '@/components/AppShell'
import { ProtectedRoute, RoleRoute } from '@/auth/ProtectedRoute'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { RegistrationSubmittedPage } from '@/pages/auth/RegistrationSubmittedPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { HomePage } from '@/pages/HomePage'
import { UserManagementPage } from '@/pages/admin/UserManagementPage'
import { HospitalManagementPage } from '@/pages/admin/HospitalManagementPage'
import { DonorProfilePage } from '@/pages/donor/DonorProfilePage'
import { DonationHistoryPage } from '@/pages/donor/DonationHistoryPage'
import { InventoryListPage } from '@/pages/officer/InventoryListPage'
import { InventoryAuditPage } from '@/pages/officer/InventoryAuditPage'
import { SearchPage } from '@/pages/clinician/SearchPage'
import { NewTransferPlaceholderPage } from '@/pages/clinician/NewTransferPlaceholderPage'
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
            <Route path="/admin/hospitals" element={<HospitalManagementPage />} />
          </Route>

          <Route element={<RoleRoute allow={['DONOR']} />}>
            <Route path="/donor" element={<DonorProfilePage />} />
            <Route path="/donor/history" element={<DonationHistoryPage />} />
          </Route>

          <Route element={<RoleRoute allow={['OFFICER']} />}>
            <Route path="/inventory" element={<InventoryListPage />} />
            <Route path="/inventory/:id/audit" element={<InventoryAuditPage />} />
          </Route>

          <Route element={<RoleRoute allow={['CLINICIAN']} />}>
            <Route path="/search" element={<SearchPage />} />
            <Route path="/transfers/new" element={<NewTransferPlaceholderPage />} />
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
