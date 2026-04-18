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
          <p className="mb-5 text-[12px] md:text-[13px] font-semibold uppercase tracking-[0.12em] text-brand">
            AI 개발자 모의면접 · 타임스탬프 영상 피드백
          </p>
          <h1
            id="hero-heading"
            className="text-[2.5rem] leading-[1.1] font-bold tracking-[-0.03em] text-foreground md:text-5xl lg:text-[3.75rem]"
          >
            다음 면접에서 뭘 고칠지,
            <br className="hidden md:block" />{' '}
            30분이면 보입니다.
          </h1>
          <p className="mt-7 max-w-md text-[17px] md:text-lg font-medium leading-[1.7] text-muted-foreground">
            이력서 한 장이면 맞춤 질문이 시작되고,
            <br className="hidden md:block" />{' '}
            녹화한 답변을 <span className="text-foreground font-semibold">초 단위 타임스탬프</span>로 짚어드려요.
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
              <span aria-hidden="true">베타 전 기능 무료 · 이력서 올리면 3분 뒤 시작 · Chrome 권장</span>
              <span className="sr-only">베타 전 기능 무료, 이력서 올리면 3분 뒤 시작, Chrome 권장</span>
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
