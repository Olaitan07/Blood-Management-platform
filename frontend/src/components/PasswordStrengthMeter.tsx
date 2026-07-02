// Reflects the actual backend rule (@StrongPassword): >= 8 chars AND at
// least one digit — nothing else is required, so "meets requirements" is
// reachable at the medium segment, not just the top one.
function scorePassword(password: string): 0 | 1 | 2 | 3 {
  if (password.length === 0) return 0
  const hasDigit = /\d/.test(password)
  const meetsMinimum = password.length >= 8 && hasDigit
  if (!meetsMinimum) return 1
  const hasVariety = /[A-Z]/.test(password) && /[^A-Za-z0-9]/.test(password)
  if (password.length >= 12 && hasVariety) return 3
  return 2
}

const LABELS = ['', 'Too weak', 'Meets requirements', 'Strong'] as const
const BAR_COLORS = ['bg-gray-800', 'bg-red-600', 'bg-green-600', 'bg-green-500'] as const

export function PasswordStrengthMeter({ password }: { password: string }) {
  const score = scorePassword(password)

  return (
    <div aria-hidden={password.length === 0} className="flex flex-col gap-1.5">
      <div className="flex gap-1.5">
        {[1, 2, 3].map((segment) => (
          <div
            key={segment}
            className={`h-1.5 flex-1 rounded-full transition-colors ${
              segment <= score ? BAR_COLORS[score] : 'bg-gray-800'
            }`}
          />
        ))}
      </div>
      {password.length > 0 && (
        <p className="text-xs text-gray-500" role="status">
          {LABELS[score]}
        </p>
      )}
    </div>
  )
}
