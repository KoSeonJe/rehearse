import { cva } from 'class-variance-authority'

/**
 * Button variants.
 *
 * Roadmap:
 * - `primary` / `md` 는 기존 호환 alias. Phase 3 전체 완료 후 호출부를 `default`로 통일하고 제거.
 * - `cta` 는 랜딩 hero 전용(강한 shadow). 토큰/시스템 정리 시 재검토.
 */
export const buttonVariants = cva(
  'inline-flex items-center justify-center font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        // shadcn 표준 variants
        // default = warm off-black (secondary dark), primary/cta = teal brand signature
        default:
          'bg-primary text-primary-foreground shadow-lg shadow-primary/20 border-t border-white/10 hover:bg-primary/90 active:scale-[0.98]',
        destructive:
          'bg-error text-white hover:opacity-90 active:scale-[0.98]',
        outline:
          'border border-brand bg-background text-brand shadow-[0_1px_2px_rgba(0,0,0,0.02)] hover:bg-brand-bg active:scale-[0.98]',
        secondary:
          'bg-background text-text-primary border border-border shadow-[0_1px_2px_rgba(0,0,0,0.02)] hover:bg-muted hover:border-text-tertiary/30 active:scale-[0.98]',
        ghost:
          'bg-transparent text-text-secondary hover:text-brand hover:bg-brand-bg active:scale-[0.98]',
        link: 'text-brand underline-offset-4 hover:underline',
        // Brand-colored primary CTA (teal)
        primary:
          'bg-brand text-brand-foreground shadow-lg shadow-brand/25 border-t border-white/15 hover:bg-brand-hover active:scale-[0.98]',
        cta: 'bg-brand text-brand-foreground shadow-[0_10px_20px_-5px_hsl(var(--brand)/0.35)] border-t border-white/15 hover:bg-brand-hover hover:shadow-[0_20px_25px_-5px_hsl(var(--brand)/0.4)] active:scale-[0.98]',
      },
      size: {
        default: 'h-10 px-8 py-2 rounded-button text-sm',
        sm: 'h-9 px-4 rounded-button text-sm',
        lg: 'h-16 px-10 py-4 rounded-button text-lg',
        icon: 'h-9 w-9 rounded-button',
        // 기존 호환 alias
        md: 'h-10 px-8 py-2 rounded-button text-sm',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)
