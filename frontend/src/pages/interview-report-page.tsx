import { useParams, useNavigate } from 'react-router-dom'
import { useReport } from '../hooks/use-report'
import { LogoIcon } from '@/components/ui/logo-icon'
import { Button } from '@/components/ui/button'
import { Character } from '@/components/ui/character'
import { ScoreCard } from '../components/review/score-card'
import { ImprovementList } from '../components/review/improvement-list'

export const InterviewReportPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: response, isLoading, isError } = useReport(id ?? '')

  if (isLoading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background">
        <Character mood="thinking" size={100} className="mx-auto" />
        <p className="mt-4 text-sm text-text-secondary">리포트를 생성하고 있습니다...</p>
      </div>
    )
  }

  if (isError || !response?.data) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background">
        <Character mood="confused" size={100} className="mx-auto" />
        <p className="mt-4 text-lg font-medium text-text-primary">리포트를 불러올 수 없습니다</p>
        <Button
          variant="secondary"
          className="mt-4"
          onClick={() => navigate('/')}
        >
          홈으로 돌아가기
        </Button>
      </div>
    )
  }

  const report = response.data

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Header */}
      <header className="border-b border-border bg-surface px-4 py-4 sm:px-6">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <div className="flex items-center gap-3">
            <LogoIcon size={28} />
            <h1 className="text-lg font-bold text-text-primary">Rehearse</h1>
            <span className="hidden text-sm text-text-secondary sm:inline">종합 리포트</span>
          </div>
          <div className="flex items-center gap-2 sm:gap-3">
            <Button
              variant="secondary"
              onClick={() => navigate(`/interview/${id}/review`)}
              className="hidden sm:inline-flex"
            >
              피드백 리뷰
            </Button>
            <Button
              variant="primary"
              onClick={() => navigate('/')}
            >
              홈으로
            </Button>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto w-full max-w-3xl space-y-6 px-4 py-6 sm:px-6 sm:py-8">
        <ScoreCard score={report.overallScore} />

        {/* 요약 */}
        <div className="rounded-card border border-border bg-surface p-6">
          <h2 className="mb-3 text-sm font-semibold text-text-primary">종합 평가</h2>
          <p className="text-sm leading-relaxed text-text-secondary">{report.summary}</p>
          <p className="mt-3 text-xs text-text-tertiary">
            총 {report.feedbackCount}개의 피드백을 기반으로 분석되었습니다.
          </p>
        </div>

        {/* 강점 / 개선점 */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <ImprovementList
            title="강점"
            items={report.strengths}
            variant="strength"
          />
          <ImprovementList
            title="개선 포인트"
            items={report.improvements}
            variant="improvement"
          />
        </div>

        {/* 다음 단계 */}
        <div className="rounded-card border border-border bg-surface p-6 text-center">
          <p className="mb-4 text-sm text-text-secondary">
            더 자세한 피드백을 확인하고 싶다면 타임스탬프 리뷰를 확인하세요.
          </p>
          <Button
            variant="primary"
            onClick={() => navigate(`/interview/${id}/review`)}
          >
            타임스탬프 피드백 리뷰 보기
          </Button>
        </div>
      </main>
    </div>
  )
}
