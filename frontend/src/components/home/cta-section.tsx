import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface CtaSectionProps {
  onNavigate: () => void
}

export const CtaSection = ({ onNavigate }: CtaSectionProps) => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="relative bg-accent py-32"
    >
      <div className="bg-grid-light absolute inset-0 opacity-20" aria-hidden="true" />
      <div className="relative mx-auto max-w-4xl px-5 md:px-8 text-center">
        <div className="mb-8">
          <Character mood="happy" size={80} />
        </div>
        <h2 className="heading-section text-3xl leading-snug text-white md:text-4xl">
          준비된 만큼 보여줄 수 있습니다
        </h2>
        <p className="mt-4 text-lg text-white/70">
          지금 바로 리허설을 시작하세요.
        </p>
        <div className="mt-10">
          <button
            className="rounded-full bg-accent-teal px-12 py-5 text-lg font-bold text-white transition-all hover:bg-accent-teal/90 active:scale-95 shadow-glow-teal"
            onClick={onNavigate}
            aria-label="무료로 시작하기"
          >
            무료로 시작하기
          </button>
        </div>
      </div>
    </section>
  )
}
