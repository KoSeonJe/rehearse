import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface StickyRailProps {
  children: ReactNode
  /** Tailwind column-span class. Default: col-span-4. */
  col?: string
  /** Sticky top offset class. Default: var(--utility-bar-height). */
  offset?: string
  className?: string
}

/**
 * Layout primitive for a sticky side rail (spec §5.4-A).
 * Pure layout: no domain knowledge. Composed by VideoDock, side outlines, etc.
 * Viewport clipping is prevented via max-h + overflow-y-auto.
 */
export const StickyRail = ({
  children,
  col = 'col-span-4',
  offset = 'top-[var(--utility-bar-height)]',
  className,
}: StickyRailProps) => (
  <aside
    className={cn(
      col,
      'sticky self-start',
      offset,
      'max-h-[calc(100vh-var(--utility-bar-height))] overflow-y-auto',
      className,
    )}
  >
    {children}
  </aside>
)
