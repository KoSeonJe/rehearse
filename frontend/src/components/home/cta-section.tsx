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
      className="mx-auto max-w-4xl px-5 py-32 text-center"
    >
      <h2 className="text-3xl font-extrabold leading-snug text-text-primary md:text-4xl">
        당신의 면접, 이제 혼자 고민하지 마세요.<br />
        리허설이 함께합니다.
      </h2>
      <div className="mt-12">
        <button
          className="rounded-2xl bg-accent px-12 py-5 text-lg font-bold text-white transition-all hover:bg-accent-hover active:scale-95"
          onClick={onNavigate}
        >
          무료로 체험하기
        </button>
      </div>
    </section>
  )
}
