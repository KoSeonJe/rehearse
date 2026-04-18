import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { Button } from '@/components/ui/button'

interface CtaSectionProps {
  onNavigate: () => void
}

interface ReadyItem {
  label: string
  note: string
}

const READY_ITEMS: ReadyItem[] = [
  { label: 'Chrome 최신 버전', note: '데스크톱 Chrome에 최적화' },
  { label: '카메라 · 마이크 허용', note: '권한 승인 1회면 끝' },
  { label: '조용하고 밝은 환경', note: '소음이 적고 조명이 고른 곳' },
]

export const CtaSection = ({ onNavigate }: CtaSectionProps) => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="cta-heading"
      className="bg-background py-20 md:py-28"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="rounded-3xl bg-foreground px-7 py-14 text-background md:px-14 md:py-20">
          <div className="max-w-2xl">
            <h2
              id="cta-heading"
              className="text-4xl md:text-5xl font-bold leading-[1.05] tracking-[-0.03em] text-background"
            >
              첫 면접 1건은<br />
              무료입니다.
            </h2>
            <p className="mt-5 text-base md:text-lg font-medium leading-relaxed text-background/75">
              이력서만 있으면 3분 뒤 시작합니다. 이력서 기반으로 질문이 생성되고, 녹화가 끝나면 타임스탬프 피드백이 자동 생성됩니다.
            </p>

            <ul className="mt-10 divide-y divide-background/15 border-t border-background/20" aria-label="시작 전 체크리스트">
              {READY_ITEMS.map((item) => (
                <li
                  key={item.label}
                  className="flex flex-col gap-0.5 py-3.5 md:flex-row md:items-baseline md:justify-between md:gap-6"
                >
                  <span className="text-[15px] font-semibold text-background">{item.label}</span>
                  <span className="text-[13px] font-medium text-background/60">{item.note}</span>
                </li>
              ))}
            </ul>

            <div className="mt-10">
              <Button
                variant="secondary"
                size="lg"
                onClick={onNavigate}
                aria-label="무료로 리허설 시작하기"
                className="rounded-2xl bg-background px-9 text-foreground hover:bg-background/90"
              >
                무료로 1회 면접 시작
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
