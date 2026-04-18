import type { ReactNode } from 'react'
import { Card } from '@/components/ui/card'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface MetricItem {
  label: string
  question: string
  icon: ReactNode
}

// ── Icons ──────────────────────────────────────────────────────────────
const iconProps = {
  width: 20,
  height: 20,
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
  {
    label: '답변 완성도',
    question: '질문에 얼마나 정확히 답했는가?',
    icon: <CheckIcon />,
  },
  {
    label: '기술적 정확도',
    question: '기술 용어나 개념에 오류가 있었는가?',
    icon: <CodeIcon />,
  },
  {
    label: '반복사 횟수',
    question: '어, 음, 그 같은 반복사가 몇 개?',
    icon: <FilterIcon />,
  },
  {
    label: '말하기 속도',
    question: '빨랐나, 느렸나, 적당했나?',
    icon: <StopwatchIcon />,
  },
]

const NONVERBAL_METRICS: MetricItem[] = [
  {
    label: '자세 안정성',
    question: '몸이 흔들렸거나 웅크렸나?',
    icon: <PostureIcon />,
  },
  {
    label: '표정 변화',
    question: '표정이 딱딱했나, 자연스러웠나?',
    icon: <SmileIcon />,
  },
  {
    label: '시선 처리',
    question: '카메라를 얼마나 바라봤나?',
    icon: <EyeIcon />,
  },
  {
    label: '음성 자신감',
    question: '목소리가 떨렸나, 확신감 있나?',
    icon: <MicIcon />,
  },
  {
    label: '감정 표현',
    question: '긍정적/중립적/불안했나?',
    icon: <SparkleIcon />,
  },
]

// ── Card ───────────────────────────────────────────────────────────────
const MetricCard = ({ label, question, icon }: MetricItem) => (
  <Card
    className="group border border-border bg-background p-5 transition-colors duration-200 hover:-translate-y-1 hover:border-text-tertiary hover:shadow-md"
    role="article"
  >
    <div className="h-10 w-10 rounded-xl flex items-center justify-center mb-3 bg-secondary text-text-primary transition-colors duration-200 group-hover:bg-text-primary group-hover:text-white">
      {icon}
    </div>
    <h3 className="text-sm font-extrabold text-text-primary mb-1">{label}</h3>
    <p className="text-xs font-medium text-text-secondary leading-relaxed">{question}</p>
  </Card>
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

        {/* 헤딩 */}
        <div className="text-center mb-14">
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4">
            FEEDBACK METRICS
          </p>
          <h2
            id="metrics-heading"
            className="text-3xl md:text-4xl font-extrabold tracking-tighter text-text-primary"
          >
            답변의 매 순간을 분석하는 9가지 지표
          </h2>
        </div>

        {/* 지표 컨테이너 — 언어/비언어를 한 배경 안에 함께 감쌈 */}
        <div className="rounded-3xl bg-secondary p-6 md:p-8">
          {/* 언어 피드백 그룹 */}
          <div className="mb-8" role="group" aria-label="언어 피드백 지표">
            <div className="flex items-center gap-3 mb-6">
              <div className="h-px flex-1 bg-border" />
              <span className="text-xs font-bold text-text-tertiary uppercase tracking-wider">언어 피드백</span>
              <div className="h-px flex-1 bg-border" />
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-4">
              {VERBAL_METRICS.map((m) => (
                <MetricCard key={m.label} {...m} />
              ))}
            </div>
          </div>

          {/* 비언어 피드백 그룹 */}
          <div role="group" aria-label="비언어 피드백 지표">
            <div className="flex items-center gap-3 mb-6">
              <div className="h-px flex-1 bg-border" />
              <span className="text-xs font-bold text-text-tertiary uppercase tracking-wider">비언어 피드백</span>
              <div className="h-px flex-1 bg-border" />
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
              {NONVERBAL_METRICS.map((m) => (
                <MetricCard key={m.label} {...m} />
              ))}
            </div>
          </div>
        </div>

      </div>
    </section>
  )
}
