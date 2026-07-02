import { useRef } from 'react'
import { BLOOD_GROUPS, type BloodGroup } from '@/api/types'

interface BloodGroupChipsProps {
  value: BloodGroup | null
  onChange: (value: BloodGroup) => void
  label: string
}

export function BloodGroupChips({ value, onChange, label }: BloodGroupChipsProps) {
  const chipRefs = useRef<Record<string, HTMLButtonElement | null>>({})

  function handleKeyDown(e: React.KeyboardEvent, index: number) {
    let nextIndex: number | null = null
    if (e.key === 'ArrowRight' || e.key === 'ArrowDown') nextIndex = (index + 1) % BLOOD_GROUPS.length
    if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') nextIndex = (index - 1 + BLOOD_GROUPS.length) % BLOOD_GROUPS.length
    if (nextIndex !== null) {
      e.preventDefault()
      const nextGroup = BLOOD_GROUPS[nextIndex]
      onChange(nextGroup)
      chipRefs.current[nextGroup]?.focus()
    }
  }

  return (
    <div role="radiogroup" aria-label={label} className="flex flex-wrap gap-2 sm:flex-nowrap sm:overflow-x-auto">
      {BLOOD_GROUPS.map((group, index) => {
        const selected = value === group
        return (
          <button
            key={group}
            ref={(el) => {
              chipRefs.current[group] = el
            }}
            type="button"
            role="radio"
            aria-checked={selected}
            tabIndex={selected || (value === null && index === 0) ? 0 : -1}
            onKeyDown={(e) => handleKeyDown(e, index)}
            onClick={() => onChange(group)}
            className={`shrink-0 rounded-full px-5 py-3 text-base font-semibold transition-colors sm:px-4 sm:py-2 sm:text-sm ${
              selected
                ? 'bg-white text-gray-950'
                : 'border border-gray-700 text-gray-200 hover:border-gray-500'
            }`}
          >
            {group}
          </button>
        )
      })}
    </div>
  )
}
