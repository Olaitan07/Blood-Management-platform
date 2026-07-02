import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AuthCard } from '@/components/AuthCard'
import { Button } from '@/components/Button'
import { TextField } from '@/components/TextField'
import { Select } from '@/components/Select'
import { PasswordStrengthMeter } from '@/components/PasswordStrengthMeter'
import { register } from '@/api/auth'
import { listHospitals } from '@/api/hospitals'
import { ApiError } from '@/api/client'
import { ROLES_REQUIRING_HOSPITAL, type Role } from '@/api/types'

// Admin is deliberately not self-registerable here — admin accounts are
// seeded/promoted, not created through open registration. Donors register
// through this same auth flow (role=DONOR, no hospital, pending admin
// approval like any other role) rather than a separate mechanism.
const REGISTERABLE_ROLES: { value: Role; label: string }[] = [
  { value: 'CLINICIAN', label: 'Clinician' },
  { value: 'OFFICER', label: 'Blood Bank Officer' },
  { value: 'DONOR', label: 'Donor' },
]

interface FormState {
  name: string
  email: string
  password: string
  role: Role
  hospitalId: string
}

interface FieldErrors {
  name?: string
  email?: string
  password?: string
  role?: string
  hospitalId?: string
  general?: string
}

function validate(form: FormState): FieldErrors {
  const errors: FieldErrors = {}
  if (!form.name.trim()) errors.name = 'Name is required'
  if (!form.email.trim()) {
    errors.email = 'Email is required'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = 'Invalid email format'
  }
  if (!form.password) {
    errors.password = 'Password is required'
  } else if (form.password.length < 8 || !/\d/.test(form.password)) {
    errors.password = 'Password must be at least 8 characters and contain at least one digit'
  }
  if (ROLES_REQUIRING_HOSPITAL.includes(form.role) && !form.hospitalId) {
    errors.hospitalId = 'Please select a hospital'
  }
  return errors
}

// Maps the backend's errors map (keyed by field name, or by "registerRequest"
// for the class-level @HospitalRequiredForRole check) plus known plain-message
// exceptions (409 duplicate email, 400 unknown hospital) onto form fields.
function mapApiError(err: ApiError): FieldErrors {
  const mapped: FieldErrors = {}
  if (err.fieldErrors) {
    for (const [key, message] of Object.entries(err.fieldErrors)) {
      if (key === 'name' || key === 'email' || key === 'password') {
        mapped[key] = message
      } else {
        // "registerRequest" (object-level) or any other unmapped key.
        mapped.hospitalId = message
      }
    }
    return mapped
  }
  if (err.status === 409) {
    mapped.email = err.message
    return mapped
  }
  if (err.status === 400 && /hospital/i.test(err.message)) {
    mapped.hospitalId = err.message
    return mapped
  }
  mapped.general = err.message
  return mapped
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<FormState>({
    name: '',
    email: '',
    password: '',
    role: 'CLINICIAN',
    hospitalId: '',
  })
  const [errors, setErrors] = useState<FieldErrors>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const requiresHospital = useMemo(() => ROLES_REQUIRING_HOSPITAL.includes(form.role), [form.role])

  // Public — the register form is anonymous, so this hits the unauthenticated
  // GET /api/hospitals route (active hospitals only, the endpoint's default).
  const hospitalsQuery = useQuery({
    queryKey: ['public-hospitals'],
    queryFn: () => listHospitals(),
    enabled: requiresHospital,
  })

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const clientErrors = validate(form)
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors)
      return
    }
    setErrors({})
    setIsSubmitting(true)
    try {
      const user = await register({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role,
        hospitalId: requiresHospital ? Number(form.hospitalId) : null,
      })
      const hospitalName = hospitalsQuery.data?.find((h) => h.id === user.hospitalId)?.name
      navigate('/registration-submitted', { state: { hospitalName } })
    } catch (err) {
      setErrors(mapApiError(err as ApiError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthCard>
      <h1 className="text-2xl font-semibold text-gray-100">Create your account</h1>

      {errors.general && (
        <div role="alert" className="mt-4 rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
          {errors.general}
        </div>
      )}

      <form className="mt-6 flex flex-col gap-4" onSubmit={onSubmit} noValidate>
        <TextField
          label="Full name"
          placeholder="Jane Doe"
          value={form.name}
          onChange={(e) => update('name', e.target.value)}
          error={errors.name}
          autoComplete="name"
          required
        />

        <TextField
          label="Email"
          type="email"
          placeholder="name@hospital.org"
          value={form.email}
          onChange={(e) => update('email', e.target.value)}
          error={errors.email}
          autoComplete="username"
          required
        />

        <div>
          <TextField
            label="Password"
            type="password"
            placeholder="At least 8 characters"
            value={form.password}
            onChange={(e) => update('password', e.target.value)}
            error={errors.password}
            autoComplete="new-password"
            required
          />
          <div className="mt-2">
            <PasswordStrengthMeter password={form.password} />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Select
            label="Role"
            value={form.role}
            onChange={(e) => update('role', e.target.value as Role)}
          >
            {REGISTERABLE_ROLES.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </Select>

          {requiresHospital ? (
            <div className="flex flex-col gap-1.5">
              <Select
                label="Hospital"
                value={form.hospitalId}
                onChange={(e) => update('hospitalId', e.target.value)}
                error={errors.hospitalId}
                disabled={hospitalsQuery.isLoading || hospitalsQuery.isError}
              >
                <option value="">
                  {hospitalsQuery.isLoading ? 'Loading hospitals…' : 'Select a hospital'}
                </option>
                {hospitalsQuery.data?.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name} — {h.city}
                  </option>
                ))}
              </Select>
              {hospitalsQuery.isError && (
                <p className="text-sm text-red-400">
                  Couldn&apos;t load hospitals.{' '}
                  <button
                    type="button"
                    onClick={() => hospitalsQuery.refetch()}
                    className="underline"
                  >
                    Retry
                  </button>
                </p>
              )}
              {hospitalsQuery.isSuccess && hospitalsQuery.data.length === 0 && (
                <p className="text-sm text-gray-500">
                  No hospitals are registered yet — contact an administrator.
                </p>
              )}
            </div>
          ) : (
            <TextField label="Hospital" value="Not required" disabled hint="Not required for this role" />
          )}
        </div>

        <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
          Create account
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-gray-400">
        Already have an account?{' '}
        <Link to="/login" className="text-brand-500 hover:underline">
          Sign in
        </Link>
      </p>
    </AuthCard>
  )
}
