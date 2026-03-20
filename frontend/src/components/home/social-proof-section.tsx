import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

const FEATURES = [
  {
    keyword: 'AI 맞춤 질문',
    description: '이력서 · CS · 직무 지식 기반',
  },
  {
    keyword: '영상 타임스탬프 피드백',
    description: '답변의 매 순간을 분석',
  },
  {
    keyword: '비언어 분석',
    description: '표정, 시선, 자세까지',
  },
] as const

export const SocialProofSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="border-y border-border bg-surface py-12"
      aria-label="주요 기능 요약"
    >
      <div className="mx-auto grid max-w-5xl grid-cols-1 gap-8 px-5 md:grid-cols-3 md:px-8">
        {FEATURES.map((feature, index) => (
          <div
            key={feature.keyword}
            className="relative flex flex-col items-center text-center md:items-start md:text-left"
          >
            <p className="text-2xl font-extrabold text-accent">{feature.keyword}</p>
            <p className="mt-1 text-sm text-text-secondary">{feature.description}</p>

            {index < FEATURES.length - 1 && (
              <div
                className="absolute -right-4 top-1/2 hidden h-8 w-px -translate-y-1/2 bg-border md:block"
                aria-hidden="true"
              />
            )}
          </div>
        ))}
      </div>
    </section>
  )
}
