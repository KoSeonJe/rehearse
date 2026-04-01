import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { StatsCards } from '@/components/dashboard/stats-cards'
import { InterviewList } from '@/components/dashboard/interview-list'
import { InterviewTable } from '@/components/dashboard/interview-table'
import { Sidebar } from '@/components/dashboard/sidebar'
import { DashboardHeader } from '@/components/dashboard/dashboard-header'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { useInterviews, useInterviewStats, useDeleteInterview } from '@/hooks/use-interviews'
import { apiClient } from '@/lib/api-client'

export const DashboardPage = () => {
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
      {/* 사이드바 — 데스크탑 전용 */}
      <Sidebar user={user} onLogout={handleLogout} />

      {/* 메인 콘텐츠 */}
      <div className="lg:ml-60">
        {/* 모바일/데스크탑 헤더 통합 */}
        <DashboardHeader user={user} onLogout={handleLogout} />

        <main className="px-5 py-8 lg:px-10 lg:py-10">
          {/* 인사 섹션 — 데스크탑 */}
          <div className="hidden lg:block mb-8">
            <h1 className="text-xl font-extrabold text-text-primary tracking-tight">
              {user?.name ? `${user.name}님, 안녕하세요` : '안녕하세요'}
            </h1>
          </div>

          {/* 통계 카드 */}
          <StatsCards stats={stats} isLoading={isStatsLoading} />

          {/* 면접 기록 */}
          {(!isInterviewsLoading && interviews.length > 0) && (
            <h2 className="hidden lg:block mb-4 text-lg font-bold text-text-primary">면접 기록</h2>
          )}

          {/* 데스크탑: 테이블 뷰 */}
          <div className="hidden lg:block">
            <InterviewTable
              interviews={interviews}
              isLoading={isInterviewsLoading}
              onDelete={handleDelete}
              deletingId={isDeletePending ? deletingId : null}
            />
          </div>

          {/* 모바일: 카드 뷰 */}
          <div className="lg:hidden">
            {(!isInterviewsLoading && interviews.length > 0) && (
              <h2 className="mb-4 text-lg font-bold text-text-primary">면접 기록</h2>
            )}
            <InterviewList
              interviews={interviews}
              isLoading={isInterviewsLoading}
              onDelete={handleDelete}
              deletingId={isDeletePending ? deletingId : null}
            />
          </div>
        </main>
      </div>
    </div>
  )
}
