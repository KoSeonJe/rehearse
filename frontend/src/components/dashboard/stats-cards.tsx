import type { InterviewStats } from '@/types/interview'

interface StatCardProps {
  label: string
  value: number
}

const StatCard = ({ label, value }: StatCardProps) => (
  <div className="rounded-card bg-surface p-5">
    <p className="text-xs font-semibold text-text-tertiary tracking-wide uppercase">{label}</p>
    <p className="mt-2 font-mono text-3xl font-extrabold text-text-primary">{value}</p>
  </div>
)

const StatCardSkeleton = () => (
  <div className="rounded-card bg-surface p-5 animate-pulse">
    <div className="h-3 w-16 bg-border/50 rounded-lg" />
    <div className="mt-3 h-10 w-20 bg-border/50 rounded-lg" />
  </div>
)

interface StatsCardsProps {
  stats: InterviewStats | undefined
  isLoading: boolean
}

export const StatsCards = ({ stats, isLoading }: StatsCardsProps) => {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <StatCardSkeleton />
        <StatCardSkeleton />
        <StatCardSkeleton />
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <StatCard label="총 면접" value={stats?.totalCount ?? 0} />
      <StatCard label="완료" value={stats?.completedCount ?? 0} />
      <StatCard label="이번 주" value={stats?.thisWeekCount ?? 0} />
    </div>
  )
}
