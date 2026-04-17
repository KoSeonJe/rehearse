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
    'bg-violet-legacy text-white shadow-lg shadow-violet-legacy/20 border-t border-white/10',
    'hover:bg-violet-legacy-hover hover:shadow-[0_4px_12px_rgba(0,0,0,0.1)] active:scale-[0.98]',
    'disabled:bg-border disabled:text-text-tertiary disabled:cursor-not-allowed disabled:shadow-none disabled:scale-100',
    'px-8 py-4 rounded-button text-sm font-black',
  ].join(' '),
  secondary: [
    'bg-white text-text-primary border border-border shadow-[0_1px_2px_rgba(0,0,0,0.02)]',
    'hover:bg-background hover:border-text-tertiary/30 active:scale-[0.98]',
    'disabled:bg-background disabled:text-text-tertiary disabled:border-border disabled:cursor-not-allowed disabled:scale-100',
    'px-8 py-4 rounded-button text-sm font-bold',
  ].join(' '),
  ghost: [
    'bg-transparent text-text-secondary hover:text-text-primary',
    'hover:bg-violet-legacy-light active:scale-[0.98]',
    'disabled:text-text-tertiary disabled:cursor-not-allowed disabled:scale-100',
    'px-4 py-2 rounded-button text-sm font-bold',
  ].join(' '),
  cta: [
    'bg-violet-legacy text-white shadow-[0_10px_20px_-5px_rgba(0,0,0,0.1)] border-t border-white/10',
    'hover:bg-violet-legacy-hover hover:shadow-[0_20px_25px_-5px_rgba(0,0,0,0.1)] active:scale-[0.98]',
    'disabled:bg-border disabled:text-text-tertiary disabled:cursor-not-allowed disabled:shadow-none disabled:scale-100',
    'px-10 py-4 text-base font-black tracking-tight rounded-button',
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
        'inline-flex items-center justify-center font-medium transition-all duration-150',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-legacy focus-visible:ring-offset-2',
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
