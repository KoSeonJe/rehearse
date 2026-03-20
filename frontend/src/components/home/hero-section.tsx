import { Character } from '@/components/ui/character'
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
      className="py-24 md:py-32"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="flex flex-col items-center gap-12 md:flex-row md:items-center md:gap-16">

          {/* Left: 텍스트 */}
          <div className="flex-1 text-center md:text-left">
            <h1 className="text-4xl font-extrabold leading-[1.2] tracking-tighter text-text-primary md:text-5xl">
              면접, 연습하면<br />
              <span className="text-accent">달라집니다.</span>
            </h1>
            <p className="mt-6 text-lg font-medium leading-relaxed text-text-secondary md:text-xl">
              AI가 이력서를 분석해 맞춤 질문을 만들고,<br className="hidden md:block" />
              답변의 매 순간을 영상과 함께 피드백합니다.
            </p>
            <div className="mt-10">
              <button
                className="rounded-2xl bg-accent px-12 py-5 text-lg font-bold text-white transition-all hover:bg-accent-hover active:scale-95 shadow-lg shadow-accent/20"
                onClick={onNavigate}
                aria-label="무료로 시작하기"
              >
                무료로 시작하기
              </button>
              <p className="mt-4 text-sm text-text-tertiary">
                회원가입 없이 바로 체험 · 이력서 업로드만 하면 끝
              </p>
            </div>
          </div>

          {/* Right: 면접 화면 목업 카드 */}
          <div className="flex-1 flex justify-center w-full md:justify-end">
            <div
              className="w-full max-w-[420px] rounded-[32px] bg-surface border border-border shadow-toss-lg rotate-2"
              aria-hidden="true"
            >
              <div className="relative aspect-video rounded-[24px] bg-gradient-to-br from-slate-50 via-white to-indigo-50/30 overflow-hidden flex items-center justify-center border border-slate-100">
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(99,102,241,0.08),transparent_70%)]" />
                <div className="relative z-10 drop-shadow-2xl">
                  <Character mood="happy" size={120} />
                </div>
                <div className="absolute bottom-4 right-4 flex items-center gap-2 h-8 px-3 bg-white/80 backdrop-blur-md rounded-xl border border-slate-200">
                  <div className="flex items-end gap-0.5 h-3">
                    {[1, 2, 3, 4].map((i) => (
                      <div
                        key={i}
                        className="w-[2px] bg-accent rounded-full h-full animate-pulse"
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
