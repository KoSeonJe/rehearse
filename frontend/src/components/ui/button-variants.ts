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
        default:
          'bg-primary text-primary-foreground shadow-lg shadow-primary/20 border-t border-white/10 hover:bg-primary/90 active:scale-[0.98]',
        destructive:
          'bg-error text-white hover:opacity-90 active:scale-[0.98]',
        outline:
          'border border-border bg-white text-text-primary shadow-[0_1px_2px_rgba(0,0,0,0.02)] hover:bg-background hover:border-text-tertiary/30 active:scale-[0.98]',
        secondary:
          'bg-white text-text-primary border border-border shadow-[0_1px_2px_rgba(0,0,0,0.02)] hover:bg-background hover:border-text-tertiary/30 active:scale-[0.98]',
        ghost:
          'bg-transparent text-text-secondary hover:text-text-primary hover:bg-muted active:scale-[0.98]',
        link: 'text-primary underline-offset-4 hover:underline',
        // 기존 호환 aliases
        primary:
          'bg-primary text-primary-foreground shadow-lg shadow-primary/20 border-t border-white/10 hover:bg-primary/90 active:scale-[0.98]',
        cta: 'bg-primary text-primary-foreground shadow-[0_10px_20px_-5px_rgba(0,0,0,0.1)] border-t border-white/10 hover:bg-primary/90 hover:shadow-[0_20px_25px_-5px_rgba(0,0,0,0.1)] active:scale-[0.98]',
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
