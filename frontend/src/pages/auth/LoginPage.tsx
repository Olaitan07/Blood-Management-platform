import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthCard } from '@/components/AuthCard'
import { Button } from '@/components/Button'
import { TextField } from '@/components/TextField'
import { ApiError } from '@/api/client'
import { useAuth } from '@/auth/AuthContext'
import type { Role } from '@/api/types'

interface LoginError {
  kind: 'invalid' | 'locked' | 'inactive' | 'network' | 'unknown'
  message: string
}

// Never let the UI imply which of email/password was wrong (login errors
// are generic by design) — but lockout and inactive-account states are
// different failure classes and get their own visual treatment per spec.
function categorize(err: ApiError): LoginError {
  if (err.status === 0) return { kind: 'network', message: err.message }
  if (err.status === 403 && /locked/i.test(err.message)) {
    return { kind: 'locked', message: err.message }
  }
  if (err.status === 403 && /not active/i.test(err.message)) {
    return { kind: 'inactive', message: err.message }
  }
  if (err.status === 401) return { kind: 'invalid', message: 'Invalid email or password.' }
  return { kind: 'unknown', message: err.message }
}

const HOME_BY_ROLE: Record<Role, string> = {
  ADMIN: '/admin/users',
  OFFICER: '/',
  CLINICIAN: '/',
  DONOR: '/',
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<LoginError | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      const user = await login({ email, password })
      const from = (location.state as { from?: Location })?.from?.pathname
      navigate(from ?? HOME_BY_ROLE[user.role], { replace: true })
    } catch (err) {
      setError(categorize(err as ApiError))
      setPassword('')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthCard>
      <h1 className="text-2xl font-semibold text-gray-100">Sign in</h1>

      {error && <ErrorBanner error={error} />}

      <form className="mt-6 flex flex-col gap-4" onSubmit={onSubmit} noValidate>
        <TextField
          label="Email"
          type="email"
          autoComplete="username"
          placeholder="name@hospital.org"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          invalid={error?.kind === 'invalid'}
          required
        />

        <div className="relative">
          <TextField
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            invalid={error?.kind === 'invalid'}
            required
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            className="absolute right-3 top-9 text-gray-500 hover:text-gray-300"
          >
            {showPassword ? '🙈' : '👁️'}
          </button>
        </div>

        <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
          Sign in
        </Button>
      </form>

      <div className="mt-4 flex items-center justify-between text-sm">
        <Link to="/forgot-password" className="text-brand-500 hover:underline">
          Forgot password?
        </Link>
        <Link to="/register" className="text-brand-500 hover:underline">
          Register
        </Link>
      </div>
    </AuthCard>
  )
}

function ErrorBanner({ error }: { error: LoginError }) {
  const styles: Record<LoginError['kind'], string> = {
    invalid: 'border-red-700 bg-red-950 text-red-300',
    locked: 'border-amber-700 bg-amber-950 text-amber-300',
    inactive: 'border-blue-700 bg-blue-950 text-blue-300',
    network: 'border-gray-700 bg-gray-800 text-gray-300',
    unknown: 'border-red-700 bg-red-950 text-red-300',
  }
  const icons: Record<LoginError['kind'], string> = {
    invalid: '⚠️',
    locked: '🔒',
    inactive: 'ℹ️',
    network: '📡',
    unknown: '⚠️',
  }

  return (
    <div role="alert" className={`mt-4 flex items-center gap-2 rounded-lg border px-3 py-2.5 text-sm ${styles[error.kind]}`}>
      <span aria-hidden="true">{icons[error.kind]}</span>
      {error.message}
    </div>
  )
}
