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
        <div className="rounded-3xl bg-brand px-7 py-14 text-brand-foreground md:px-14 md:py-20">
          <div className="max-w-2xl">
            <h2
              id="cta-heading"
              className="text-4xl md:text-5xl font-bold leading-[1.05] tracking-[-0.03em] text-brand-foreground"
            >
              지금 시작하면,<br />
              베타 피드백을 같이 만듭니다.
            </h2>
            <p className="mt-6 max-w-xl text-[16px] md:text-lg font-medium leading-[1.75] text-brand-foreground/80">
              <span className="text-brand-foreground font-semibold">베타 전 기능 무료</span>. 이력서만 있으면 3분 뒤 첫 면접이 시작되고, 질문이 자동 생성됩니다.
              <br className="hidden md:block" />
              쓰면서 불편한 점을 남겨주시면 다음 업데이트에 반영해요 — 만들어지는 제품을 같이 다듬는 피드백이 환영입니다.
            </p>

            <ul className="mt-10 divide-y divide-brand-foreground/15 border-t border-brand-foreground/20" aria-label="시작 전 체크리스트">
              {READY_ITEMS.map((item) => (
                <li
                  key={item.label}
                  className="flex flex-col gap-0.5 py-3.5 md:flex-row md:items-baseline md:justify-between md:gap-6"
                >
                  <span className="text-[15px] font-semibold text-brand-foreground">{item.label}</span>
                  <span className="text-[13px] font-medium text-brand-foreground/65">{item.note}</span>
                </li>
              ))}
            </ul>

            <div className="mt-10">
              <Button
                variant="secondary"
                size="lg"
                onClick={onNavigate}
                aria-label="무료로 리허설 시작하기"
                className="rounded-2xl bg-brand-foreground px-9 text-brand hover:bg-brand-foreground/90 border-transparent"
              >
                무료로 시작하기
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
