import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

const STEPS = [
  {
    number: 1,
    title: '면접 설정',
    description:
      '직무와 경력을 선택하고 이력서를 업로드하세요. AI가 CS 기초부터 직무 지식까지 맞춤 질문을 준비합니다.',
  },
  {
    number: 2,
    title: 'AI 면접 진행',
    description:
      '카메라와 마이크를 켜고 실전처럼 답변하세요. 자동으로 녹화됩니다.',
  },
  {
    number: 3,
    title: '상세 피드백 확인',
    description:
      '영상과 함께 타임스탬프별 피드백을 확인하고 개선점을 파악하세요.',
  },
] as const

export const HowItWorksSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="bg-white py-24"
      aria-labelledby="how-it-works-heading"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="mb-0">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-4">
            HOW IT WORKS
          </p>
          <h2
            id="how-it-works-heading"
            className="text-3xl font-extrabold tracking-tighter text-text-primary md:text-4xl"
          >
            3단계로 완성하는 면접 연습
          </h2>
        </div>

        <div className="relative mt-16 grid grid-cols-1 gap-8 md:grid-cols-3">
          {/* Desktop connector arrows */}
          <div
            className="absolute inset-0 hidden items-center justify-between md:flex"
            aria-hidden="true"
          >
            {/* First arrow: between card 1 and card 2 */}
            <div className="pointer-events-none absolute left-[calc(33.333%-12px)] top-1/2 flex -translate-y-1/2 items-center">
              <ConnectorArrow />
            </div>
            {/* Second arrow: between card 2 and card 3 */}
            <div className="pointer-events-none absolute left-[calc(66.666%-12px)] top-1/2 flex -translate-y-1/2 items-center">
              <ConnectorArrow />
            </div>
          </div>

          {STEPS.map((step) => (
            <article
              key={step.number}
              className="relative rounded-card border border-border bg-surface p-8 shadow-toss"
            >
              <div
                className="flex h-10 w-10 items-center justify-center rounded-2xl bg-accent font-black text-white"
                aria-label={`${step.number}단계`}
              >
                {step.number}
              </div>
              <h3 className="mt-4 text-xl font-extrabold text-text-primary">{step.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-text-secondary">{step.description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

const ConnectorArrow = () => (
  <svg
    width="24"
    height="16"
    viewBox="0 0 24 16"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    {/* Dashed line */}
    <line
      x1="0"
      y1="8"
      x2="18"
      y2="8"
      stroke="#C7D2FE"
      strokeWidth="1.5"
      strokeDasharray="3 3"
    />
    {/* Arrowhead */}
    <path
      d="M16 4L22 8L16 12"
      stroke="#C7D2FE"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      fill="none"
    />
  </svg>
)
