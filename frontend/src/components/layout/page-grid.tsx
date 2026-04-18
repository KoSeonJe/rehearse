import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface PageGridProps {
  children: ReactNode
  className?: string
  /** semantic wrapper element. defaults to 'div'. */
  as?: 'div' | 'section' | 'main' | 'article'
}

/**
 * 12-column asymmetric grid wrapper (spec §4.1, §5.1).
 * Responsive: 4-col (sm) → 8-col (md) → 12-col (lg+).
 * Child sections opt into a recipe via `col-span-*` utilities.
 */
export const PageGrid = ({ children, className, as: Tag = 'div' }: PageGridProps) => (
  <Tag
    className={cn(
      'mx-auto w-full max-w-canvas px-4 md:px-8 lg:px-12',
      'grid grid-cols-4 gap-x-4',
      'md:grid-cols-8 md:gap-x-5',
      'lg:grid-cols-12 lg:gap-x-6',
      className,
    )}
  >
    {children}
  </Tag>
)
