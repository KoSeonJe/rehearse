import { useParams, useNavigate } from 'react-router-dom'
import { useReport } from '../hooks/use-report'
import ScoreCard from '../components/review/score-card'
import ImprovementList from '../components/review/improvement-list'

const InterviewReportPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: response, isLoading, isError } = useReport(id ?? '')

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="space-y-4 text-center">
          <div className="mx-auto h-8 w-8 animate-spin rounded-full border-3 border-slate-300 border-t-slate-900" />
          <p className="text-sm text-slate-500">리포트를 생성하고 있습니다...</p>
        </div>
      </div>
    )
  }

  if (isError || !response?.data) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="space-y-4 text-center">
          <p className="text-lg font-medium text-slate-900">리포트를 불러올 수 없습니다</p>
          <button
            onClick={() => navigate('/')}
            className="rounded-xl border border-slate-200 bg-white px-6 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            홈으로 돌아가기
          </button>
        </div>
      </div>
    )
  }

  const report = response.data

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      {/* Header */}
      <header className="border-b border-slate-200 bg-white px-6 py-4">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-lg font-bold text-slate-900">Rehearse</h1>
            <span className="text-sm text-slate-500">종합 리포트</span>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate(`/interview/${id}/review`)}
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              피드백 리뷰
            </button>
            <button
              onClick={() => navigate('/')}
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
            >
              홈으로
            </button>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto w-full max-w-3xl space-y-6 px-6 py-8">
        <ScoreCard score={report.overallScore} />

        {/* 요약 */}
        <div className="rounded-2xl border border-slate-200 bg-white p-6">
          <h2 className="mb-3 text-sm font-semibold text-slate-900">종합 평가</h2>
          <p className="text-sm leading-relaxed text-slate-700">{report.summary}</p>
          <p className="mt-3 text-xs text-slate-400">
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
        <div className="rounded-2xl border border-slate-200 bg-white p-6 text-center">
          <p className="mb-4 text-sm text-slate-600">
            더 자세한 피드백을 확인하고 싶다면 타임스탬프 리뷰를 확인하세요.
          </p>
          <button
            onClick={() => navigate(`/interview/${id}/review`)}
            className="rounded-xl bg-slate-900 px-8 py-3 text-sm font-medium text-white hover:bg-slate-800"
          >
            타임스탬프 피드백 리뷰 보기
          </button>
        </div>
      </main>
    </div>
  )
}

export default InterviewReportPage
