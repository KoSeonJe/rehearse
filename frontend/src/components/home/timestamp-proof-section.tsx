import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { FeedbackPreviewMock } from '@/components/home/feedback-preview-mock'

interface ProofRow {
  label: string
  detail: string
}

const PROOF_ROWS: ProofRow[] = [
  {
    label: '타임라인 구간 동기화',
    detail: '답변 구간마다 피드백 블록이 붙어, 클릭하면 해당 구간 영상으로 이동합니다',
  },
  {
    label: '언어 · 비언어 레벨 분석',
    detail: '말하기 속도 · 자신감 · 자세 · 시선 · 표정을 단계로 평가하고 습관어 횟수를 집계합니다',
  },
  {
    label: '답변 단위 코멘트',
    detail: '잘한 점 · 아쉬운 점 · 이렇게 말하면 더 좋아요 세 갈래',
  },
]

export const TimestampProofSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="proof-heading"
      className="bg-surface py-20 md:py-28"
    >
      <div className="mx-auto max-w-6xl px-5 md:px-8">
        <div className="grid grid-cols-1 gap-10 lg:grid-cols-12 lg:gap-12">
          <div className="lg:col-span-5 flex flex-col justify-center">
            <h2
              id="proof-heading"
              className="text-3xl md:text-[2.5rem] font-bold leading-[1.1] tracking-[-0.025em] text-foreground"
            >
              한 문장 총평 대신,<br />
              몇 초 몇 초의 기록을 봅니다.
            </h2>
            <p className="mt-5 text-base font-medium leading-relaxed text-muted-foreground">
              리허설은 답변 전체를 "잘했다/아쉽다"로 뭉뚱그리지 않습니다.
              영상·지표·코멘트가 같은 타임스탬프로 연결돼, 클릭 한 번에 그 순간으로 돌아갑니다.
            </p>

            <dl className="mt-10 divide-y divide-foreground/8 border-t border-foreground/10">
              {PROOF_ROWS.map((row) => (
                <div key={row.label} className="flex flex-col gap-1 py-4 md:flex-row md:items-baseline md:gap-6">
                  <dt className="text-sm font-bold text-foreground md:w-44 md:shrink-0">
                    {row.label}
                  </dt>
                  <dd className="text-sm font-medium leading-relaxed text-muted-foreground">
                    {row.detail}
                  </dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="lg:col-span-7 flex items-center">
            <FeedbackPreviewMock variant="proof" />
          </div>
        </div>
      </div>
    </section>
  )
}
