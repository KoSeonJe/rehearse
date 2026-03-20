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

// 면접자 상반신 실루엣 — 웹캠 앞에 앉아 살짝 미소짓는 모습
const PersonSilhouette = () => (
  <svg
    viewBox="0 0 120 144"
    fill="none"
    aria-hidden="true"
    className="w-20 h-24"
  >
    {/* 목 */}
    <path
      d="M54 58 C54 62 52 66 52 70 L68 70 C68 66 66 62 66 58 Z"
      fill="white"
      opacity="0.55"
    />

    {/* 얼굴 형태 — 약간 갸름한 타원 */}
    <ellipse cx="60" cy="38" rx="18" ry="22" fill="white" opacity="0.75" />

    {/* 머리카락 — 자연스러운 윗부분 */}
    <path
      d="M42 34 C42 20 45 14 60 13 C75 14 78 20 78 34 C76 28 72 24 60 24 C48 24 44 28 42 34 Z"
      fill="white"
      opacity="0.45"
    />

    {/* 귀 — 좌 */}
    <ellipse cx="42" cy="38" rx="3.5" ry="5" fill="white" opacity="0.55" />
    {/* 귀 — 우 */}
    <ellipse cx="78" cy="38" rx="3.5" ry="5" fill="white" opacity="0.55" />

    {/* 눈썹 — 좌 */}
    <path
      d="M50 28 C52 26.5 56 26 58 26.5"
      stroke="white"
      strokeOpacity="0.5"
      strokeWidth="1.8"
      strokeLinecap="round"
    />
    {/* 눈썹 — 우 */}
    <path
      d="M62 26.5 C64 26 68 26.5 70 28"
      stroke="white"
      strokeOpacity="0.5"
      strokeWidth="1.8"
      strokeLinecap="round"
    />

    {/* 눈 — 좌 (아몬드형) */}
    <path
      d="M50 33 C52 31 56 31 58 33 C56 35.5 52 35.5 50 33 Z"
      fill="white"
      opacity="0.35"
    />
    <ellipse cx="54" cy="33" rx="2.2" ry="2.5" fill="white" opacity="0.6" />

    {/* 눈 — 우 (아몬드형) */}
    <path
      d="M62 33 C64 31 68 31 70 33 C68 35.5 64 35.5 62 33 Z"
      fill="white"
      opacity="0.35"
    />
    <ellipse cx="66" cy="33" rx="2.2" ry="2.5" fill="white" opacity="0.6" />

    {/* 코 — 섬세한 콧날 */}
    <path
      d="M58 36 C58 41 56 43.5 56 44.5 C57.5 45.5 62.5 45.5 64 44.5 C64 43.5 62 41 62 36"
      stroke="white"
      strokeOpacity="0.3"
      strokeWidth="1.2"
      strokeLinecap="round"
      strokeLinejoin="round"
      fill="none"
    />

    {/* 입 — 살짝 미소 (양끝이 올라간 곡선) */}
    <path
      d="M53 50 C55.5 52.5 60 53 64.5 52 C66.5 51.5 67.5 50.5 67 50"
      stroke="white"
      strokeOpacity="0.55"
      strokeWidth="1.6"
      strokeLinecap="round"
      fill="none"
    />
    {/* 윗입술 중앙 (큐피드 활) */}
    <path
      d="M55 49.5 C57 48.5 60 48 63 49 C65 49.5 67 50 67 50"
      stroke="white"
      strokeOpacity="0.3"
      strokeWidth="1.2"
      strokeLinecap="round"
      fill="none"
    />

    {/* 어깨 + 상체 — 자연스러운 드롭 숄더 */}
    <path
      d="M52 70 C46 71 36 74 24 82 C18 86 16 92 16 100 L104 100 C104 92 102 86 96 82 C84 74 74 71 68 70 Z"
      fill="white"
      opacity="0.55"
    />

    {/* 셔츠 칼라 — V넥 오른쪽 */}
    <path
      d="M60 72 L68 70 C70 72 72 76 70 80 Z"
      fill="white"
      opacity="0.7"
    />
    {/* 셔츠 칼라 — V넥 왼쪽 */}
    <path
      d="M60 72 L52 70 C50 72 48 76 50 80 Z"
      fill="white"
      opacity="0.7"
    />
    {/* 칼라 중앙 V 라인 */}
    <path
      d="M52 70 C55 74 58 78 60 82 C62 78 65 74 68 70"
      stroke="white"
      strokeOpacity="0.4"
      strokeWidth="1.2"
      strokeLinecap="round"
      fill="none"
    />

    {/* 어깨 윤곽 강조 — 좌 */}
    <path
      d="M52 70 C44 72 32 77 24 82"
      stroke="white"
      strokeOpacity="0.3"
      strokeWidth="1"
      strokeLinecap="round"
      fill="none"
    />
    {/* 어깨 윤곽 강조 — 우 */}
    <path
      d="M68 70 C76 72 88 77 96 82"
      stroke="white"
      strokeOpacity="0.3"
      strokeWidth="1"
      strokeLinecap="round"
      fill="none"
    />
  </svg>
)

interface FeedbackItem {
  time: string
  label: string
  labelClass: string
  markerClass: string
  timelinePos: string
  text: string
}

const TIMESTAMP_FEEDBACKS: FeedbackItem[] = [
  {
    time: '0:42',
    label: '잘한 점',
    labelClass: 'bg-accent/10 text-accent',
    markerClass: 'bg-accent',
    timelinePos: '28%',
    text: '기술적 부채 해결 경험을 구체적으로 잘 설명하셨어요. STAR 기법이 자연스럽게 녹아있습니다.',
  },
  {
    time: '1:15',
    label: '개선 포인트',
    labelClass: 'bg-amber-50 text-amber-600',
    markerClass: 'bg-amber-400',
    timelinePos: '55%',
    text: '이 구간에서 시선이 아래로 향했어요. 카메라를 바라보며 답변하면 자신감이 더 전달됩니다.',
  },
]

const TimestampMockup = () => (
  <div className="space-y-3">
    {/* 웹캠 영역 */}
    <div className="rounded-2xl overflow-hidden border border-border">
      {/* 카메라 뷰 */}
      <div className="relative bg-gradient-to-b from-[#16213e] to-[#0f3460] h-36 flex items-center justify-center overflow-hidden">
        {/* REC 배지 */}
        <div className="absolute top-2.5 left-3 flex items-center gap-1.5 z-10">
          <span className="inline-block w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
          <span className="text-[9px] font-black uppercase tracking-widest text-white/50">
            REC
          </span>
        </div>

        {/* 시간 표시 */}
        <div className="absolute top-2.5 right-3 z-10">
          <span className="font-mono text-[10px] font-bold text-white/50">
            00:42 / 03:00
          </span>
        </div>

        {/* 코너 브라켓 */}
        <div className="absolute top-2 right-2 w-4 h-4 border-t-2 border-r-2 border-white/15 rounded-tr" />
        <div className="absolute bottom-2 left-2 w-4 h-4 border-b-2 border-l-2 border-white/15 rounded-bl" />

        {/* 사람 실루엣 */}
        <PersonSilhouette />
      </div>

      {/* 타임라인 바 */}
      <div className="bg-white px-3 pt-2.5 pb-2">
        <div className="relative h-1 rounded-full bg-border">
          {/* 진행 채움 */}
          <div className="absolute left-0 top-0 h-full w-[28%] rounded-full bg-accent/30" />

          {/* 피드백 마커 */}
          {TIMESTAMP_FEEDBACKS.map((fb) => (
            <div
              key={fb.time}
              className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2"
              style={{ left: fb.timelinePos }}
            >
              <span
                className={`block w-2.5 h-2.5 rounded-full border-2 border-white shadow-sm ${fb.markerClass}`}
              />
            </div>
          ))}

          {/* 재생 헤드 */}
          <div
            className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2"
            style={{ left: '28%' }}
          >
            <span className="block w-3 h-3 rounded-full bg-accent border-2 border-white shadow" />
          </div>
        </div>
        <div className="flex justify-between mt-1">
          <span className="font-mono text-[9px] text-text-tertiary">0:00</span>
          <span className="font-mono text-[9px] text-text-tertiary">3:00</span>
        </div>
      </div>
    </div>

    {/* 피드백 카드 목록 */}
    <div className="space-y-2">
      {TIMESTAMP_FEEDBACKS.map((fb) => (
        <div
          key={fb.time}
          className="rounded-2xl bg-surface border border-border p-3.5 flex gap-3"
        >
          {/* 마커 도트 */}
          <span
            className={`inline-block w-2 h-2 rounded-full shrink-0 mt-0.5 ${fb.markerClass}`}
            aria-hidden="true"
          />
          <div className="min-w-0">
            <div className="flex items-center gap-1.5 mb-1.5">
              <span className="font-mono text-[10px] font-black text-text-secondary">
                {fb.time}
              </span>
              <span
                className={`inline-flex items-center rounded-md px-1.5 py-0.5 text-[9px] font-bold ${fb.labelClass}`}
              >
                {fb.label}
              </span>
            </div>
            <p className="text-xs font-medium text-text-primary leading-relaxed">
              {fb.text}
            </p>
          </div>
        </div>
      ))}
    </div>
  </div>
)

interface NonverbalFeedback {
  category: string
  categoryClass: string
  text: string
}

const NONVERBAL_FEEDBACKS: NonverbalFeedback[] = [
  {
    category: '시선',
    categoryClass: 'bg-accent/10 text-accent',
    text: '0:42~1:15 구간에서 시선이 화면 아래로 자주 향했습니다. 카메라를 바라보며 답변하면 자신감이 더 전달됩니다.',
  },
  {
    category: '자세',
    categoryClass: 'bg-amber-50 text-amber-600',
    text: '전반적으로 안정적인 자세를 유지했으나, 2:30 이후 어깨가 점차 앞으로 기울어졌습니다.',
  },
  {
    category: '표정',
    categoryClass: 'bg-emerald-50 text-emerald-600',
    text: '답변 시작 시 자연스러운 미소가 좋은 인상을 줍니다.',
  },
]

const NonverbalMockup = () => (
  <div className="space-y-3">
    <div className="rounded-2xl bg-surface border border-border p-4">
      <span className="text-xs font-bold text-text-primary">비언어 분석 결과</span>
    </div>
    <div className="space-y-2">
      {NONVERBAL_FEEDBACKS.map((fb) => (
        <div
          key={fb.category}
          className="rounded-2xl bg-surface border border-border p-3.5"
        >
          <span
            className={`inline-flex items-center rounded-md px-1.5 py-0.5 text-[9px] font-bold mb-2 ${fb.categoryClass}`}
          >
            {fb.category}
          </span>
          <p className="text-xs font-medium text-text-primary leading-relaxed">{fb.text}</p>
        </div>
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
