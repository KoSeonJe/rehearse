import type { InterviewListItem } from '@/types/interview'
import { Card } from '@/components/ui/card'
import { InterviewCard } from './interview-card'
import { EmptyState } from './empty-state'

const InterviewCardSkeleton = () => (
  <Card className="bg-surface p-5 animate-pulse border border-border shadow-sm">
    <div className="h-4 w-48 bg-border/50 rounded-lg" />
    <div className="mt-3 flex gap-2">
      <div className="h-5 w-16 bg-border/50 rounded-badge" />
      <div className="h-5 w-16 bg-border/50 rounded-badge" />
      <div className="h-5 w-10 bg-border/50 rounded-lg" />
    </div>
    <div className="mt-4 flex items-center justify-between">
      <div className="h-3 w-24 bg-border/50 rounded-lg" />
      <div className="h-5 w-12 bg-border/50 rounded-badge" />
    </div>
  </Card>
)

interface InterviewListProps {
  interviews: InterviewListItem[] | undefined
  isLoading: boolean
  onDelete: (id: number) => void
  deletingId: number | null
}

export const InterviewList = ({
  interviews,
  isLoading,
  onDelete,
  deletingId,
}: InterviewListProps) => {
  if (isLoading) {
    return (
      <div className="flex flex-col gap-3">
        <InterviewCardSkeleton />
        <InterviewCardSkeleton />
        <InterviewCardSkeleton />
      </div>
    )
  }

  if (!interviews || interviews.length === 0) {
    return <EmptyState />
  }

  return (
    <div className="flex flex-col gap-3">
      {interviews.map((interview) => (
        <InterviewCard
          key={interview.id}
          interview={interview}
          onDelete={onDelete}
          isDeleting={deletingId === interview.id}
        />
      ))}
    </div>
  )
}
