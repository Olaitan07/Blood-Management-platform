import type { ButtonHTMLAttributes } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md'
  isLoading?: boolean
}

const VARIANT_STYLES: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary: 'bg-white text-gray-950 hover:bg-gray-200 disabled:bg-gray-400',
  secondary: 'border border-gray-700 text-gray-100 hover:bg-gray-800',
  ghost: 'text-gray-300 hover:bg-gray-800',
  danger: 'bg-red-600 text-white hover:bg-red-700 disabled:bg-red-900',
}

const SIZE_STYLES: Record<NonNullable<ButtonProps['size']>, string> = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2.5 text-sm',
}

export function Button({
  variant = 'primary',
  size = 'md',
  isLoading,
  disabled,
  className = '',
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors disabled:cursor-not-allowed ${SIZE_STYLES[size]} ${VARIANT_STYLES[variant]} ${className}`}
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      {...rest}
    >
      {isLoading && (
        <span
          className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
          aria-hidden="true"
        />
      )}
      {children}
    </button>
  )
}
