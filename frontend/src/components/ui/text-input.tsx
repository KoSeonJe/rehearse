import type { InputHTMLAttributes } from 'react'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

interface TextInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string
  error?: string
}

export const TextInput = ({
  label,
  error,
  id,
  disabled,
  className = '',
  ...rest
}: TextInputProps) => {
  const inputId = id || label?.replace(/\s+/g, '-').toLowerCase()
  const errorId = inputId ? `${inputId}-error` : undefined

  return (
    <div className={className}>
      {label && (
        <Label
          htmlFor={inputId}
          className="mb-1.5 block text-sm font-medium text-text-primary"
        >
          {label}
        </Label>
      )}
      <Input
        type="text"
        id={inputId}
        disabled={disabled}
        aria-required={rest.required || undefined}
        aria-invalid={error ? true : undefined}
        aria-describedby={error && errorId ? errorId : undefined}
        className={cn(
          'w-full rounded-button border bg-white px-4 py-3 text-base text-text-primary h-auto',
          'placeholder:text-text-tertiary',
          'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-offset-0',
          'disabled:cursor-not-allowed disabled:bg-background disabled:text-text-tertiary',
          'transition-colors duration-150',
          error
            ? 'border-error focus-visible:border-error focus-visible:ring-error'
            : 'border-border focus-visible:border-text-primary focus-visible:ring-text-primary',
        )}
        {...rest}
      />
      {error && (
        <p id={errorId} className="mt-1 text-sm text-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
