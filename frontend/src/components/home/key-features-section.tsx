import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

const features = [
  {
    label: 'TIMESTAMP FEEDBACK',
    title: '답변의 매 순간, 영상과 함께 피드백',
    description:
      '녹화된 영상의 특정 시점에 맞춰 피드백이 표시됩니다. 어떤 부분이 좋았고, 어디를 개선하면 좋을지 정확히 알 수 있어요.',
    mockup: 'timestamp',
  },
  {
    label: 'NONVERBAL ANALYSIS',
    title: '표정, 시선, 자세까지 분석합니다',
    description:
      'AI가 면접 영상을 분석해 비언어적 커뮤니케이션까지 피드백합니다. 내가 모르던 습관을 발견하세요.',
    mockup: 'nonverbal',
  },
  {
    label: 'AI QUESTIONS',
    title: 'CS · 직무 지식 · 이력서 맞춤 질문',
    description:
      '이력서 분석은 물론, 컴퓨터 CS 기초(자료구조, 네트워크 등)와 직무별 기술 지식(Java/Spring, 컴포넌트 설계 등)까지 실전 면접에서 나올 법한 질문을 생성합니다.',
    mockup: 'questions',
  },
] as const

const TimestampMockup = () => (
  <div className="space-y-3">
    <div className="rounded-2xl bg-surface border border-border p-4">
      <div className="flex items-center gap-2 mb-2">
        <span className="inline-flex items-center rounded-lg bg-accent/10 px-2 py-0.5 text-[10px] font-black text-accent">
          0:42
        </span>
        <span className="text-[10px] font-bold text-text-tertiary">답변 분석</span>
      </div>
      <p className="text-xs font-bold text-text-primary leading-relaxed">
        기술적 부채 해결 경험을 구체적으로 잘 설명하셨어요. STAR 기법이 자연스럽게 녹아있습니다.
      </p>
    </div>
    <div className="rounded-2xl bg-surface border border-border p-4">
      <div className="flex items-center gap-2 mb-2">
        <span className="inline-flex items-center rounded-lg bg-accent/10 px-2 py-0.5 text-[10px] font-black text-accent">
          1:15
        </span>
        <span className="inline-flex items-center rounded-md bg-amber-50 px-1.5 py-0.5 text-[9px] font-bold text-amber-600">
          개선 포인트
        </span>
      </div>
      <p className="text-xs font-bold text-text-primary leading-relaxed">
        이 구간에서 시선이 아래로 향했어요. 카메라를 바라보며 답변하면 자신감이 더 전달됩니다.
      </p>
    </div>
  </div>
)

const NonverbalMockup = () => (
  <div className="space-y-3">
    <div className="rounded-2xl bg-surface border border-border p-4">
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-bold text-text-primary">비언어 분석 결과</span>
      </div>
      <div className="space-y-3">
        {[
          { label: '시선 처리', score: 78, color: 'bg-accent' },
          { label: '표정 자연스러움', score: 85, color: 'bg-emerald-500' },
          { label: '자세 안정성', score: 62, color: 'bg-amber-500' },
        ].map((item) => (
          <div key={item.label}>
            <div className="flex items-center justify-between mb-1">
              <span className="text-[10px] font-bold text-text-secondary">{item.label}</span>
              <span className="text-[10px] font-black text-text-primary">{item.score}점</span>
            </div>
            <div className="h-1.5 rounded-full bg-border">
              <div
                className={`h-full rounded-full ${item.color}`}
                style={{ width: `${item.score}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
    <div className="flex gap-2">
      {['자신감 있는 표정', '안정적 자세', '시선 분산 주의'].map((tag) => (
        <span
          key={tag}
          className="rounded-lg bg-surface border border-border px-2 py-1 text-[9px] font-bold text-text-secondary"
        >
          {tag}
        </span>
      ))}
    </div>
  </div>
)

const QuestionsMockup = () => (
  <div className="space-y-3">
    {[
      { q: 'HashMap과 ConcurrentHashMap의 차이를 설명하고, 각각 언제 사용하는지 말씀해주세요.', type: 'CS 기초' },
      { q: 'Spring Boot에서 트랜잭션 전파 전략을 어떻게 설계하셨나요?', type: '직무 지식' },
      { q: '팀에서 코드 리뷰 프로세스를 개선한 경험이 있다면 말씀해주세요.', type: '이력서 기반' },
    ].map((item) => (
      <div key={item.q} className="rounded-2xl bg-surface border border-border p-4">
        <span className="inline-flex items-center rounded-md bg-accent/10 px-1.5 py-0.5 text-[9px] font-black text-accent mb-2">
          {item.type}
        </span>
        <p className="text-xs font-bold text-text-primary leading-relaxed">{item.q}</p>
      </div>
    ))}
  </div>
)

const mockupComponents = {
  timestamp: TimestampMockup,
  nonverbal: NonverbalMockup,
  questions: QuestionsMockup,
} as const

export const KeyFeaturesSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="bg-surface py-32"
      aria-labelledby="key-features-heading"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="text-center mb-16">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-4">
            KEY FEATURES
          </p>
          <h2
            id="key-features-heading"
            className="text-3xl font-extrabold tracking-tighter text-text-primary md:text-4xl"
          >
            리허설만의 차별화된 기능
          </h2>
        </div>

        <div className="space-y-20">
          {features.map((feature, index) => {
            const MockupComponent = mockupComponents[feature.mockup]
            const isReversed = index % 2 !== 0

            return (
              <div
                key={feature.label}
                className={`flex flex-col items-center gap-16 ${isReversed ? 'md:flex-row-reverse' : 'md:flex-row'}`}
              >
                <div className={`flex-1 space-y-4 ${isReversed ? 'md:text-right' : ''}`}>
                  <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent">
                    {feature.label}
                  </p>
                  <h3 className="text-2xl font-extrabold tracking-tight text-text-primary md:text-3xl">
                    {feature.title}
                  </h3>
                  <p className="text-lg font-medium text-text-secondary leading-relaxed">
                    {feature.description}
                  </p>
                </div>
                <div
                  className={`flex-1 w-full max-w-[440px] rounded-[32px] bg-white border border-border p-8 shadow-toss ${isReversed ? '-rotate-1' : 'rotate-1'}`}
                  aria-hidden="true"
                >
                  <MockupComponent />
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
