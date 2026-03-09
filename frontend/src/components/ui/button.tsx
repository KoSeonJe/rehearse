import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Spinner } from '@/components/ui/spinner'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'cta'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  fullWidth?: boolean
  loading?: boolean
  children: ReactNode
}

const variantStyles: Record<ButtonVariant, string> = {
  primary: [
    'bg-slate-900 text-white',
    'hover:bg-slate-800 active:bg-slate-950',
    'disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed',
    'px-6 py-3 rounded-md',
  ].join(' '),
  secondary: [
    'bg-white text-gray-700 border border-gray-300',
    'hover:bg-gray-50',
    'disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200 disabled:cursor-not-allowed',
    'px-6 py-3 rounded-md',
  ].join(' '),
  ghost: [
    'bg-transparent text-gray-600',
    'hover:bg-gray-100',
    'disabled:text-gray-400 disabled:cursor-not-allowed',
    'px-4 py-2 rounded-md',
  ].join(' '),
  cta: [
    'bg-slate-900 text-white',
    'hover:bg-slate-800 active:bg-slate-950',
    'disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed',
    'px-8 py-4 text-lg rounded-lg',
  ].join(' '),
}

export const Button = ({
  variant = 'primary',
  fullWidth = false,
  loading = false,
  disabled,
  children,
  className = '',
  ...rest
}: ButtonProps) => {
  const isDisabled = disabled || loading

  return (
    <button
      className={[
        'inline-flex items-center justify-center font-medium transition-colors duration-150',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-500 focus-visible:ring-offset-2',
        variantStyles[variant],
        fullWidth ? 'w-full' : '',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      disabled={isDisabled}
      aria-disabled={isDisabled || undefined}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && (
        <Spinner
          className={`h-5 w-5 mr-2 ${variant === 'ghost' ? 'border-gray-600' : 'border-white'}`}
        />
      )}
      {children}
    </button>
  )
}
