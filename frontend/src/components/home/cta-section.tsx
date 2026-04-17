import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { Button } from '@/components/ui/button'

interface CtaSectionProps {
  onNavigate: () => void
}

export const CtaSection = ({ onNavigate }: CtaSectionProps) => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="bg-gradient-to-b from-violet-legacy/5 to-white pt-20 pb-32"
    >
      <div className="mx-auto max-w-4xl px-5 md:px-8 text-center">
        <div className="mb-8 flex justify-center">
          <Character mood="happy" size={80} />
        </div>
        <h2 className="text-3xl font-extrabold leading-snug tracking-tighter text-text-primary md:text-4xl">
          준비된 만큼 보여줄 수 있습니다.
        </h2>
        <p className="mt-4 text-base text-text-secondary md:text-lg">
          지금 바로 리허설을 시작하고,<br />
          당신의 면접 영상을 한 프레임씩 복기해보세요.
        </p>
        <div className="mt-10">
          <Button
            variant="cta"
            size="lg"
            onClick={onNavigate}
            aria-label="무료로 Rehearse 시작하기"
            className="rounded-2xl px-12"
          >
            지금 시작하기
          </Button>
          <p className="mt-4 text-sm text-text-tertiary">
            <span aria-hidden="true">무료 · 30초 가입 · Chrome 브라우저만 필요</span>
            <span className="sr-only">무료, 30초 가입, Chrome 브라우저만 필요</span>
          </p>
        </div>
      </div>
    </section>
  )
}
