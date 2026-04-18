import { Skeleton } from '@/components/ui/skeleton'
import { Card } from '@/components/ui/card'

export const QuestionCardSkeleton = () => {
  return (
    <Card
      className="border border-border bg-surface p-5 shadow-sm"
      aria-hidden="true"
      role="listitem"
    >
      <div className="flex items-center gap-3">
        <Skeleton className="h-7 w-7 rounded-full" />
        <Skeleton className="h-5 w-16" />
      </div>
      <Skeleton className="mt-3 h-4 w-full" />
      <Skeleton className="mt-2 h-4 w-3/4" />
    </Card>
  )
}
