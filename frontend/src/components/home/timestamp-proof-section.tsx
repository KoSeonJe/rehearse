import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'
import { FeedbackPreviewMock } from '@/components/home/feedback-preview-mock'

interface ProofRow {
  label: string
  detail: string
}

const PROOF_ROWS: ProofRow[] = [
  {
    label: '타임라인 동기화',
    detail: '답변 구간마다 피드백이 붙고, 클릭하면 그 구간 영상으로 바로 이동합니다.',
  },
  {
    label: '언어 · 비언어 분석',
    detail: '말하기 속도 · 음성 안정성 · 자세 · 시선 · 표정을 단계별로 평가하고, 습관어 횟수를 집계합니다. (OpenAI Whisper + GPT-4o Vision)',
  },
  {
    label: '답변 단위 코멘트',
    detail: '잘한 점 · 아쉬운 점 · 이렇게 말하면 더 좋아요 — 세 갈래로 나눠, 다음에 뭘 바꿀지 즉시 보입니다.',
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
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="grid grid-cols-1 gap-10 lg:grid-cols-12 lg:gap-12">
          <div className="lg:col-span-5 flex flex-col justify-center">
            <h2
              id="proof-heading"
              className="text-3xl md:text-[2.5rem] font-bold leading-[1.15] tracking-[-0.025em] text-foreground"
            >
              답변별로 피드백을 주는 건,
              <br className="hidden md:block" />{' '}
              여기뿐입니다.
            </h2>
            <p className="mt-5 max-w-md text-[15px] md:text-base font-medium leading-[1.75] text-muted-foreground">
              잘한 점 · 아쉬운 점 · 다음엔 이렇게 —
              <br className="hidden md:block" />{' '}
              <span className="text-foreground font-semibold">답변 하나하나에 따로 코멘트가 붙습니다.</span>
              <br className="hidden md:block" />{' '}
              영상 · 지표 · 코멘트가 같은 타임스탬프로 묶여, 클릭 한 번에 그 순간으로 돌아가요.
            </p>

            <dl className="mt-10 divide-y divide-foreground/8 border-t border-foreground/10">
              {PROOF_ROWS.map((row) => (
                <div key={row.label} className="flex flex-col gap-1 py-4 md:flex-row md:items-baseline md:gap-6">
                  <dt className="text-[15px] font-bold text-foreground md:w-44 md:shrink-0">
                    {row.label}
                  </dt>
                  <dd className="text-[14px] font-medium leading-[1.7] text-muted-foreground">
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
