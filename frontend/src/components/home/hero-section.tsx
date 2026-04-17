import { useNavigate } from 'react-router-dom'
import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { Button } from '@/components/ui/button'

interface HeroSectionProps {
  onNavigate: () => void
  isAuthenticated: boolean
}

export const HeroSection = ({ onNavigate, isAuthenticated }: HeroSectionProps) => {
  const navigate = useNavigate()
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  const handleStart = () => {
    if (isAuthenticated) {
      navigate('/interview/setup')
    } else {
      onNavigate()
    }
  }

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="hero-heading"
      className="py-20 md:py-28 bg-white"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="flex flex-col items-center gap-12 md:flex-row md:items-center md:gap-16">

          {/* Left: 텍스트 */}
          <div className="flex-1 text-center md:text-left">
            <h1
              id="hero-heading"
              className="text-4xl font-extrabold leading-[1.2] tracking-tighter text-text-primary md:text-5xl"
            >
              면접, 연습하면<br />
              <span className="text-text-primary">달라집니다.</span>
            </h1>
            <p className="mt-6 text-lg font-medium leading-relaxed text-text-secondary md:text-xl">
              이력서 분석은 물론, CS 기초와 직무 지식까지<br className="hidden md:block" />
              AI가 맞춤 질문을 만들고 영상과 함께 피드백합니다.
            </p>
            <div className="mt-10">
              <Button
                variant="cta"
                size="lg"
                onClick={handleStart}
                aria-label="무료로 리허설 시작하기"
                className="rounded-2xl px-12"
              >
                무료로 시작하기
              </Button>
              <p className="mt-4 text-sm text-text-tertiary">
                <span aria-hidden="true">무료 · 30초 가입 · Chrome 브라우저만 필요</span>
                <span className="sr-only">무료, 30초 가입, Chrome 브라우저만 필요</span>
              </p>
            </div>
          </div>

          {/* Right: 캐릭터 목업 */}
          {/* TODO(plan-04): Aceternity 인터랙션 삽입 자리 — Hero 우측 카드에 Spotlight/Beam 효과 적용 예정 */}
          <div className="flex-1 flex justify-center w-full md:justify-end">
            <div
              className="w-full max-w-[420px] rounded-[32px] bg-surface border border-border shadow-toss-lg rotate-2"
              aria-hidden="true"
            >
              <div className="relative aspect-video rounded-[24px] bg-surface overflow-hidden flex items-center justify-center border border-border">
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(0,0,0,0.03),transparent_70%)]" />
                <div className="relative z-10 drop-shadow-2xl">
                  <Character mood="happy" size={120} />
                </div>
                <div className="absolute bottom-4 right-4 flex items-center gap-2 h-8 px-3 bg-white/80 backdrop-blur-md rounded-xl border border-slate-200">
                  <div className="flex items-end gap-0.5 h-3">
                    {[1, 2, 3, 4].map((i) => (
                      <div
                        key={i}
                        className="w-[2px] bg-text-primary rounded-full h-full animate-pulse"
                        style={{ animationDelay: `${i * 100}ms` }}
                      />
                    ))}
                  </div>
                  <span className="text-[8px] font-black text-text-primary uppercase tracking-wider">
                    Recording
                  </span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </section>
  )
}
