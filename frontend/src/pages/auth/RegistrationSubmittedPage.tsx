import { Link, useLocation } from 'react-router-dom'
import { AuthCard } from '@/components/AuthCard'

interface LocationState {
  hospitalId?: number | null
}

export function RegistrationSubmittedPage() {
  const location = useLocation()
  const { hospitalId } = (location.state as LocationState | null) ?? {}

  return (
    <AuthCard>
      <div className="flex flex-col items-center text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-green-950">
          <span className="text-2xl text-green-400" aria-hidden="true">
            ✓
          </span>
        </div>
        <h1 className="mt-4 text-xl font-semibold text-gray-100">Registration submitted</h1>
        <p className="mt-2 text-sm text-gray-400">
          Your account is pending approval from an administrator
          {hospitalId ? ` at Hospital #${hospitalId}` : ''}. You&apos;ll receive an email once
          it&apos;s approved.
        </p>
        <Link
          to="/login"
          className="mt-6 inline-flex w-full items-center justify-center rounded-lg border border-gray-700 px-4 py-2.5 text-sm font-semibold text-gray-100 hover:bg-gray-800"
        >
          Back to sign in
        </Link>
      </div>
    </AuthCard>
  )
}
