import { useNavigate } from 'react-router-dom'
import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { Button } from '@/components/ui/button'
import { PageGrid } from '@/components/layout/page-grid'

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
      className="pt-16 pb-20 md:pt-20 md:pb-28 bg-background"
    >
      <PageGrid>
        {/* 좌 7-col — 앵커 카피 */}
        <div className="col-span-4 md:col-span-5 lg:col-span-7 flex flex-col justify-center">
          <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.1em] text-muted-foreground mb-6">
            AI MOCK INTERVIEW
          </p>
          <h1
            id="hero-heading"
            className="text-4xl font-bold leading-[1.1] tracking-[-0.025em] text-foreground md:text-5xl lg:text-[3.5rem]"
          >
            면접, 연습하면<br />
            달라집니다.
          </h1>
          <p className="mt-6 text-base md:text-lg font-medium leading-relaxed text-muted-foreground max-w-lg">
            이력서 분석은 물론, CS 기초와 직무 지식까지<br className="hidden md:block" />
            AI가 맞춤 질문을 만들고 영상과 함께 피드백합니다.
          </p>
          <div className="mt-10 flex items-center gap-6">
            <Button
              variant="cta"
              size="lg"
              onClick={handleStart}
              aria-label="무료로 리허설 시작하기"
              className="rounded-2xl px-10"
            >
              무료로 시작하기
            </Button>
            <p className="text-sm text-muted-foreground hidden sm:block">
              <span aria-hidden="true">30초 가입 · Chrome 브라우저만 필요</span>
              <span className="sr-only">30초 가입, Chrome 브라우저만 필요</span>
            </p>
          </div>
        </div>

        {/* 우 5-col — 제품 목업 */}
        <div
          className="col-span-4 md:col-span-3 lg:col-span-5 flex items-center justify-center mt-12 md:mt-0"
          aria-hidden="true"
        >
          <div className="w-full max-w-[380px] rounded-3xl bg-surface border border-border shadow-toss-lg rotate-1 overflow-hidden">
            <div className="relative aspect-video bg-surface flex items-center justify-center">
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(0,0,0,0.03),transparent_70%)]" />
              <div className="relative z-10 drop-shadow-2xl">
                <Character mood="happy" size={110} />
              </div>
              {/* 녹화 인디케이터 */}
              <div className="absolute bottom-4 right-4 flex items-center gap-2 h-7 px-3 bg-background/85 backdrop-blur-md rounded-xl border border-border">
                <div className="flex items-end gap-0.5 h-3">
                  {[1, 2, 3, 4].map((i) => (
                    <div
                      key={i}
                      className="w-[2px] bg-foreground rounded-full h-full animate-pulse"
                      style={{ animationDelay: `${i * 100}ms` }}
                    />
                  ))}
                </div>
                <span className="text-[8px] font-black text-foreground uppercase tracking-wider">
                  REC
                </span>
              </div>
            </div>
            {/* 하단 메타 바 */}
            <div className="px-5 py-4 border-t border-border flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground">AI 면접관 질문 중</span>
              <span className="font-tabular text-xs font-bold text-foreground">00:42</span>
            </div>
          </div>
        </div>
      </PageGrid>
    </section>
  )
}
