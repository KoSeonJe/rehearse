import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Logo } from '@/components/ui/logo'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

const DemoFeedbackItem = ({
  time,
  label,
  category,
  content,
}: {
  time: string
  label: string
  category: 'verbal' | 'nonverbal' | 'content'
  content: string
}) => {
  const categoryColor = {
    verbal: 'border-l-text-primary',
    nonverbal: 'border-l-info',
    content: 'border-l-success',
  }[category]

  return (
    <div className={`border-l-2 ${categoryColor} py-2 pl-3`}>
      <div className="flex items-center gap-2">
        <span className="font-mono text-xs text-text-tertiary">{time}</span>
        <span className="rounded-badge bg-accent-light px-2 py-0.5 font-mono text-[10px] font-medium uppercase tracking-wider text-text-secondary">
          {label}
        </span>
      </div>
      <p className="mt-1 text-sm leading-relaxed text-text-secondary">{content}</p>
    </div>
  )
}

export const HomePage = () => {
  const navigate = useNavigate()
  const { ref: heroRef, style: heroStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: demoRef, style: demoStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: featuresRef, style: featuresStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: ctaRef, style: ctaStyle } = useFadeInOnScroll<HTMLElement>()

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-border bg-surface/80 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4 sm:px-6">
          <div className="flex items-center gap-2">
            <Logo size={80} />
            <span className="text-lg font-bold text-text-primary">
              리허설
            </span>
          </div>
          <nav aria-label="메인 네비게이션">
            <Button variant="primary" className="px-4 py-2 text-sm" onClick={() => navigate('/interview/setup')}>
              시작하기
            </Button>
          </nav>
        </div>
      </header>

      <main>
        {/* Hero — 짧고 강렬하게 */}
        <section
          ref={heroRef}
          style={heroStyle}
          className="px-4 pb-16 pt-20 sm:px-6 sm:pb-24 sm:pt-28 md:pt-36"
        >
          <div className="mx-auto max-w-3xl text-center">
            <p className="font-mono text-xs uppercase tracking-widest text-text-tertiary">
              ai mock interview for developers
            </p>
            <h1 className="mt-6 text-4xl font-extralight tracking-tight text-text-primary sm:text-5xl md:text-6xl">
              면접, <span className="font-extrabold">다시 한 번.</span>
            </h1>
            <p className="mx-auto mt-6 max-w-md text-base leading-relaxed text-text-secondary">
              AI가 면접을 녹화하고 분석합니다.
              타임스탬프 기반 피드백으로 정확히 어디를 고쳐야 하는지 알려드립니다.
            </p>
            <div className="mt-10">
              <Button variant="cta" className="w-full sm:w-auto" onClick={() => navigate('/interview/setup')}>
                면접 시작하기
              </Button>
            </div>
          </div>
        </section>

        {/* Product Demo — 서비스 차별점을 직접 보여줌 */}
        <section
          ref={demoRef}
          style={demoStyle}
          className="px-4 pb-16 sm:px-6 sm:pb-24"
        >
          <div className="mx-auto max-w-4xl">
            <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-sm">
              {/* 브라우저 탑바 */}
              <div className="flex items-center gap-2 border-b border-border px-4 py-3">
                <div className="flex gap-1.5">
                  <div className="h-2.5 w-2.5 rounded-full bg-border" />
                  <div className="h-2.5 w-2.5 rounded-full bg-border" />
                  <div className="h-2.5 w-2.5 rounded-full bg-border" />
                </div>
                <div className="ml-3 flex-1 rounded-md bg-accent-light px-3 py-1">
                  <span className="font-mono text-[11px] text-text-tertiary">
                    rehearse.app/interview/1/review
                  </span>
                </div>
              </div>

              {/* 피드백 리뷰 UI 목업 */}
              <div className="flex flex-col sm:flex-row">
                {/* 좌: 비디오 영역 */}
                <div className="flex-1 bg-[#0a0a0a] p-4 sm:p-6">
                  <div className="flex aspect-video items-center justify-center rounded-lg bg-[#1a1a1a]">
                    <div className="text-center">
                      <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full border-2 border-[#333] text-[#555]">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M8 5v14l11-7z" />
                        </svg>
                      </div>
                      <p className="font-mono text-xs text-[#555]">00:45 / 03:20</p>
                    </div>
                  </div>
                  {/* 타임라인 바 */}
                  <div className="mt-3 h-1.5 w-full rounded-full bg-[#222]">
                    <div className="relative h-full w-[23%] rounded-full bg-[#444]">
                      {/* 피드백 마커들 */}
                      <div className="absolute -top-0.5 left-[10%] h-2.5 w-0.5 rounded-full bg-info" />
                      <div className="absolute -top-0.5 left-[35%] h-2.5 w-0.5 rounded-full bg-success" />
                      <div className="absolute -top-0.5 left-[65%] h-2.5 w-0.5 rounded-full bg-warning" />
                    </div>
                  </div>
                </div>

                {/* 우: 피드백 패널 */}
                <div className="w-full border-t border-border p-4 sm:w-72 sm:border-l sm:border-t-0 sm:p-5 md:w-80">
                  <h3 className="mb-1 text-xs font-semibold uppercase tracking-wider text-text-tertiary">
                    타임스탬프 피드백
                  </h3>
                  <div className="mt-4 space-y-4">
                    <DemoFeedbackItem
                      time="00:05"
                      label="content"
                      category="content"
                      content="질문 의도를 정확히 파악하고 답변을 시작했습니다."
                    />
                    <DemoFeedbackItem
                      time="00:15"
                      label="verbal"
                      category="verbal"
                      content="답변 초반에 결론을 먼저 말하면 더 효과적입니다."
                    />
                    <DemoFeedbackItem
                      time="00:30"
                      label="nonverbal"
                      category="nonverbal"
                      content="시선이 자주 아래로 향하는 경향이 있습니다."
                    />
                  </div>
                </div>
              </div>
            </div>

            <p className="mt-4 text-center font-mono text-xs text-text-tertiary">
              실제 피드백 리뷰 화면
            </p>
          </div>
        </section>

        {/* Features — 3-column 대신 좌우 교차 레이아웃 */}
        <section
          ref={featuresRef}
          style={featuresStyle}
          className="border-t border-border px-4 py-20 sm:px-6 sm:py-28"
          aria-labelledby="features-heading"
        >
          <div className="mx-auto max-w-3xl">
            <h2
              id="features-heading"
              className="sr-only"
            >
              주요 기능
            </h2>

            <div className="space-y-20 sm:space-y-28">
              {/* Feature 1 */}
              <div className="flex flex-col gap-4">
                <span className="font-mono text-xs uppercase tracking-widest text-text-tertiary">
                  01 — ai questions
                </span>
                <h3 className="text-2xl font-bold tracking-tight text-text-primary sm:text-3xl">
                  이력서 기반 맞춤 질문
                </h3>
                <p className="max-w-lg text-base leading-relaxed text-text-secondary">
                  이력서를 입력하면 직무와 경력에 맞는 면접 질문을 생성합니다.
                  답변에 따라 후속 질문까지 이어집니다. 실제 면접처럼.
                </p>
              </div>

              {/* Feature 2 */}
              <div className="flex flex-col gap-4">
                <span className="font-mono text-xs uppercase tracking-widest text-text-tertiary">
                  02 — nonverbal analysis
                </span>
                <h3 className="text-2xl font-bold tracking-tight text-text-primary sm:text-3xl">
                  보이지 않는 습관까지
                </h3>
                <p className="max-w-lg text-base leading-relaxed text-text-secondary">
                  시선, 표정, 자세를 실시간으로 추적합니다.
                  면접에서 놓치기 쉬운 비언어적 습관을 객관적으로 확인하세요.
                </p>
              </div>

              {/* Feature 3 */}
              <div className="flex flex-col gap-4">
                <span className="font-mono text-xs uppercase tracking-widest text-text-tertiary">
                  03 — timestamp feedback
                </span>
                <h3 className="text-2xl font-bold tracking-tight text-text-primary sm:text-3xl">
                  정확한 시점에, 정확한 피드백
                </h3>
                <p className="max-w-lg text-base leading-relaxed text-text-secondary">
                  녹화 영상과 AI 피드백이 타임스탬프로 동기화됩니다.
                  영상을 재생하며 어떤 순간에 무엇이 좋았고, 무엇을 고쳐야 하는지.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Bottom CTA */}
        <section
          ref={ctaRef}
          style={ctaStyle}
          className="border-t border-border bg-surface px-4 py-20 sm:px-6 sm:py-28"
        >
          <div className="mx-auto max-w-md text-center">
            <h2 className="text-2xl font-bold text-text-primary sm:text-3xl">
              준비되셨나요?
            </h2>
            <p className="mt-3 text-base text-text-secondary">
              지금 바로 AI 모의면접을 시작하세요.
            </p>
            <div className="mt-10">
              <Button variant="cta" className="w-full sm:w-auto" onClick={() => navigate('/interview/setup')}>
                지금 시작하기
              </Button>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-border px-4 py-8 sm:px-6">
        <p className="text-center font-mono text-xs text-text-tertiary">
          &copy; 2026 리허설(Rehearse)
        </p>
      </footer>
    </div>
  )
}
