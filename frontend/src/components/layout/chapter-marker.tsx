import { cn } from '@/lib/utils'

interface ChapterMarkerProps {
  /** 1-based index (renders as 01, 02, ...). */
  index: number
  /** Section title — display-lg, the real hierarchy anchor. */
  title: string
  /** Optional over-line category label (e.g. 'BEHAVIORAL'). */
  label?: string
  className?: string
  /**
   * Heading element for SEO/a11y. defaults to 'h2'.
   * Use 'h3' inside nested contexts.
   */
  as?: 'h2' | 'h3'
}

/**
 * Editorial chapter divider (spec §5.6).
 * Number is demoted to 11px over-line caption; title takes display-lg (40–48px).
 * Hairline top border + fade-in entry. No card box — hierarchy lives in type.
 */
export const ChapterMarker = ({
  index,
  title,
  label,
  className,
  as: Heading = 'h2',
}: ChapterMarkerProps) => (
  <div className={cn('pt-12 pb-6 border-t border-foreground/10 animate-fade-in', className)}>
    <div className="flex items-baseline gap-3 mb-3">
      <span
        className="font-tabular text-sm font-semibold text-accent-editorial select-none"
        aria-hidden="true"
      >
        {String(index).padStart(2, '0')}
      </span>
      {label && (
        <span className="text-xs font-semibold text-muted-foreground">
          {label}
        </span>
      )}
    </div>
    <Heading className="text-[2.5rem] md:text-[3rem] font-bold leading-[1.10] tracking-[-0.02em] text-foreground">
      {title}
    </Heading>
  </div>
)
