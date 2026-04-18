import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface ReadingColumnProps {
  children: ReactNode
  className?: string
}

/**
 * Long-form reading container tuned for feedback scan (spec §5.2).
 * 55ch width + 1.65 line-height for scan-first density (vs 65ch/1.75 prose default).
 * Applies paragraph + h2 rhythm via descendant utilities.
 */
export const ReadingColumn = ({ children, className }: ReadingColumnProps) => (
  <div
    className={cn(
      'max-w-[55ch] text-[1.0625rem]/[1.65] text-foreground',
      '[&>p+p]:mt-6',
      '[&>h2]:mt-12 [&>h2]:mb-4',
      className,
    )}
  >
    {children}
  </div>
)
