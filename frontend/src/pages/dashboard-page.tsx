import { useState, useEffect, useRef } from 'react'
import { Helmet } from 'react-helmet-async'
import { useNavigate } from 'react-router-dom'
import { StatsCards } from '@/components/dashboard/stats-cards'
import { InterviewList } from '@/components/dashboard/interview-list'
import { InterviewTable } from '@/components/dashboard/interview-table'
import { AppShell } from '@/components/layout/app-shell'
import { DashboardHeader } from '@/components/dashboard/dashboard-header'
import { ServiceFeedbackModal } from '@/components/dashboard/service-feedback-modal'
import { useAuth } from '@/hooks/use-auth'
import { useLogout } from '@/hooks/use-logout'
import { MessageSquarePlus } from 'lucide-react'
import { useInterviews, useInterviewStats, useDeleteInterview } from '@/hooks/use-interviews'
import { useFeedbackNeedCheck } from '@/hooks/use-service-feedback'

export const DashboardPage = () => {
  const navigate = useNavigate()
  const { user } = useAuth()
  const logout = useLogout()

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
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setShowAutoFeedback(true)
      autoFeedbackShownRef.current = true
    }
  }, [needCheckData?.data.needsFeedback])

  const handleDismissFeedback = () => {
    localStorage.setItem('feedback-dismissed-at', String(Date.now()))
    setShowAutoFeedback(false)
  }

  const handleOpenVoluntaryFeedback = () => setShowVoluntaryFeedback(true)

  const handleDelete = (id: number) => {
    setDeletingId(id)
    deleteInterview(id, {
      onSettled: () => setDeletingId(null),
    })
  }

  const interviews = interviewsData?.data?.content ?? []
  const stats = statsData?.data

  return (
    <AppShell header={<DashboardHeader user={user} onLogout={logout} onFeedbackClick={handleOpenVoluntaryFeedback} />}>
      <Helmet>
        <title>대시보드 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>
      {/* 인사 섹션 — 데스크탑 */}
      <div className="hidden lg:flex items-center justify-between mb-8">
        <h1 className="text-xl font-extrabold text-text-primary tracking-tight">
          {user?.name ? `${user.name}님, 안녕하세요` : '안녕하세요'}
        </h1>
        <button
          onClick={handleOpenVoluntaryFeedback}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium text-violet-legacy hover:bg-violet-legacy-light transition-colors cursor-pointer"
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
            className="h-9 px-4 rounded-button bg-violet-legacy text-white text-sm font-bold hover:bg-violet-legacy-hover active:scale-95 transition-all cursor-pointer"
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
              className="h-9 px-4 rounded-button bg-violet-legacy text-white text-sm font-bold hover:bg-violet-legacy-hover active:scale-95 transition-all cursor-pointer"
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
    </AppShell>
  )
}
