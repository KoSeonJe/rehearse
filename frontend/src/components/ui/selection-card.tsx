import { type KeyboardEvent } from 'react'
import { cn } from '@/lib/utils'

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
      className={cn(
        'cursor-pointer rounded-lg border p-4 text-left transition-colors duration-200 shadow-sm w-full',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-legacy focus-visible:ring-offset-2',
        selected
          ? 'border-violet-legacy bg-violet-legacy-light'
          : 'border-border bg-surface hover:border-text-tertiary hover:shadow-md hover:-translate-y-0.5',
        disabled
          ? 'cursor-not-allowed border-border bg-background text-text-tertiary'
          : '',
      )}
    >
      <span className="block text-base font-medium text-text-primary">
        {label}
      </span>
      {description && (
        <span className="mt-1 block text-sm text-text-secondary">{description}</span>
      )}
    </button>
  )
}
