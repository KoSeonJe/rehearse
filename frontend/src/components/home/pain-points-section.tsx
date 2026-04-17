import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface PainPoint {
  title: string
  quote: string
  solution: string
  icon: React.ReactNode
}

const ScoreIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="3" y="12" width="4" height="8" rx="1" stroke="#6366F1" strokeWidth="2" />
    <rect x="10" y="7" width="4" height="13" rx="1" stroke="#6366F1" strokeWidth="2" />
    <rect x="17" y="3" width="4" height="17" rx="1" stroke="#6366F1" strokeWidth="2" />
  </svg>
)

const SpeechIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M12 2v8" stroke="#6366F1" strokeWidth="2" strokeLinecap="round" />
    <rect x="9" y="2" width="6" height="12" rx="3" stroke="#6366F1" strokeWidth="2" />
    <path d="M5 11a7 7 0 0 0 14 0" stroke="#6366F1" strokeWidth="2" strokeLinecap="round" />
    <path d="M12 18v3" stroke="#6366F1" strokeWidth="2" strokeLinecap="round" />
  </svg>
)

const AnswerIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" stroke="#6366F1" strokeWidth="2" strokeLinejoin="round" />
    <circle cx="9" cy="11.5" r="1" fill="#6366F1" />
    <circle cx="13" cy="11.5" r="1" fill="#6366F1" />
    <circle cx="17" cy="11.5" r="1" fill="#6366F1" />
  </svg>
)

const PAIN_POINTS: PainPoint[] = [
  {
    title: '종합 분석만 해준다',
    quote: '총평은 주는데, 어느 답변이 약했고 어느 부분을 고쳐야 하는지는 알 수 없어요.',
    solution:
      '답변 한 건 한 건을 "잘한 점 · 아쉬운 점 · 이렇게 말하면 더 좋아요" 3가지로 쪼개 보여드립니다.',
    icon: <ScoreIcon />,
  },
  {
    title: '말투·습관어를 놓친다',
    quote: '"어…", "그…" 같은 습관어를 내가 얼마나 쓰는지 체감이 안 돼요.',
    solution:
      '답변 구간별로 습관어 횟수·말하기 속도·기술적 정확도까지 STT로 분석해 표시합니다.',
    icon: <SpeechIcon />,
  },
  {
    title: '텍스트 피드백만으로는 부족하다',
    quote: '답변 텍스트에 대한 코멘트만 있어서, 실제 내 말투·표정이 어땠는지는 알 수 없어요.',
    solution:
      '피드백마다 영상 타임스탬프가 연결돼, 클릭하면 그 순간 영상으로 이동해 말투·표정까지 함께 복기할 수 있습니다.',
    icon: <AnswerIcon />,
  },
]

const PainCard = ({ title, quote, solution, icon }: PainPoint) => (
  <article className="rounded-[20px] bg-white border border-border p-6 shadow-sm">
    <div className="h-10 w-10 rounded-2xl bg-violet-legacy/10 flex items-center justify-center mb-4">
      {icon}
    </div>
    <h3 className="text-lg font-extrabold text-text-primary mb-3">{title}</h3>
    <blockquote className="text-sm font-medium text-text-secondary italic leading-relaxed border-l-2 border-violet-legacy/20 pl-3">
      "{quote}"
    </blockquote>
    <div className="border-t border-border my-4" />
    <p className="text-sm font-bold text-violet-legacy leading-relaxed">{solution}</p>
  </article>
)

export const PainPointsSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="pain-heading"
      className="bg-surface py-20 md:py-28"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="mb-14 text-center">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-violet-legacy mb-4">
            WHY REHEARSE
          </p>
          <h2
            id="pain-heading"
            className="text-3xl md:text-4xl font-extrabold tracking-tighter text-text-primary"
          >
            면접 준비, 이런 점이 아쉽지 않으셨나요?
          </h2>
          <p className="mt-4 text-base font-medium text-text-secondary">
            점수와 총평만으로는 다음 면접에서 무엇을 바꿔야 할지 알기 어렵습니다.
          </p>
        </div>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          {PAIN_POINTS.map((item) => (
            <PainCard key={item.title} {...item} />
          ))}
        </div>
      </div>
    </section>
  )
}
