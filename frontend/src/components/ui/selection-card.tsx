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
        'cursor-pointer rounded-card border p-4 text-left transition-all duration-150',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2',
        selected
          ? 'border-accent bg-accent-light'
          : 'border-border bg-white hover:border-text-tertiary hover:shadow-sm',
        disabled
          ? 'cursor-not-allowed border-border bg-background text-text-tertiary'
          : '',
      ]
        .filter(Boolean)
        .join(' ')}
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
