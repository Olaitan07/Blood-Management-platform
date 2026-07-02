import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

type ToastVariant = 'success' | 'error' | 'info'

interface Toast {
  id: number
  variant: ToastVariant
  message: string
}

interface ToastContextValue {
  show: (message: string, variant?: ToastVariant) => void
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined)

let nextId = 1

const VARIANT_STYLES: Record<ToastVariant, string> = {
  success: 'border-green-700 bg-green-950 text-green-200',
  error: 'border-red-700 bg-red-950 text-red-200',
  info: 'border-gray-700 bg-gray-900 text-gray-200',
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const dismiss = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const show = useCallback(
    (message: string, variant: ToastVariant = 'info') => {
      const id = nextId++
      setToasts((prev) => [...prev, { id, variant, message }])
      setTimeout(() => dismiss(id), 5000)
    },
    [dismiss],
  )

  const value = useMemo(() => ({ show }), [show])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        role="region"
        aria-live="polite"
        aria-label="Notifications"
        className="fixed bottom-4 right-4 z-50 flex flex-col gap-2"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="status"
            className={`min-w-[260px] max-w-sm rounded-lg border px-4 py-3 text-sm shadow-lg ${VARIANT_STYLES[toast.variant]}`}
          >
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within a ToastProvider')
  return ctx
}
