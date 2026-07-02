import { forwardRef, useId, type SelectHTMLAttributes } from 'react'

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, id, className = '', children, ...rest }, ref) => {
    const generatedId = useId()
    const fieldId = id ?? generatedId
    const errorId = `${fieldId}-error`

    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={fieldId} className="text-sm font-medium text-gray-300">
          {label}
        </label>
        <select
          ref={ref}
          id={fieldId}
          aria-invalid={!!error}
          aria-describedby={error ? errorId : undefined}
          className={`rounded-lg border bg-gray-900 px-3 py-2.5 text-gray-100 focus:outline-none focus:ring-2 focus:ring-brand-500 ${
            error ? 'border-red-600' : 'border-gray-700'
          } ${className}`}
          {...rest}
        >
          {children}
        </select>
        {error && (
          <p id={errorId} role="alert" className="text-sm text-red-400">
            {error}
          </p>
        )}
      </div>
    )
  },
)
Select.displayName = 'Select'
