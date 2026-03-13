import { Logo } from '@/components/ui/logo'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface HeroSectionProps {
  onNavigate: () => void
}

export const HeroSection = ({ onNavigate }: HeroSectionProps) => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="mx-auto max-w-4xl px-5 pt-20 text-center md:pt-32"
    >
      <div className="flex flex-col items-center justify-center gap-4">
        <Logo size={120} />
        <h1 className="mt-4 text-3xl font-extrabold leading-[1.2] tracking-tighter text-text-primary md:text-[40px]">
          준비한 만큼 보여줄 수 있게<br />
          <span className="text-accent">면접 연습, 리허설해 보세요.</span>
        </h1>
      </div>
      <p className="mt-10 text-lg font-medium leading-relaxed text-text-secondary md:text-xl">
        AI가 이력서를 분석해 맞춤 질문을 만들고,<br className="hidden md:block" />
        답변의 매 순간을 피드백합니다. 지금 바로 체험해 보세요.
      </p>
      <div className="mt-12">
        <button
          className="rounded-2xl bg-accent px-12 py-5 text-lg font-bold text-white transition-all hover:bg-accent-hover active:scale-95 shadow-lg shadow-accent/20"
          onClick={onNavigate}
        >
          무료로 체험하기
        </button>
      </div>
    </section>
  )
}
