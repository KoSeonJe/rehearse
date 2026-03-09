import { type KeyboardEvent } from 'react'

interface SelectionCardProps {
  value: string
  label: string
  description?: string
  selected: boolean
  disabled?: boolean
  onSelect: (value: string) => void
}

export const SelectionCard = ({
  value,
  label,
  description,
  selected,
  disabled = false,
  onSelect,
}: SelectionCardProps) => {
  const handleKeyDown = (e: KeyboardEvent<HTMLButtonElement>) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      if (!disabled) {
        onSelect(value)
      }
    }
  }

  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={disabled}
      onClick={() => onSelect(value)}
      onKeyDown={handleKeyDown}
      className={[
        'cursor-pointer rounded-lg border p-4 text-left transition-all duration-150',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-500 focus-visible:ring-offset-2',
        selected
          ? 'border-slate-900 bg-slate-50 ring-1 ring-slate-900'
          : 'border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50',
        disabled
          ? 'cursor-not-allowed border-gray-100 bg-gray-50 text-gray-400'
          : '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <span className="block text-base font-medium text-gray-900">
        {label}
      </span>
      {description && (
        <span className="mt-1 block text-sm text-gray-500">{description}</span>
      )}
    </button>
  )
}
