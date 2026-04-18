import type { ReactNode } from 'react'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface MetricItem {
  label: string
  question: string
  icon: ReactNode
}

// ── Icons ──────────────────────────────────────────────────────────────
const iconProps = {
  width: 16,
  height: 16,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
}

const CheckIcon = () => (
  <svg {...iconProps}>
    <path d="m9 12 2 2 4-4" />
    <circle cx="12" cy="12" r="9" />
  </svg>
)

const CodeIcon = () => (
  <svg {...iconProps}>
    <path d="m8 6-6 6 6 6" />
    <path d="m16 6 6 6-6 6" />
  </svg>
)

const FilterIcon = () => (
  <svg {...iconProps}>
    <path d="M21 5H3l7 8v6l4 2v-8l7-8z" />
  </svg>
)

const StopwatchIcon = () => (
  <svg {...iconProps}>
    <circle cx="12" cy="14" r="7" />
    <path d="M12 10v4l2 2" />
    <path d="M9 2h6" />
  </svg>
)

const PostureIcon = () => (
  <svg {...iconProps}>
    <circle cx="12" cy="5" r="2.5" />
    <path d="M12 8v8" />
    <path d="M7 21l5-5 5 5" />
    <path d="M9 12h6" />
  </svg>
)

const SmileIcon = () => (
  <svg {...iconProps}>
    <circle cx="12" cy="12" r="9" />
    <path d="M8 14s1.5 2 4 2 4-2 4-2" />
    <path d="M9 9h.01M15 9h.01" />
  </svg>
)

const EyeIcon = () => (
  <svg {...iconProps}>
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
)

const MicIcon = () => (
  <svg {...iconProps}>
    <rect x="9" y="3" width="6" height="11" rx="3" />
    <path d="M5 11a7 7 0 0 0 14 0" />
    <path d="M12 18v3" />
  </svg>
)

const SparkleIcon = () => (
  <svg {...iconProps}>
    <path d="M12 3v4M12 17v4M3 12h4M17 12h4" />
    <path d="M12 8a4 4 0 0 0 4 4 4 4 0 0 0-4 4 4 4 0 0 0-4-4 4 4 0 0 0 4-4z" />
  </svg>
)

// ── Metric data ────────────────────────────────────────────────────────
const VERBAL_METRICS: MetricItem[] = [
  { label: '답변 완성도', question: '질문에 얼마나 정확히 답했는가?', icon: <CheckIcon /> },
  { label: '기술적 정확도', question: '기술 용어나 개념에 오류가 있었는가?', icon: <CodeIcon /> },
  { label: '반복사 횟수', question: '어, 음, 그 같은 반복사가 몇 개?', icon: <FilterIcon /> },
  { label: '말하기 속도', question: '빨랐나, 느렸나, 적당했나?', icon: <StopwatchIcon /> },
]

const NONVERBAL_METRICS: MetricItem[] = [
  { label: '자세 안정성', question: '몸이 흔들렸거나 웅크렸나?', icon: <PostureIcon /> },
  { label: '표정 변화', question: '표정이 딱딱했나, 자연스러웠나?', icon: <SmileIcon /> },
  { label: '시선 처리', question: '카메라를 얼마나 바라봤나?', icon: <EyeIcon /> },
  { label: '음성 자신감', question: '목소리가 떨렸나, 확신감 있나?', icon: <MicIcon /> },
  { label: '감정 표현', question: '긍정적/중립적/불안했나?', icon: <SparkleIcon /> },
]

// ── Row item — hairline 구분, 박스 없음 ───────────────────────────────
const MetricRow = ({ label, question, icon }: MetricItem) => (
  <li className="flex items-start gap-4 py-4 border-b border-foreground/8 last:border-none">
    <span className="flex-shrink-0 mt-0.5 text-muted-foreground">{icon}</span>
    <div className="min-w-0">
      <p className="text-sm font-bold text-foreground">{label}</p>
      <p className="text-xs font-medium text-muted-foreground mt-0.5 leading-relaxed">{question}</p>
    </div>
  </li>
)

export const MetricsSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="metrics-heading"
      className="bg-background py-20 md:py-28"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">

        {/* 헤딩 — 좌정렬 editorial */}
        <div className="mb-12 border-t border-foreground/10 pt-10">
          <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.1em] text-muted-foreground mb-4">
            FEEDBACK METRICS
          </p>
          <h2
            id="metrics-heading"
            className="text-3xl md:text-[2.75rem] font-bold leading-[1.1] tracking-[-0.02em] text-foreground"
          >
            답변의 매 순간을<br className="hidden md:block" />
            9가지 지표로 분석합니다
          </h2>
        </div>

        {/* 지표 — 2열 hairline 리스트 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-16">

          <div role="group" aria-label="언어 피드백 지표">
            <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.08em] text-muted-foreground mb-2">
              언어 피드백
            </p>
            <ul aria-label="언어 피드백 지표 목록">
              {VERBAL_METRICS.map((m) => (
                <MetricRow key={m.label} {...m} />
              ))}
            </ul>
          </div>

          <div role="group" aria-label="비언어 피드백 지표">
            <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.08em] text-muted-foreground mb-2 mt-10 md:mt-0">
              비언어 피드백
            </p>
            <ul aria-label="비언어 피드백 지표 목록">
              {NONVERBAL_METRICS.map((m) => (
                <MetricRow key={m.label} {...m} />
              ))}
            </ul>
          </div>

        </div>
      </div>
    </section>
  )
}
