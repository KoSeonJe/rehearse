import { cn } from '@/lib/utils'
import type { StickyOutlineBaseProps } from './types'

interface DesktopProps extends StickyOutlineBaseProps {
  className?: string
}

/**
 * xl+ variant: col-span-2 sticky navigation (spec §5.3).
 * Hidden below xl via CSS; callers mount all three variants and let breakpoints decide.
 */
export const Desktop = ({ items, activeId, onSelect, className }: DesktopProps) => (
  <nav
    aria-label="질문 목차"
    className={cn(
      'hidden xl:flex flex-col col-span-2',
      'sticky top-[var(--utility-bar-height)] self-start',
      'pt-12 gap-1',
      className,
    )}
  >
    {items.map((item) => {
      const isActive = activeId === item.id
      return (
        <button
          key={item.id}
          type="button"
          onClick={() => onSelect(item.id)}
          aria-current={isActive ? 'true' : undefined}
          className={cn(
            'flex items-center gap-2 text-left pl-2.5 pr-2 py-2 rounded-sm',
            'text-[13px] transition-[color,background-color,border-color] duration-[var(--duration-fast)]',
            /* 선택 상태를 더 확실히 강조 — 배경 tint + 굵은 텍스트 + 3px accent 바 */
            isActive
              ? 'bg-accent-editorial-bg text-foreground font-semibold border-l-[3px] border-accent-editorial shadow-[inset_0_0_0_1px_rgba(166,81,49,0.08)]'
              : 'text-muted-foreground border-l-[3px] border-transparent hover:text-foreground hover:bg-foreground/4',
          )}
        >
          <span
            className={cn(
              'font-tabular text-[11px] w-5 shrink-0 transition-colors',
              isActive ? 'text-accent-editorial font-bold' : '',
            )}
          >
            {String(item.index).padStart(2, '0')}
          </span>
          <span className="truncate">{item.label}</span>
          {item.hasIssue && (
            <span
              aria-label="이슈 있음"
              className="ml-auto w-1.5 h-1.5 rounded-full bg-signal-warning shrink-0"
            />
          )}
        </button>
      )
    })}
  </nav>
)
