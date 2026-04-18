import { useState } from 'react'
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { cn } from '@/lib/utils'
import type { StickyOutlineBaseProps } from './types'

interface MobileSheetProps extends StickyOutlineBaseProps {
  triggerLabel?: string
}

/**
 * md/sm variant: Radix Sheet bottom-sheet (spec §5.3).
 * Portal-based, so CSS hidden/block cannot hide the content — we gate the trigger only.
 * Closes automatically on select so the user lands back on reading flow.
 */
export const MobileSheet = ({
  items,
  activeId,
  onSelect,
  triggerLabel = '목차',
}: MobileSheetProps) => {
  const [open, setOpen] = useState(false)

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <button
          type="button"
          aria-label={triggerLabel}
          className="lg:hidden fixed bottom-6 right-4 z-30 h-11 min-w-11 px-4 rounded-pill bg-foreground text-background text-sm font-medium shadow-lg"
        >
          ≡ {triggerLabel}
        </button>
      </SheetTrigger>
      <SheetContent
        side="bottom"
        className="max-h-[60vh] rounded-t-radius-lg p-4"
      >
        <SheetTitle className="sr-only">질문 목차</SheetTitle>
        <nav aria-label="질문 목차" className="flex flex-col gap-1 py-4">
          {items.map((item) => {
            const isActive = activeId === item.id
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  onSelect(item.id)
                  setOpen(false)
                }}
                aria-current={isActive ? 'true' : undefined}
                className={cn(
                  'flex items-center gap-3 px-4 py-3 text-left rounded-sm min-h-11',
                  'transition-colors duration-[var(--duration-fast)]',
                  isActive ? 'text-foreground font-medium' : 'text-muted-foreground',
                )}
              >
                <span className="font-tabular text-[11px] w-5 text-accent-editorial">
                  {String(item.index).padStart(2, '0')}
                </span>
                <span className="flex-1">{item.label}</span>
                {item.hasIssue && (
                  <span
                    aria-label="이슈 있음"
                    className="w-1.5 h-1.5 rounded-full bg-signal-warning"
                  />
                )}
              </button>
            )
          })}
        </nav>
      </SheetContent>
    </Sheet>
  )
}
