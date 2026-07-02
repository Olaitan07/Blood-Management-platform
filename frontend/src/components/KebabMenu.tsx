import { useEffect, useRef, useState } from 'react'

export interface KebabMenuItem {
  label: string
  onClick: () => void
  disabled?: boolean
  tone?: 'default' | 'danger'
}

export function KebabMenu({ items, label }: { items: KebabMenuItem[]; label: string }) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onClickOutside)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  return (
    <div className="relative inline-block text-left" ref={containerRef}>
      <button
        type="button"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="rounded p-1.5 text-gray-400 hover:bg-gray-800 hover:text-gray-100"
      >
        ⋮
      </button>
      {open && (
        <div
          role="menu"
          className="absolute right-0 z-20 mt-1 w-44 overflow-hidden rounded-lg border border-gray-800 bg-gray-900 py-1 shadow-xl"
        >
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              disabled={item.disabled}
              onClick={() => {
                setOpen(false)
                item.onClick()
              }}
              className={`block w-full px-3 py-2 text-left text-sm disabled:cursor-not-allowed disabled:opacity-40 ${
                item.tone === 'danger'
                  ? 'text-red-400 hover:bg-red-950'
                  : 'text-gray-200 hover:bg-gray-800'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
