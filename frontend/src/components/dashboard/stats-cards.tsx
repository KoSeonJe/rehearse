import type { InterviewStats } from '@/types/interview'

interface StatItemProps {
  label: string
  value: number
}

const StatItem = ({ label, value }: StatItemProps) => (
  <div className="flex flex-col gap-1">
    <p className="font-tabular text-[3rem] font-bold leading-none tracking-[-0.03em] text-foreground">
      {value}
    </p>
    <p className="text-sm font-semibold text-muted-foreground mt-2">
      {label}
    </p>
  </div>
)

const StatItemSkeleton = () => (
  <div className="flex flex-col gap-2 animate-pulse">
    <div className="h-12 w-16 bg-border/50 rounded-lg" />
    <div className="h-3 w-12 bg-border/40 rounded-lg mt-1" />
  </div>
)

interface StatsCardsProps {
  stats: InterviewStats | undefined
  isLoading: boolean
}

export const StatsCards = ({ stats, isLoading }: StatsCardsProps) => {
  if (isLoading) {
    return (
      <div className="flex items-start gap-12 mb-10 pb-8 border-b border-foreground/8">
        <StatItemSkeleton />
        <StatItemSkeleton />
        <StatItemSkeleton />
      </div>
    )
  }

  const allZero = !stats || (stats.totalCount === 0 && stats.completedCount === 0 && stats.thisWeekCount === 0)

  return (
    <div className="mb-10 pb-8 border-b border-foreground/8">
      <div className="flex items-start gap-10 md:gap-16">
        <StatItem label="총 면접" value={stats?.totalCount ?? 0} />
        <StatItem label="완료" value={stats?.completedCount ?? 0} />
        <StatItem label="이번 주" value={stats?.thisWeekCount ?? 0} />
      </div>
      {allZero && (
        <p className="text-xs text-muted-foreground mt-4">
          면접을 완료하면 여기에 기록이 쌓여요
        </p>
      )}
    </div>
  )
}
