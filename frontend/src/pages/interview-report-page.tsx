import { useParams, useNavigate } from 'react-router-dom'
import { useReport } from '@/hooks/use-report'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import { useEffect, useRef, useState } from 'react'
import type { InterviewReport } from '@/types/interview'

// --- 점수 등급 유틸 ---

interface ScoreGrade {
  label: string
  badge: string
  color: string
  bgColor: string
  message: string
  action: string
}

const getScoreGrade = (score: number): ScoreGrade => {
  if (score >= 90) return {
    label: '최고 수준',
    badge: 'A+',
    color: 'text-emerald-600',
    bgColor: 'bg-emerald-50 border-emerald-200',
    message: '면접관을 사로잡을 수 있는 실력이에요. 자신감을 가지고 실전에 임하세요!',
    action: '실전 면접에서 같은 컨디션을 유지하는 연습을 추천해요.',
  }
  if (score >= 80) return {
    label: '우수',
    badge: 'A',
    color: 'text-emerald-600',
    bgColor: 'bg-emerald-50 border-emerald-200',
    message: '전반적으로 매우 인상적인 답변이었어요. 세부 사항만 다듬으면 완벽해요.',
    action: '보완점에 표시된 부분만 집중 연습하면 A+도 충분히 가능해요.',
  }
  if (score >= 70) return {
    label: '합격권',
    badge: 'B+',
    color: 'text-blue-600',
    bgColor: 'bg-blue-50 border-blue-200',
    message: '합격 가능성이 높은 수준이에요. 몇 가지 포인트만 보완하면 더 좋아질 거예요.',
    action: '강점을 유지하면서 개선점에 집중해 2~3회 더 연습해보세요.',
  }
  if (score >= 60) return {
    label: '보통',
    badge: 'B',
    color: 'text-amber-600',
    bgColor: 'bg-amber-50 border-amber-200',
    message: '기본기는 갖추고 있어요. 구체적인 사례와 논리적 구조를 더해보세요.',
    action: '타임스탬프 리뷰에서 아쉬운 구간을 확인하고 답변을 재구성해보세요.',
  }
  if (score >= 50) return {
    label: '보완 필요',
    badge: 'C+',
    color: 'text-orange-600',
    bgColor: 'bg-orange-50 border-orange-200',
    message: '답변의 방향은 맞지만, 깊이와 구체성을 더 보강할 필요가 있어요.',
    action: '각 질문에 대해 STAR 기법으로 답변을 정리해보세요.',
  }
  return {
    label: '집중 연습 필요',
    badge: 'C',
    color: 'text-red-600',
    bgColor: 'bg-red-50 border-red-200',
    message: '아직 준비가 더 필요해요. 하지만 연습할수록 반드시 나아질 거예요!',
    action: '기출 질문 리스트를 만들고, 하루 2~3개씩 답변을 정리하는 것부터 시작해보세요.',
  }
}

// --- 원형 게이지 컴포넌트 ---

const ScoreGauge = ({ score, grade }: { score: number; grade: ScoreGrade }) => {
  const [animatedScore, setAnimatedScore] = useState(0)
  const [strokeOffset, setStrokeOffset] = useState(283)
  const rafRef = useRef<number>(0)
  const startTimeRef = useRef<number>(0)

  useEffect(() => {
    const duration = 1500
    const circumference = 283 // 2 * PI * 45

    startTimeRef.current = 0
    const animate = (timestamp: number) => {
      if (!startTimeRef.current) startTimeRef.current = timestamp
      const elapsed = timestamp - startTimeRef.current
      const progress = Math.min(elapsed / duration, 1)

      // easeOutCubic
      const eased = 1 - Math.pow(1 - progress, 3)

      setAnimatedScore(Math.round(eased * score))
      setStrokeOffset(circumference - (eased * score / 100) * circumference)

      if (progress < 1) {
        rafRef.current = requestAnimationFrame(animate)
      }
    }

    // 약간의 딜레이 후 애니메이션 시작
    const timer = setTimeout(() => {
      rafRef.current = requestAnimationFrame(animate)
    }, 300)

    return () => {
      clearTimeout(timer)
      cancelAnimationFrame(rafRef.current)
    }
  }, [score])

  const strokeColor = (() => {
    if (score >= 80) return '#059669'
    if (score >= 70) return '#2563eb'
    if (score >= 60) return '#d97706'
    if (score >= 50) return '#ea580c'
    return '#dc2626'
  })()

  return (
    <div className="relative inline-flex items-center justify-center">
      <svg width="200" height="200" viewBox="0 0 100 100" className="-rotate-90">
        {/* 배경 원 */}
        <circle
          cx="50" cy="50" r="45"
          fill="none"
          stroke="#f1f5f9"
          strokeWidth="6"
        />
        {/* 점수 원 */}
        <circle
          cx="50" cy="50" r="45"
          fill="none"
          stroke={strokeColor}
          strokeWidth="6"
          strokeLinecap="round"
          strokeDasharray="283"
          strokeDashoffset={strokeOffset}
          className="transition-none"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-5xl font-black tracking-tighter text-text-primary">
          {animatedScore}
        </span>
        <span className="text-sm font-bold text-text-tertiary mt-0.5">/ 100</span>
        <span className={`mt-2 inline-flex items-center rounded-full px-3 py-1 text-xs font-black border ${grade.bgColor} ${grade.color}`}>
          {grade.badge}
        </span>
      </div>
    </div>
  )
}

// --- 리포트 콘텐츠 ---

const ReportContent = ({ report, interviewId }: { report: InterviewReport; interviewId: string }) => {
  const navigate = useNavigate()
  const grade = getScoreGrade(report.overallScore)

  return (
    <div className="min-h-screen bg-white pb-32">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md px-5 pt-6 pb-4 md:px-8">
        <div className="mx-auto flex max-w-4xl items-center justify-between">
          <div className="flex items-center gap-2" onClick={() => navigate('/')} role="button">
            <Logo size={60} />
            <span className="text-lg font-black tracking-tight text-text-primary">면접 리포트</span>
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
        {/* 1. Hero Section — 원형 게이지 + 등급 */}
        <section className="text-center mb-16">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-8">
            Interview Result
          </p>

          <ScoreGauge score={report.overallScore} grade={grade} />

          <p className="mt-8 text-lg font-bold text-text-secondary leading-relaxed max-w-lg mx-auto tracking-tight">
            {report.summary}
          </p>

          <div className="mt-4 inline-flex items-center gap-1.5 text-xs font-bold text-text-tertiary">
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
            </svg>
            {report.feedbackCount}개 구간 분석 완료
          </div>
        </section>

        <div className="space-y-8">
          {/* 2. 강점 & 개선점 2컬럼 카드 */}
          <section className="grid gap-5 md:grid-cols-2">
            {/* 강점 */}
            <div className="rounded-2xl bg-emerald-50/50 p-7 border border-emerald-100">
              <div className="flex items-center gap-2 mb-5">
                <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-500 text-white text-xs font-black">
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                  </svg>
                </span>
                <h3 className="text-sm font-black tracking-wide text-emerald-700">강점</h3>
              </div>
              <ul className="space-y-3.5">
                {report.strengths.map((s, i) => (
                  <li key={i} className="flex gap-2.5 text-[15px] font-semibold text-text-primary leading-snug tracking-tight">
                    <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" />
                    {s}
                  </li>
                ))}
              </ul>
            </div>

            {/* 개선점 */}
            <div className="rounded-2xl bg-accent/5 p-7 border border-accent/10">
              <div className="flex items-center gap-2 mb-5">
                <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-accent text-white text-xs font-black">
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m0 0l6.75-6.75M12 19.5l-6.75-6.75" />
                  </svg>
                </span>
                <h3 className="text-sm font-black tracking-wide text-accent">보완점</h3>
              </div>
              <ul className="space-y-3.5">
                {report.improvements.map((s, i) => (
                  <li key={i} className="flex gap-2.5 text-[15px] font-semibold text-text-primary leading-snug tracking-tight">
                    <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-accent/60" />
                    {s}
                  </li>
                ))}
              </ul>
            </div>
          </section>

          {/* 3. 점수 인사이트 카드 */}
          <section className={`rounded-2xl p-7 border ${grade.bgColor}`}>
            <div className="flex items-start gap-3">
              <span className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-black ${grade.color} bg-white border`}>
                {grade.badge}
              </span>
              <div>
                <h3 className={`text-base font-extrabold tracking-tight ${grade.color}`}>
                  {grade.label}
                </h3>
                <p className="mt-2 text-sm font-medium text-text-secondary leading-relaxed">
                  {grade.message}
                </p>
                <p className="mt-3 text-sm font-bold text-text-primary leading-relaxed">
                  {grade.action}
                </p>
              </div>
            </div>
          </section>

          {/* 4. CTA 섹션 */}
          <section className="space-y-3 pt-4">
            <button
              onClick={() => navigate(`/interview/${interviewId}/feedback`)}
              className="h-14 w-full rounded-2xl bg-slate-900 font-bold text-white text-[15px] active:scale-[0.98] transition-transform"
            >
              타임스탬프 리뷰 보기
            </button>
            <button
              onClick={() => navigate('/')}
              className="h-14 w-full rounded-2xl bg-surface font-bold text-text-primary text-[15px] active:scale-[0.98] transition-transform"
            >
              새 면접 시작하기
            </button>
            <button
              onClick={() => navigate('/')}
              className="h-12 w-full rounded-2xl font-bold text-text-tertiary text-sm hover:text-text-secondary transition-colors"
            >
              홈으로
            </button>
          </section>
        </div>
      </main>
    </div>
  )
}

// --- 메인 페이지 ---

export const InterviewReportPage = () => {
  const { id } = useParams<{ id: string }>()
  const { report, reportStatus } = useReport(id ?? '')

  // 로딩 / 생성 중 상태
  if (reportStatus === 'loading' || reportStatus === 'generating') {
    const isGenerating = reportStatus === 'generating'
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white px-5">
        <div className="relative mb-10">
          <Character mood="thinking" size={160} />
          <div className="absolute -bottom-4 left-1/2 -translate-x-1/2 w-24 h-1.5 bg-slate-100 rounded-full overflow-hidden">
            <div className="h-full bg-accent animate-progress-loading" />
          </div>
        </div>
        <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary">
          {isGenerating ? 'AI가 리포트를 생성하고 있어요' : 'AI가 리포트를 분석하고 있어요'}
        </h1>
        <p className="mt-3 text-base font-medium text-text-secondary">
          {isGenerating ? '곧 완성됩니다, 잠시만 기다려주세요' : '잠시만 기다려주세요'}
        </p>
      </div>
    )
  }

  // 에러 상태
  if (reportStatus === 'error' || !report) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white px-5">
        <Character mood="confused" size={140} className="mb-8" />
        <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary text-center">
          리포트를 불러올 수<br />없습니다
        </h1>
        <button
          className="mt-10 h-16 w-full max-w-xs rounded-[24px] bg-accent font-black text-white active:scale-95"
          onClick={() => window.location.reload()}
        >
          다시 시도하기
        </button>
      </div>
    )
  }

  return <ReportContent report={report} interviewId={id ?? ''} />
}
