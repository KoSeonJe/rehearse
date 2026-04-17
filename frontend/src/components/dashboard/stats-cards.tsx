import { FileText, CheckCircle, Calendar } from 'lucide-react'
import type { InterviewStats } from '@/types/interview'

interface StatCardProps {
  label: string
  value: number
  icon: React.ReactNode
}

const StatCard = ({ label, value, icon }: StatCardProps) => (
  <div className="rounded-card bg-surface p-5 flex items-center gap-4 border border-border shadow-toss">
    <div className="flex-shrink-0 w-10 h-10 rounded-xl bg-violet-legacy-light flex items-center justify-center text-violet-legacy">
      {icon}
    </div>
    <div>
      <p className="text-xs font-semibold text-text-tertiary tracking-wide uppercase">{label}</p>
      <p className="mt-1 font-mono text-3xl font-extrabold text-text-primary">{value}</p>
    </div>
  </div>
)

const StatCardSkeleton = () => (
  <div className="rounded-card bg-surface p-5 animate-pulse flex items-center gap-4 border border-border shadow-toss">
    <div className="flex-shrink-0 w-10 h-10 bg-border/50 rounded-xl" />
    <div>
      <div className="h-3 w-16 bg-border/50 rounded-lg" />
      <div className="mt-2 h-10 w-20 bg-border/50 rounded-lg" />
    </div>
  </div>
)

interface StatsCardsProps {
  stats: InterviewStats | undefined
  isLoading: boolean
}

export const StatsCards = ({ stats, isLoading }: StatsCardsProps) => {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <StatCardSkeleton />
        <StatCardSkeleton />
        <StatCardSkeleton />
      </div>
    )
  }

  const allZero = !stats || (stats.totalCount === 0 && stats.completedCount === 0 && stats.thisWeekCount === 0)

  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-2">
        <StatCard label="총 면접" value={stats?.totalCount ?? 0} icon={<FileText size={20} />} />
        <StatCard label="완료" value={stats?.completedCount ?? 0} icon={<CheckCircle size={20} />} />
        <StatCard label="이번 주" value={stats?.thisWeekCount ?? 0} icon={<Calendar size={20} />} />
      </div>
      {allZero && (
        <p className="text-xs text-text-tertiary mt-1 mb-6">면접을 완료하면 여기에 기록이 쌓여요</p>
      )}
      {!allZero && <div className="mb-8" />}
    </div>
  )
}
