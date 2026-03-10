import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { EditIcon } from '@/components/icons/edit-icon'
import { EyeIcon } from '@/components/icons/eye-icon'
import { VideoIcon } from '@/components/icons/video-icon'

const FeatureCard = ({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode
  title: string
  description: string
}) => (
  <div
    className="rounded-card border border-border bg-surface p-6 transition-shadow duration-200 hover:shadow-md"
    role="article"
  >
    <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-card bg-accent-light text-accent">
      {icon}
    </div>
    <h3 className="text-lg font-semibold text-text-primary">{title}</h3>
    <p className="mt-2 text-sm leading-relaxed text-text-secondary">
      {description}
    </p>
  </div>
)

const StepItem = ({
  step,
  title,
  description,
}: {
  step: number
  title: string
  description: string
}) => (
  <div className="flex flex-col items-center text-center">
    <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-full bg-accent text-sm font-bold text-white">
      {step}
    </div>
    <h3 className="text-base font-semibold text-text-primary">{title}</h3>
    <p className="mt-2 max-w-xs text-sm leading-relaxed text-text-secondary">
      {description}
    </p>
  </div>
)

export const HomePage = () => {
  const navigate = useNavigate()
  const { ref: heroRef, style: heroStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: featuresRef, style: featuresStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: stepsRef, style: stepsStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: ctaRef, style: ctaStyle } = useFadeInOnScroll<HTMLElement>()

  return (
    <div className="min-h-screen bg-background">
      {/* ─── Header ─── */}
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
        {/* ─── Hero ─── */}
        <section
          ref={heroRef}
          style={heroStyle}
          className="px-4 pb-20 pt-16 sm:px-6 sm:pb-24 sm:pt-20 md:pt-28"
        >
          <div className="mx-auto max-w-5xl text-center">
            <div className="mb-8 flex justify-center">
              <div className="animate-float">
                <Character mood="happy" size={160} />
              </div>
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-text-primary sm:text-4xl md:text-5xl">
              면접, 다시 한 번.
            </h1>
            <p className="mx-auto mt-4 max-w-lg text-base leading-relaxed text-text-secondary sm:text-lg">
              AI가 면접을 녹화하고 분석합니다.
              <br className="hidden sm:block" />
              타임스탬프 기반 피드백으로 정확히 어디를 고쳐야 하는지 알려드립니다.
            </p>
            <div className="mt-10">
              <Button variant="cta" className="w-full sm:w-auto" onClick={() => navigate('/interview/setup')}>
                면접 시작하기
              </Button>
            </div>
          </div>
        </section>

        {/* ─── Value Propositions ─── */}
        <section
          ref={featuresRef}
          style={featuresStyle}
          className="bg-surface px-4 py-16 sm:px-6 sm:py-20"
          aria-labelledby="features-heading"
        >
          <div className="mx-auto max-w-5xl">
            <h2
              id="features-heading"
              className="mb-10 text-center text-2xl font-bold text-text-primary sm:mb-12 sm:text-3xl"
            >
              왜 리허설인가요?
            </h2>
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              <FeatureCard
                icon={<EditIcon />}
                title="AI 맞춤 질문 생성"
                description="이력서를 기반으로 직무와 경력에 맞는 면접 질문을 생성합니다. 답변에 따라 후속 질문까지 이어집니다."
              />
              <FeatureCard
                icon={<EyeIcon />}
                title="비언어 분석"
                description="시선, 표정, 자세를 실시간으로 추적합니다. 면접에서 놓치기 쉬운 비언어적 습관을 객관적으로 확인하세요."
              />
              <FeatureCard
                icon={<VideoIcon />}
                title="타임스탬프 피드백"
                description="녹화 영상과 AI 피드백이 타임스탬프로 동기화됩니다. 영상을 재생하며 정확한 시점의 피드백을 확인하세요."
              />
            </div>
          </div>
        </section>

        {/* ─── How It Works ─── */}
        <section
          ref={stepsRef}
          style={stepsStyle}
          className="px-4 py-16 sm:px-6 sm:py-20"
          aria-labelledby="steps-heading"
        >
          <div className="mx-auto max-w-5xl">
            <h2
              id="steps-heading"
              className="mb-10 text-center text-2xl font-bold text-text-primary sm:mb-12 sm:text-3xl"
            >
              어떻게 사용하나요?
            </h2>
            <div className="grid gap-10 sm:grid-cols-3 sm:gap-6">
              <StepItem
                step={1}
                title="면접 설정"
                description="직무와 경력 레벨을 선택하고, 이력서를 입력하면 AI가 맞춤 질문을 준비합니다."
              />
              <StepItem
                step={2}
                title="AI와 면접 진행"
                description="카메라와 마이크를 켜고 실제 면접처럼 진행합니다. 영상 녹화와 분석이 동시에 이루어집니다."
              />
              <StepItem
                step={3}
                title="피드백 리뷰"
                description="녹화 영상을 재생하며 타임스탬프에 맞춰 언어적, 비언어적 피드백을 확인합니다."
              />
            </div>
          </div>
        </section>

        {/* ─── Bottom CTA ─── */}
        <section
          ref={ctaRef}
          style={ctaStyle}
          className="bg-surface px-4 py-16 sm:px-6 sm:py-20"
        >
          <div className="mx-auto max-w-md text-center">
            <Character mood="default" size={100} className="mx-auto mb-6" />
            <h2 className="text-2xl font-bold text-text-primary sm:text-3xl">
              준비되셨나요?
            </h2>
            <p className="mt-3 text-base text-text-secondary">
              지금 바로 AI 모의면접을 시작하세요.
            </p>
            <div className="mt-8">
              <Button variant="cta" className="w-full sm:w-auto" onClick={() => navigate('/interview/setup')}>
                지금 시작하기
              </Button>
            </div>
          </div>
        </section>
      </main>

      {/* ─── Footer ─── */}
      <footer className="border-t border-border px-4 py-8 sm:px-6">
        <p className="text-center text-sm text-text-tertiary">
          &copy; 2026 리허설(Rehearse). AI 기반 개발자 모의면접 플랫폼.
        </p>
      </footer>
    </div>
  )
}
