import { Link } from 'react-router-dom'
import { AuthCard } from '@/components/AuthCard'

// No password-reset endpoint exists on the backend today — this is a
// deliberate stub so the "Forgot password?" link on the login screen
// doesn't dead-end, not a built feature.
export function ForgotPasswordPage() {
  return (
    <AuthCard>
      <h1 className="text-xl font-semibold text-gray-100">Password reset</h1>
      <p className="mt-2 text-sm text-gray-400">
        Password reset isn&apos;t available yet. Contact your hospital administrator to have your
        password reset manually.
      </p>
      <Link to="/login" className="mt-6 inline-block text-sm text-brand-500 hover:underline">
        ← Back to sign in
      </Link>
    </AuthCard>
  )
}
