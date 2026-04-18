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
      className="bg-background py-24 md:py-32"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="border-t border-foreground/10 pt-12">

          {/* Manifesto — 좌정렬, 대형 editorial 타이포 */}
          <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.1em] text-muted-foreground mb-6">
            START NOW
          </p>
          <h2 className="text-4xl md:text-5xl font-bold leading-[1.08] tracking-[-0.025em] text-foreground max-w-2xl">
            준비된 만큼<br />
            보여줄 수 있습니다.
          </h2>
          <p className="mt-6 text-base md:text-lg font-medium text-muted-foreground max-w-xl leading-relaxed">
            면접은 실력보다 연습이 결정합니다.<br className="hidden md:block" />
            리허설로 매 답변을 한 프레임씩 복기하세요.
          </p>

          <div className="mt-10 flex items-center gap-6">
            <Button
              variant="cta"
              size="lg"
              onClick={onNavigate}
              aria-label="무료로 Rehearse 시작하기"
              className="rounded-2xl px-10"
            >
              지금 시작하기
            </Button>
            <p className="text-sm text-muted-foreground">
              <span aria-hidden="true">무료 · 30초 가입 · Chrome 브라우저만 필요</span>
              <span className="sr-only">무료, 30초 가입, Chrome 브라우저만 필요</span>
            </p>
          </div>

        </div>
      </div>
    </section>
  )
}
