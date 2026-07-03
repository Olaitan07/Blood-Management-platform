import { forwardRef, useId } from 'react'

interface NumberStepperProps {
  label: string
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  error?: string
}

export const NumberStepper = forwardRef<HTMLInputElement, NumberStepperProps>(
  ({ label, value, onChange, min = 0, max, error }, ref) => {
    const fieldId = useId()
    const errorId = `${fieldId}-error`

    function clamp(next: number) {
      const bounded = Number.isFinite(next) ? Math.max(min, next) : min
      onChange(max !== undefined ? Math.min(max, bounded) : bounded)
    }

    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={fieldId} className="text-sm font-medium text-gray-300">
          {label}
        </label>
        <div className="flex items-center gap-2">
          <button
            type="button"
            aria-label={`Decrease ${label.toLowerCase()}`}
            onClick={() => clamp(value - 1)}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-gray-700 text-lg text-gray-200 hover:bg-gray-800"
          >
            −
          </button>
          <input
            ref={ref}
            id={fieldId}
            type="number"
            inputMode="numeric"
            min={min}
            max={max}
            value={value}
            aria-invalid={!!error}
            aria-describedby={error ? errorId : undefined}
            onChange={(e) => clamp(Number(e.target.value))}
            className={`w-full rounded-lg border bg-gray-900 px-3 py-2.5 text-center text-gray-100 focus:outline-none focus:ring-2 focus:ring-brand-500 ${
              error ? 'border-red-600' : 'border-gray-700'
            }`}
          />
          <button
            type="button"
            aria-label={`Increase ${label.toLowerCase()}`}
            onClick={() => clamp(value + 1)}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-gray-700 text-lg text-gray-200 hover:bg-gray-800"
          >
            +
          </button>
        </div>
        {error && (
          <p id={errorId} role="alert" className="text-sm text-red-400">
            {error}
          </p>
        )}
      </div>
    )
  },
)
NumberStepper.displayName = 'NumberStepper'
