import { useNavigate } from 'react-router-dom'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { Button } from '@/components/ui/button'
import { PageGrid } from '@/components/layout/page-grid'
import { InterviewWebcamMock } from '@/components/home/interview-webcam-mock'

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
      className="pt-14 pb-20 md:pt-20 md:pb-28 bg-background"
    >
      <PageGrid>
        <div className="col-span-4 md:col-span-5 lg:col-span-6 flex flex-col justify-center">
          <h1
            id="hero-heading"
            className="text-[2.5rem] leading-[1.05] font-bold tracking-[-0.03em] text-foreground md:text-5xl lg:text-[3.75rem]"
          >
            면접의 약한 순간만<br />
            다시 연습합니다.
          </h1>
          <p className="mt-7 max-w-lg text-base md:text-lg font-medium leading-relaxed text-muted-foreground">
            답변을 녹화하면, 타임스탬프마다 무엇을 고쳐야 하는지가 보입니다.
            총평이 아니라 순간을 돌려봅니다.
          </p>
          <div className="mt-10 flex flex-wrap items-center gap-5">
            <Button
              variant="cta"
              size="lg"
              onClick={handleStart}
              aria-label="무료로 리허설 시작하기"
              className="rounded-2xl px-9"
            >
              무료로 시작하기
            </Button>
            <p className="text-xs font-medium text-muted-foreground">
              <span aria-hidden="true">베타 기간 전 기능 무료 · 30초 가입 · Chrome 권장</span>
              <span className="sr-only">베타 기간 전 기능 무료, 30초 가입, Chrome 권장</span>
            </p>
          </div>
        </div>

        <div className="col-span-4 md:col-span-3 lg:col-span-6 mt-10 md:mt-0 flex items-center">
          <InterviewWebcamMock className="rotate-[0.5deg]" />
        </div>
      </PageGrid>
    </section>
  )
}
