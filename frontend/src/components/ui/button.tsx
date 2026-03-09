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
    'bg-accent text-white',
    'hover:bg-accent-hover active:bg-accent-hover',
    'disabled:bg-border disabled:text-text-tertiary disabled:cursor-not-allowed',
    'px-6 py-3 rounded-button',
  ].join(' '),
  secondary: [
    'bg-white text-text-primary border border-border',
    'hover:bg-background',
    'disabled:bg-background disabled:text-text-tertiary disabled:border-border disabled:cursor-not-allowed',
    'px-6 py-3 rounded-button',
  ].join(' '),
  ghost: [
    'bg-transparent text-text-secondary',
    'hover:bg-[#F5F5F5]',
    'disabled:text-text-tertiary disabled:cursor-not-allowed',
    'px-4 py-2 rounded-button',
  ].join(' '),
  cta: [
    'bg-accent text-white',
    'hover:bg-accent-hover active:bg-accent-hover',
    'disabled:bg-border disabled:text-text-tertiary disabled:cursor-not-allowed',
    'px-8 py-4 text-lg rounded-button',
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
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2',
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
          className={`h-5 w-5 mr-2 ${variant === 'ghost' ? 'border-text-secondary' : 'border-white'}`}
        />
      )}
      {children}
    </button>
  )
}
