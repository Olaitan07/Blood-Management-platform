import { forwardRef, useId, type InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: string
  /** Show the invalid (red) border without rendering an inline message —
   * for fields whose error is already surfaced elsewhere (e.g. a form-level banner). */
  invalid?: boolean
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(
  ({ label, error, hint, invalid, id, className = '', ...rest }, ref) => {
    const generatedId = useId()
    const fieldId = id ?? generatedId
    const errorId = `${fieldId}-error`
    const hintId = `${fieldId}-hint`
    const isInvalid = !!error || !!invalid

    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={fieldId} className="text-sm font-medium text-gray-300">
          {label}
        </label>
        <input
          ref={ref}
          id={fieldId}
          aria-invalid={isInvalid}
          aria-describedby={error ? errorId : hint ? hintId : undefined}
          className={`rounded-lg border bg-gray-900 px-3 py-2.5 text-gray-100 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-brand-500 ${
            isInvalid ? 'border-red-600' : 'border-gray-700'
          } ${className}`}
          {...rest}
        />
        {error && (
          <p id={errorId} role="alert" className="text-sm text-red-400">
            {error}
          </p>
        )}
        {!error && hint && (
          <p id={hintId} className="text-sm text-gray-500">
            {hint}
          </p>
        )}
      </div>
    )
  },
)
TextField.displayName = 'TextField'
