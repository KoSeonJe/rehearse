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
      className="bg-gradient-to-b from-accent/5 to-white pt-16 pb-32"
    >
      <div className="mx-auto max-w-4xl px-5 md:px-8 text-center">
        <div className="mb-8">
          <Character mood="happy" size={80} />
        </div>
        <h2 className="text-3xl font-extrabold leading-snug text-text-primary md:text-4xl">
          준비된 만큼 보여줄 수 있습니다
        </h2>
        <p className="mt-4 text-lg text-text-secondary">
          지금 바로 리허설을 시작하세요.
        </p>
        <div className="mt-10">
          <button
            className="rounded-2xl bg-accent px-12 py-5 text-lg font-bold text-white transition-all hover:bg-accent-hover active:scale-95 shadow-lg shadow-accent/20"
            onClick={onNavigate}
          >
            무료로 시작하기
          </button>
        </div>
      </div>
    </section>
  )
}
