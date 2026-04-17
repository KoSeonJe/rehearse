import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface QuestionItem {
  badge: string
  badgeBg: string
  badgeText: string
  question: string
}

const QUESTION_ITEMS: QuestionItem[] = [
  {
    badge: 'CS 기초',
    badgeBg: 'bg-blue-100',
    badgeText: 'text-blue-600',
    question:
      'HashMap과 ConcurrentHashMap의 차이를 설명하고, 각각 언제 사용하는지 말씀해주세요.',
  },
  {
    badge: '직무 지식',
    badgeBg: 'bg-secondary',
    badgeText: 'text-text-secondary',
    question: 'Spring Boot에서 트랜잭션 전파 전략을 어떻게 설계하셨나요?',
  },
  {
    badge: '이력서 기반',
    badgeBg: 'bg-emerald-100',
    badgeText: 'text-emerald-700',
    question:
      '팀에서 코드 리뷰 프로세스를 개선한 경험이 있다면 말씀해주세요.',
  },
]

const BULLET_ITEMS = ['CS 기초 질문', '직무 지식 질문', '이력서 기반 질문']

export const DevTailoredSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="devtailored-heading"
      className="bg-surface py-20 md:py-28"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="flex flex-col gap-12 md:flex-row md:items-center md:gap-16">

          {/* 좌측: 텍스트 */}
          <div className="flex-1">
            <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-violet-legacy mb-4">
              DEVELOPER FOCUSED
            </p>
            <h2
              id="devtailored-heading"
              className="text-2xl md:text-3xl font-extrabold tracking-tighter text-text-primary leading-snug"
            >
              개발자 이력서로<br />
              CS·직무·설계 질문을 생성합니다
            </h2>
            <p className="mt-4 text-base font-medium text-text-secondary leading-relaxed">
              이력서를 업로드하면, 실제 면접에서 나올 법한
              질문 3가지 유형으로 구성됩니다.
            </p>
            <ul className="mt-4 space-y-2">
              {BULLET_ITEMS.map((item) => (
                <li
                  key={item}
                  className="flex items-center gap-2 text-sm font-medium text-text-secondary"
                >
                  <span className="h-1.5 w-1.5 rounded-full bg-text-tertiary shrink-0" />
                  {item}
                </li>
              ))}
            </ul>
          </div>

          {/* 우측: 질문 목업 카드 (장식적) */}
          <div className="flex-1 flex justify-center md:justify-end" aria-hidden="true">
            <div className="w-full max-w-[480px] rounded-[32px] bg-white border border-border shadow-toss -rotate-1 p-7">

              {/* 질문 카드 3개 */}
              <div className="space-y-3">
                {QUESTION_ITEMS.map((item) => (
                  <div
                    key={item.badge}
                    className="rounded-[20px] bg-surface border border-border p-4"
                  >
                    <span
                      className={`inline-flex items-center rounded-md px-1.5 py-0.5 text-[9px] font-black mb-2 ${item.badgeBg} ${item.badgeText}`}
                    >
                      {item.badge}
                    </span>
                    <p className="text-xs font-bold text-text-primary leading-relaxed">
                      {item.question}
                    </p>
                  </div>
                ))}
              </div>

              {/* 심화 질문 예시 */}
              <div className="border-t border-dashed border-border mt-4 pt-4">
                <p className="text-xs font-medium italic text-text-tertiary leading-relaxed">
                  → "그럼 Redis 캐시 전략에서 TTL을 어떻게 설정하셨나요?" — 후속 질문 예시
                </p>
              </div>

            </div>
          </div>

        </div>
      </div>
    </section>
  )
}
