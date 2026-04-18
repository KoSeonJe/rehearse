import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface MetricItem {
  label: string
  question: string
}

const VERBAL_METRICS: MetricItem[] = [
  { label: '답변 완성도', question: '질문에 얼마나 정확히 답했는가?' },
  { label: '기술적 정확도', question: '기술 용어나 개념에 오류가 있었는가?' },
  { label: '습관어 횟수', question: '어, 음, 그… 를 몇 번 썼는가?' },
  { label: '말하기 속도', question: '너무 빨랐나, 느렸나, 적당했나?' },
]

const NONVERBAL_METRICS: MetricItem[] = [
  { label: '자세 안정성', question: '몸이 흔들렸거나 웅크렸는가?' },
  { label: '표정 변화', question: '표정이 딱딱했나, 자연스러웠나?' },
  { label: '시선 처리', question: '카메라를 얼마나 바라봤는가?' },
  { label: '음성 자신감', question: '목소리가 떨렸는가, 확신감 있었는가?' },
  { label: '감정 표현', question: '긍정적·중립·불안 중 어디에 가까웠는가?' },
]

interface MetricRowProps {
  index: number
  label: string
  question: string
}

const MetricRow = ({ index, label, question }: MetricRowProps) => (
  <li className="grid grid-cols-[2.25rem_1fr] items-start gap-x-4 border-b border-foreground/8 py-4 last:border-b-0">
    <span
      className="font-tabular text-[13px] font-semibold text-foreground/40"
      aria-hidden="true"
    >
      {String(index).padStart(2, '0')}
    </span>
    <div className="min-w-0">
      <p className="text-[15px] font-bold text-foreground">{label}</p>
      <p className="mt-1 text-[13px] font-medium leading-relaxed text-muted-foreground">
        {question}
      </p>
    </div>
  </li>
)

export const MetricsSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="metrics-heading"
      className="bg-surface py-20 md:py-28"
    >
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <div className="border-t border-foreground/10 pt-10">
          <h2
            id="metrics-heading"
            className="text-3xl md:text-[2.5rem] font-bold leading-[1.1] tracking-[-0.025em] text-foreground max-w-3xl"
          >
            답변의 매 순간을<br className="hidden md:block" />
            9가지 축으로 봅니다.
          </h2>
          <p className="mt-4 max-w-2xl text-base font-medium leading-relaxed text-muted-foreground">
            말한 내용(언어 4축)과 말하는 태도(비언어 5축)를 따로 보면, 다음에 고쳐야 할 게 선명해집니다.
          </p>
        </div>

        <div className="mt-12 grid grid-cols-1 gap-x-16 gap-y-12 md:grid-cols-2">
          <div>
            <p className="text-sm font-bold text-foreground">언어</p>
            <p className="mt-1 text-xs font-medium text-muted-foreground">말한 내용에 대한 분석 · 4가지</p>
            <ul className="mt-4 border-t border-foreground/10" aria-label="언어 지표 목록">
              {VERBAL_METRICS.map((m, idx) => (
                <MetricRow key={m.label} index={idx + 1} {...m} />
              ))}
            </ul>
          </div>

          <div>
            <p className="text-sm font-bold text-foreground">비언어</p>
            <p className="mt-1 text-xs font-medium text-muted-foreground">말하는 태도에 대한 분석 · 5가지</p>
            <ul className="mt-4 border-t border-foreground/10" aria-label="비언어 지표 목록">
              {NONVERBAL_METRICS.map((m, idx) => (
                <MetricRow key={m.label} index={idx + 5} {...m} />
              ))}
            </ul>
          </div>
        </div>

        <p className="mt-10 text-xs font-medium text-muted-foreground">
          비언어 분석은 GPT-4o Vision 기반 · 음성 전사는 OpenAI Whisper.
        </p>
      </div>
    </section>
  )
}
