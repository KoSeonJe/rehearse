import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface JourneyStep {
  number: number
  title: string
  description: string
  imageSrc: string
  imageAlt: string
  rotate: string
}

const JOURNEY_STEPS: JourneyStep[] = [
  {
    number: 1,
    title: '나만을 위한 면접 설정',
    description: '직무·경력·이력서를 선택하세요.',
    imageSrc: '/images/landing/journey-setup.png',
    imageAlt: '면접 설정 화면 — 직무와 경력을 선택하는 UI',
    rotate: 'rotate-2',
  },
  {
    number: 2,
    title: '실전처럼 답변하고 녹화',
    description: '카메라·마이크로 실전 면접처럼 녹화합니다.',
    imageSrc: '/images/landing/journey-interview.png',
    imageAlt: '면접 진행 화면 — 카메라와 마이크로 녹화 중인 UI',
    rotate: '-rotate-1',
  },
  {
    number: 3,
    title: '매 순간의 타임스탬프 피드백',
    description: '영상·마커·패널이 동시에 연결됩니다.',
    imageSrc: '/images/landing/journey-feedback.png',
    imageAlt: '피드백 화면 — 타임스탬프 마커와 피드백 패널이 연결된 UI',
    rotate: 'rotate-2',
  },
  {
    number: 4,
    title: '개선점을 찾고 다시 도전',
    description: '피드백을 정리하고 다음 면접을 준비하세요.',
    imageSrc: '/images/landing/journey-retry.png',
    imageAlt: '복기 화면 — 개선점을 확인하고 재도전하는 UI',
    rotate: '-rotate-1',
  },
]

interface StepImageProps {
  step: JourneyStep
  isReversed: boolean
}

const StepImage = ({ step, isReversed }: StepImageProps) => (
  <div
    className={`flex-1 w-full max-w-[440px] rounded-[32px] bg-surface border border-border shadow-toss p-4 ${step.rotate} ${isReversed ? 'md:mr-auto' : 'md:ml-auto'}`}
    aria-hidden="true"
  >
    <div className="relative rounded-[24px] overflow-hidden bg-white border border-slate-100">
      <img
        src={step.imageSrc}
        alt={step.imageAlt}
        loading="lazy"
        className="block w-full h-auto"
      />
    </div>
  </div>
)

interface StepTextProps {
  step: JourneyStep
  isReversed: boolean
}

const StepText = ({ step, isReversed }: StepTextProps) => (
  <div className={`flex-1 space-y-4 ${isReversed ? 'md:text-right' : ''}`}>
    <div
      className={`inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-violet-legacy text-white font-black text-sm ${isReversed ? 'md:ml-auto' : ''}`}
      aria-label={`${step.number}단계`}
    >
      {step.number}
    </div>
    <h3 className="text-2xl font-extrabold tracking-tight text-text-primary md:text-3xl">
      {step.title}
    </h3>
    <p className="text-base font-medium text-text-secondary leading-relaxed">
      {step.description}
    </p>
  </div>
)

export const JourneySection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="journey-heading"
      className="bg-white py-24 md:py-32"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">

        {/* 헤딩 */}
        <div className="mb-20 text-center">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-violet-legacy mb-4">
            THE JOURNEY
          </p>
          <h2
            id="journey-heading"
            className="text-3xl md:text-4xl font-extrabold tracking-tighter text-text-primary"
          >
            당신의 면접 여정, 4단계
          </h2>
        </div>

        {/* 단계 목록 */}
        <div className="space-y-24 md:space-y-32">
          {JOURNEY_STEPS.map((step, idx) => {
            const isReversed = idx % 2 !== 0
            return (
              <article
                key={step.number}
                className={`flex flex-col gap-10 md:flex-row md:items-center md:gap-16 ${isReversed ? 'md:flex-row-reverse' : ''}`}
              >
                <StepText step={step} isReversed={isReversed} />
                <StepImage step={step} isReversed={isReversed} />
              </article>
            )
          })}
        </div>

      </div>
    </section>
  )
}
