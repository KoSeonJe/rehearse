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
          className="mb-1.5 block text-sm font-medium text-gray-700"
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
          'w-full rounded-md border bg-white px-4 py-3 text-base',
          'placeholder:text-gray-400',
          'focus:outline-none focus:ring-1',
          'disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400',
          'transition-colors duration-150',
          error
            ? 'border-red-500 focus:border-red-500 focus:ring-red-500'
            : 'border-gray-200 focus:border-slate-500 focus:ring-slate-500',
        ].join(' ')}
        {...rest}
      />
      {error && (
        <p id={errorId} className="mt-1 text-sm text-red-600" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
