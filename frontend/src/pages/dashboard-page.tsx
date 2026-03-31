import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Logo } from '@/components/ui/logo'
import { StatsCards } from '@/components/dashboard/stats-cards'
import { InterviewList } from '@/components/dashboard/interview-list'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { useInterviews, useInterviewStats, useDeleteInterview } from '@/hooks/use-interviews'
import { apiClient } from '@/lib/api-client'

export const DashboardPage = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const { logout } = useAuthStore()

  const { data: interviewsData, isLoading: isInterviewsLoading } = useInterviews()
  const { data: statsData, isLoading: isStatsLoading } = useInterviewStats()
  const { mutate: deleteInterview, isPending: isDeletePending } = useDeleteInterview()

  const [deletingId, setDeletingId] = useState<number | null>(null)

  const handleLogout = async () => {
    try {
      await apiClient.post('/api/v1/auth/logout')
    } finally {
      logout()
      queryClient.removeQueries({ queryKey: ['auth', 'me'] })
    }
  }

  const handleDelete = (id: number) => {
    setDeletingId(id)
    deleteInterview(id, {
      onSettled: () => setDeletingId(null),
    })
  }

  const interviews = interviewsData?.data?.content ?? []
  const stats = statsData?.data

  return (
    <div className="min-h-screen bg-background text-text-primary">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-border/50">
        <div className="mx-auto flex h-16 max-w-3xl items-center justify-between px-5">
          <div className="flex items-center gap-2">
            <Logo size={80} />
            <span className="text-xl font-extrabold tracking-tight text-text-primary">
              리허설
            </span>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden text-sm font-medium text-text-secondary sm:block">
              {user?.name}
            </span>
            <button
              onClick={handleLogout}
              className="rounded-xl border border-border px-4 py-2 text-sm font-medium text-text-secondary transition-colors hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-border cursor-pointer"
            >
              로그아웃
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-5 py-12">
        {/* 인사 섹션 */}
        <section className="mb-10">
          <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary">
            {user?.name ? `${user.name}님의 면접 기록` : '면접 기록'}
          </h1>
        </section>

        {/* 통계 카드 */}
        <section className="mb-8">
          <StatsCards stats={stats} isLoading={isStatsLoading} />
        </section>

        {/* CTA 버튼 */}
        <section className="mb-12">
          <button
            onClick={() => navigate('/interview/setup')}
            className="flex h-14 w-full items-center justify-center gap-2 rounded-button bg-accent font-bold text-white hover:bg-accent-hover active:scale-95 transition-all duration-200 cursor-pointer"
          >
            <Plus size={20} />
            새 면접 시작하기
          </button>
        </section>

        {/* 면접 목록 */}
        <section>
          {(!isInterviewsLoading && interviews.length > 0) && (
            <h2 className="mb-4 text-lg font-bold text-text-primary">면접 기록</h2>
          )}
          <InterviewList
            interviews={interviews}
            isLoading={isInterviewsLoading}
            onDelete={handleDelete}
            deletingId={isDeletePending ? deletingId : null}
          />
        </section>
      </main>
    </div>
  )
}
