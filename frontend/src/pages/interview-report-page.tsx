import { useParams, useNavigate } from 'react-router-dom'
import { useReport } from '@/hooks/use-report'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'

export const InterviewReportPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: response, isLoading, isError } = useReport(id ?? '')

  if (isLoading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white px-5">
        <div className="relative mb-10">
          <Character mood="thinking" size={160} />
          <div className="absolute -bottom-4 left-1/2 -translate-x-1/2 w-24 h-1.5 bg-slate-100 rounded-full overflow-hidden">
            <div className="h-full bg-accent animate-progress-loading" />
          </div>
        </div>
        <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary">AI가 리포트를 분석하고 있어요</h1>
        <p className="mt-3 text-base font-medium text-text-secondary">잠시만 기다려주세요</p>
      </div>
    )
  }

  if (isError || !response?.data) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white px-5">
        <Character mood="confused" size={140} className="mb-8" />
        <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary text-center">리포트를 불러올 수<br />없습니다</h1>
        <button
          className="mt-10 h-16 w-full max-w-xs rounded-[24px] bg-accent font-black text-white active:scale-95"
          onClick={() => navigate('/')}
        >
          홈으로 돌아가기
        </button>
      </div>
    )
  }

  const report = response.data

  return (
    <div className="min-h-screen bg-white pb-32">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md px-5 pt-6 pb-4 md:px-8">
        <div className="mx-auto flex max-w-4xl items-center justify-between">
          <div className="flex items-center gap-2" onClick={() => navigate('/')} role="button">
            <Logo size={60} />
            <span className="text-lg font-black tracking-tight text-text-primary">리허설 리포트</span>
          </div>
          <button 
            onClick={() => navigate('/')}
            className="text-sm font-bold text-text-secondary hover:text-text-primary"
          >
            닫기
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-5 pt-12 md:px-8">
        {/* Score Hero */}
        <section className="text-center mb-16">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-4">
            Interview Result
          </p>
          <div className="inline-flex items-end gap-1 mb-6">
            <span className="text-8xl font-black tracking-tighter text-text-primary">
              {report.overallScore}
            </span>
            <span className="text-2xl font-black text-text-tertiary mb-3">/ 100</span>
          </div>
          <h2 className="text-2xl font-extrabold tracking-tighter text-text-primary sm:text-3xl">
            {report.overallScore >= 80 ? '매우 인상적인 면접이었어요!' : '충분히 잘하셨어요, 조금만 더 다듬어볼까요?'}
          </h2>
        </section>

        <div className="space-y-10">
          {/* Summary Card */}
          <section className="rounded-[32px] bg-surface p-8 md:p-10">
            <h3 className="text-sm font-black uppercase tracking-widest text-text-tertiary mb-6">종합 평가</h3>
            <p className="text-lg font-bold leading-relaxed text-text-primary tracking-tight">
              {report.summary}
            </p>
            <div className="mt-8 pt-6 border-t border-border/50 flex items-center justify-between">
              <span className="text-xs font-bold text-text-tertiary">분석 데이터</span>
              <span className="text-xs font-bold text-text-secondary">{report.feedbackCount}개의 타임스탬프 피드백</span>
            </div>
          </section>

          {/* Strengths & Improvements */}
          <section className="grid gap-6 md:grid-cols-2">
            <div className="rounded-[32px] bg-success/5 p-8 border border-success/10">
              <div className="flex items-center gap-2 mb-6">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-success text-[10px] text-white">✓</span>
                <h3 className="text-sm font-black uppercase tracking-widest text-success">강점</h3>
              </div>
              <ul className="space-y-4">
                {report.strengths.map((s, i) => (
                  <li key={i} className="text-[15px] font-bold text-text-primary leading-snug tracking-tight">
                    • {s}
                  </li>
                ))}
              </ul>
            </div>

            <div className="rounded-[32px] bg-accent/5 p-8 border border-accent/10">
              <div className="flex items-center gap-2 mb-6">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-accent text-[10px] text-white">!</span>
                <h3 className="text-sm font-black uppercase tracking-widest text-accent">보완점</h3>
              </div>
              <ul className="space-y-4">
                {report.improvements.map((s, i) => (
                  <li key={i} className="text-[15px] font-bold text-text-primary leading-snug tracking-tight">
                    • {s}
                  </li>
                ))}
              </ul>
            </div>
          </section>

          {/* Detailed Feedback CTA */}
          <section className="rounded-[32px] bg-slate-950 p-10 text-center text-white">
            <h3 className="text-xl font-extrabold mb-3">영상을 보며 정밀 교정하기</h3>
            <p className="text-white/60 text-sm font-medium mb-10">
              피드백이 생성된 정확한 시점을 확인하고<br />
              나의 시선과 표정을 다시 체크해보세요.
            </p>
            <button
              onClick={() => navigate(`/interview/${id}/feedback`)}
              className="h-16 w-full rounded-[24px] bg-white text-slate-950 font-black text-lg active:scale-95 transition-all"
            >
              타임스탬프 리뷰 보기
            </button>
          </section>
        </div>
      </main>
    </div>
  )
}
