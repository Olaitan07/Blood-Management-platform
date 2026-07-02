import { useEffect, type ReactNode } from 'react'

interface DrawerProps {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}

export function Drawer({ open, title, onClose, children }: DrawerProps) {
  useEffect(() => {
    if (!open) return
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, onClose])

  return (
    <div
      className={`fixed inset-0 z-40 transition-opacity ${open ? 'pointer-events-auto opacity-100' : 'pointer-events-none opacity-0'}`}
      aria-hidden={!open}
    >
      <button
        type="button"
        aria-label="Close panel"
        className="absolute inset-0 bg-black/60"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="drawer-title"
        className={`absolute right-0 top-0 h-full w-full max-w-md transform border-l border-gray-800 bg-gray-900 shadow-xl transition-transform ${
          open ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        <div className="flex items-center justify-between border-b border-gray-800 px-6 py-4">
          <h2 id="drawer-title" className="text-lg font-semibold text-gray-100">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="rounded p-1 text-gray-400 hover:bg-gray-800 hover:text-gray-100"
          >
            ✕
          </button>
        </div>
        <div className="overflow-y-auto p-6">{children}</div>
      </div>
    </div>
  )
}
