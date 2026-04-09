import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { StatsCards } from '@/components/dashboard/stats-cards'
import { InterviewList } from '@/components/dashboard/interview-list'
import { InterviewTable } from '@/components/dashboard/interview-table'
import { Sidebar } from '@/components/dashboard/sidebar'
import { DashboardHeader } from '@/components/dashboard/dashboard-header'
import { ServiceFeedbackModal } from '@/components/dashboard/service-feedback-modal'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { MessageSquarePlus } from 'lucide-react'
import { useInterviews, useInterviewStats, useDeleteInterview } from '@/hooks/use-interviews'
import { useFeedbackNeedCheck } from '@/hooks/use-service-feedback'
import { apiClient } from '@/lib/api-client'

export const DashboardPage = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const { logout } = useAuthStore()

  const { data: interviewsData, isLoading: isInterviewsLoading } = useInterviews()
  const { data: statsData, isLoading: isStatsLoading } = useInterviewStats()
  const { mutate: deleteInterview, isPending: isDeletePending } = useDeleteInterview()
  const { data: needCheckData } = useFeedbackNeedCheck()

  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [showAutoFeedback, setShowAutoFeedback] = useState(false)
  const autoFeedbackShownRef = useRef(false)
  const [showVoluntaryFeedback, setShowVoluntaryFeedback] = useState(false)

  const isDismissedRecently = () => {
    const dismissed = localStorage.getItem('feedback-dismissed-at')
    if (!dismissed) return false
    return Date.now() - Number(dismissed) < 24 * 60 * 60 * 1000
  }

  useEffect(() => {
    if (needCheckData?.data.needsFeedback && !autoFeedbackShownRef.current && !isDismissedRecently()) {
      setShowAutoFeedback(true)
      autoFeedbackShownRef.current = true
    }
  }, [needCheckData?.data.needsFeedback])

  const handleDismissFeedback = () => {
    localStorage.setItem('feedback-dismissed-at', String(Date.now()))
    setShowAutoFeedback(false)
  }

  const handleOpenVoluntaryFeedback = () => setShowVoluntaryFeedback(true)

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
        <DashboardHeader user={user} onLogout={handleLogout} onFeedbackClick={handleOpenVoluntaryFeedback} />

        <main className="px-5 py-8 lg:px-10 lg:py-10">
          {/* 인사 섹션 — 데스크탑 */}
          <div className="hidden lg:flex items-center justify-between mb-8">
            <h1 className="text-xl font-extrabold text-text-primary tracking-tight">
              {user?.name ? `${user.name}님, 안녕하세요` : '안녕하세요'}
            </h1>
            <button
              onClick={handleOpenVoluntaryFeedback}
              className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium text-accent hover:bg-accent-light transition-colors cursor-pointer"
            >
              <MessageSquarePlus size={16} />
              피드백 보내기
            </button>
          </div>

          {/* 통계 카드 */}
          <StatsCards stats={stats} isLoading={isStatsLoading} />

          {/* 면접 기록 */}
          {(!isInterviewsLoading && interviews.length > 0) && (
            <div className="hidden lg:flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-text-primary">면접 기록</h2>
              <button
                onClick={() => navigate('/interview/setup')}
                className="h-9 px-4 rounded-button bg-accent text-white text-sm font-bold hover:bg-accent-hover active:scale-95 transition-all cursor-pointer"
              >
                + 새 면접
              </button>
            </div>
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
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-text-primary">면접 기록</h2>
                <button
                  onClick={() => navigate('/interview/setup')}
                  className="h-9 px-4 rounded-button bg-accent text-white text-sm font-bold hover:bg-accent-hover active:scale-95 transition-all cursor-pointer"
                >
                  + 새 면접
                </button>
              </div>
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

      <ServiceFeedbackModal
        isOpen={showAutoFeedback}
        onClose={handleDismissFeedback}
        source="AUTO_POPUP"
      />
      <ServiceFeedbackModal
        isOpen={showVoluntaryFeedback}
        onClose={() => setShowVoluntaryFeedback(false)}
        source="VOLUNTARY"
      />
    </div>
  )
}
