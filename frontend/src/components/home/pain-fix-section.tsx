import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface PainFix {
  pain: string
  fix: string
}

const PAIN_FIX: PainFix[] = [
  {
    pain: '총평은 주는데, 어느 답변이 약했고 어느 부분을 고쳐야 하는지는 알 수 없어요.',
    fix: '답변 한 건 한 건을 "잘한 점 · 아쉬운 점 · 이렇게 말하면 더 좋아요" 세 갈래로 쪼개 보여드립니다.',
  },
  {
    pain: '"어…", "그…" 같은 습관어를 내가 얼마나 쓰는지 체감이 안 돼요.',
    fix: '답변 구간별로 습관어 횟수·말하기 속도·기술적 정확도까지 STT로 분석해 표시합니다.',
  },
  {
    pain: '답변 텍스트 코멘트만 있어서, 실제 내 말투·표정이 어땠는지는 알 수 없어요.',
    fix: '피드백마다 영상 타임스탬프가 연결돼, 클릭하면 그 순간 영상으로 이동해 말투·표정까지 복기할 수 있습니다.',
  },
]

export const PainFixSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="painfix-heading"
      className="bg-background py-20 md:py-28"
    >
      <div className="mx-auto max-w-4xl px-5 md:px-8">
        <h2
          id="painfix-heading"
          className="text-3xl md:text-[2.5rem] font-bold leading-[1.1] tracking-[-0.025em] text-foreground"
        >
          면접 피드백, 왜 늘 부족했을까요
        </h2>
        <p className="mt-4 max-w-2xl text-base font-medium leading-relaxed text-muted-foreground">
          점수와 한 줄 총평으로는 다음 면접에서 무엇을 바꿔야 할지 알기 어렵습니다.
          리허설은 세 가지 불만을 한 번에 답합니다.
        </p>

        <ol
          className="mt-14 border-t border-foreground/10"
          aria-label="기존 면접 피드백의 한계와 리허설의 답변"
        >
          {PAIN_FIX.map((item, idx) => (
            <li
              key={item.pain}
              className="grid grid-cols-[2.5rem_1fr] gap-x-5 gap-y-3 border-b border-foreground/10 py-8 md:grid-cols-[3rem_1fr_1fr] md:gap-x-8 md:py-10"
            >
              <span
                className="font-tabular text-sm font-semibold text-foreground/40 md:row-span-2 md:text-base"
                aria-hidden="true"
              >
                0{idx + 1}
              </span>
              <p className="text-sm italic font-medium leading-relaxed text-muted-foreground md:text-[15px]">
                “{item.pain}”
              </p>
              <p className="col-start-2 text-[15px] font-bold leading-relaxed text-foreground md:col-start-3 md:row-start-1 md:text-base">
                {item.fix}
              </p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}
