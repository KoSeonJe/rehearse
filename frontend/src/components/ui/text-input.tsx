import type { InputHTMLAttributes } from 'react'

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
        <label
          htmlFor={inputId}
          className="mb-1.5 block text-sm font-medium text-text-primary"
        >
          {label}
        </label>
      )}
      <input
        type="text"
        id={inputId}
        disabled={disabled}
        aria-required={rest.required || undefined}
        aria-invalid={error ? true : undefined}
        aria-describedby={error && errorId ? errorId : undefined}
        className={[
          'w-full rounded-button border bg-white px-4 py-3 text-base text-text-primary',
          'placeholder:text-text-tertiary',
          'focus:outline-none focus:ring-1',
          'disabled:cursor-not-allowed disabled:bg-background disabled:text-text-tertiary',
          'transition-colors duration-150',
          error
            ? 'border-error focus:border-error focus:ring-error'
            : 'border-border focus:border-text-primary focus:ring-text-primary',
        ].join(' ')}
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
