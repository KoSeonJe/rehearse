import { cn } from '@/lib/utils'
import type { StickyOutlineBaseProps } from './types'

interface TabBarProps extends StickyOutlineBaseProps {
  className?: string
}

/**
 * lg variant: horizontal scrollable tab bar (spec §5.3).
 * Visible in [lg, xl); hidden at xl+ where Desktop takes over.
 */
export const TabBar = ({ items, activeId, onSelect, className }: TabBarProps) => (
  <nav
    aria-label="질문 탭"
    className={cn(
      'hidden lg:flex xl:hidden overflow-x-auto',
      'sticky top-[var(--utility-bar-height)] z-10',
      'bg-background/95 backdrop-blur-sm border-b border-foreground/10',
      'px-4 gap-1',
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
            'shrink-0 px-3 py-2.5 text-[13px] relative font-tabular',
            'transition-[color,border-color] duration-[var(--duration-fast)]',
            isActive
              ? 'text-foreground border-b-2 border-accent-editorial font-medium'
              : 'text-muted-foreground border-b-2 border-transparent hover:text-foreground',
          )}
        >
          {String(item.index).padStart(2, '0')}
          {item.hasIssue && (
            <span
              aria-label="이슈 있음"
              className="absolute top-2 right-1 w-1.5 h-1.5 rounded-full bg-signal-warning"
            />
          )}
        </button>
      )
    })}
  </nav>
)
