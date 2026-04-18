import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface UtilityBarProps {
  /** Over-line context, e.g. "FEEDBACK · Q3 of 8". */
  chapter?: string
  /** Right-side actions. Each interactive child should guarantee ≥44×44 hit box. */
  actions?: ReactNode
  className?: string
}

/**
 * Thin sticky header replacing legacy SaaS nav (spec §5.5).
 * 44px desktop / 56px mobile. Height is pulled from `--utility-bar-height` CSS var
 * so downstream sticky offsets stay consistent.
 */
export const UtilityBar = ({ chapter, actions, className }: UtilityBarProps) => (
  <header
    role="banner"
    className={cn(
      'sticky top-0 z-20 w-full border-b border-foreground/8',
      /* Quiet Rigor anti-slop: backdrop-blur 사용 금지.
         불투명 배경 + hairline border로 정직하게 분리한다. */
      'bg-background',
      'h-[var(--utility-bar-height)]',
      'flex items-center justify-between',
      'px-4 md:px-8',
      className,
    )}
  >
    {chapter ? (
      <span className="text-xs font-semibold text-muted-foreground">
        {chapter}
      </span>
    ) : (
      <span aria-hidden="true" />
    )}
    <div className="flex items-center gap-1">{actions}</div>
  </header>
)
