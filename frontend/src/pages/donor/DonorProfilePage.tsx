import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMyDonorProfile, registerDonor } from '@/api/donors'
import { ApiError } from '@/api/client'
import { useAuth } from '@/auth/AuthContext'
import { Button } from '@/components/Button'
import { TextField } from '@/components/TextField'
import { Select } from '@/components/Select'
import { useToast } from '@/components/Toast'
import { BLOOD_GROUPS, type BloodGroup, type DonorRequest, type DonorResponse } from '@/api/types'

export function DonorProfilePage() {
  const profileQuery = useQuery({
    queryKey: ['donor', 'me'],
    queryFn: getMyDonorProfile,
    retry: false,
  })

  if (profileQuery.isLoading) {
    return <ProfileSkeleton />
  }

  if (profileQuery.isError) {
    const err = profileQuery.error as ApiError
    if (err.status === 404) {
      return <CompleteProfileForm />
    }
    return (
      <div className="mx-auto max-w-xl px-6 py-16 text-center">
        <p className="text-gray-300">Couldn&apos;t load your profile.</p>
        <button
          type="button"
          onClick={() => profileQuery.refetch()}
          className="mt-3 text-sm font-medium text-brand-500 hover:underline"
        >
          Retry
        </button>
      </div>
    )
  }

  const donor = profileQuery.data!
  return <ProfileView donor={donor} />
}

function ProfileSkeleton() {
  return (
    <div className="mx-auto max-w-xl px-6 py-10">
      <div className="animate-pulse overflow-hidden rounded-2xl border border-gray-800">
        <div className="h-16 bg-gray-800" />
        <div className="space-y-3 bg-gray-900 p-6">
          <div className="h-5 w-1/2 rounded bg-gray-800" />
          <div className="h-4 w-full rounded bg-gray-800" />
          <div className="h-4 w-full rounded bg-gray-800" />
          <div className="h-4 w-full rounded bg-gray-800" />
        </div>
      </div>
    </div>
  )
}

function ProfileView({ donor }: { donor: DonorResponse }) {
  const { user } = useAuth()
  const eligible = donor.eligibilityStatus === 'ELIGIBLE'
  const isFirstTime = donor.lastDonationDate === null

  return (
    <div className="mx-auto max-w-xl px-6 py-10">
      <div className="overflow-hidden rounded-2xl border border-gray-800">
        <div
          className={`px-6 py-4 ${eligible ? 'bg-green-950 text-green-300' : 'bg-amber-950 text-amber-300'}`}
        >
          <p className="flex items-center gap-2 text-sm font-semibold tracking-wide">
            <span aria-hidden="true">{eligible ? '●' : '●'}</span>
            {eligible ? 'ELIGIBLE' : 'NOT ELIGIBLE YET'}
          </p>
          <p className="mt-1 text-sm">
            {eligible
              ? isFirstTime
                ? "You're eligible to donate."
                : 'You can donate now.'
              : `You can donate again starting ${formatDate(donor.eligibleFrom)}.`}
          </p>
        </div>

        <div className="space-y-4 bg-gray-900 p-6">
          <h1 className="text-lg font-semibold text-gray-100">{donor.fullName}</h1>
          <dl className="space-y-2 text-sm">
            <Row label="Blood group" value={donor.bloodGroup} />
            <Row label="Phone" value={donor.phone} />
            <Row label="Email" value={user?.email ?? ''} />
          </dl>

          <Link
            to="/donor/history"
            className="mt-2 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-gray-700 px-4 py-2.5 text-sm font-semibold text-gray-100 hover:bg-gray-800"
          >
            View donation history <span aria-hidden="true">→</span>
          </Link>
        </div>
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between border-t border-gray-800 pt-2 first:border-t-0 first:pt-0">
      <dt className="text-gray-400">{label}</dt>
      <dd className="font-medium text-gray-100">{value}</dd>
    </div>
  )
}

function formatDate(isoDate: string | null): string {
  if (!isoDate) return ''
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

interface FieldErrors {
  fullName?: string
  phone?: string
  bloodGroup?: string
  general?: string
}

function validate(form: DonorRequest): FieldErrors {
  const errors: FieldErrors = {}
  if (!form.fullName.trim()) errors.fullName = 'Enter your full name'
  if (!form.phone.trim()) {
    errors.phone = 'Enter a valid phone number'
  } else if (!/^\+?[0-9]{7,15}$/.test(form.phone.trim())) {
    errors.phone = 'Enter a valid phone number'
  }
  if (!form.bloodGroup) errors.bloodGroup = 'Select your blood group'
  return errors
}

function mapApiError(err: ApiError): FieldErrors {
  if (err.fieldErrors) return { ...err.fieldErrors }
  if (err.status === 409) return { phone: err.message }
  return { general: err.message }
}

function CompleteProfileForm() {
  const { user } = useAuth()
  const { show } = useToast()
  const queryClient = useQueryClient()

  const [form, setForm] = useState<DonorRequest>({
    fullName: user?.name ?? '',
    phone: '',
    bloodGroup: '' as BloodGroup,
  })
  const [clientErrors, setClientErrors] = useState<FieldErrors>({})

  const mutation = useMutation({
    mutationFn: registerDonor,
    onSuccess: () => {
      show('Welcome — your donor profile is ready.', 'success')
      queryClient.invalidateQueries({ queryKey: ['donor', 'me'] })
    },
  })

  const errors = { ...(mutation.error ? mapApiError(mutation.error as ApiError) : {}), ...clientErrors }

  function update<K extends keyof DonorRequest>(key: K, value: DonorRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const validation = validate(form)
    if (Object.keys(validation).length > 0) {
      setClientErrors(validation)
      return
    }
    setClientErrors({})
    mutation.mutate(form)
  }

  return (
    <div className="flex min-h-[calc(100vh-57px)] items-center justify-center px-4 py-10">
      <div className="w-full max-w-md rounded-2xl border border-gray-800 bg-gray-900 p-8 shadow-2xl">
        <h1 className="text-2xl font-semibold text-gray-100">Become a donor</h1>
        <p className="mt-1 text-sm text-gray-400">Takes less than a minute.</p>

        {errors.general && (
          <div role="alert" className="mt-4 rounded-lg border border-red-700 bg-red-950 px-3 py-2.5 text-sm text-red-300">
            {errors.general}
          </div>
        )}

        <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          <TextField
            label="Full name"
            placeholder="Jane Doe"
            value={form.fullName}
            onChange={(e) => update('fullName', e.target.value)}
            error={errors.fullName}
            required
          />
          <TextField
            label="Phone"
            placeholder="(555) 010-1234"
            value={form.phone}
            onChange={(e) => update('phone', e.target.value)}
            error={errors.phone}
            required
          />
          <Select
            label="Blood group"
            value={form.bloodGroup}
            onChange={(e) => update('bloodGroup', e.target.value as BloodGroup)}
            error={errors.bloodGroup}
          >
            <option value="">Select your blood group</option>
            {BLOOD_GROUPS.map((bg) => (
              <option key={bg} value={bg}>
                {bg}
              </option>
            ))}
          </Select>

          <Button type="submit" isLoading={mutation.isPending} className="mt-2 w-full">
            Register
          </Button>
        </form>
      </div>
    </div>
  )
}
