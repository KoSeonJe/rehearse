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
            'flex items-center gap-2 text-left px-2 py-1.5 rounded-sm',
            'text-[13px] transition-[color,border-color] duration-[var(--duration-fast)]',
            isActive
              ? 'text-foreground font-medium border-l-2 border-accent-editorial'
              : 'text-muted-foreground border-l-2 border-transparent hover:text-foreground',
          )}
        >
          <span className="font-tabular text-[11px] w-5 shrink-0">
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
