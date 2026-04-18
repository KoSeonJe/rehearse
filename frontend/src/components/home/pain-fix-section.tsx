import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface PainFix {
  pain: string
  fix: string
}

const PAIN_FIX: PainFix[] = [
  {
    pain: '총평만 받아서, 어느 답변이 약했는지 몰라요.',
    fix: '답변 한 건마다 잘한 점 · 아쉬운 점 · 이렇게 말하면 더 좋아요, 세 갈래로 쪼개 보여드립니다.',
  },
  {
    pain: '“어…”, “그…” 같은 습관어가 얼마나 나오는지 체감이 안 돼요.',
    fix: '답변 구간별로 습관어 횟수와 말하기 속도를 집계하고, 음성을 글자로 바꿔 한 번에 볼 수 있습니다.',
  },
  {
    pain: '글자로만 받은 피드백은 내 말투·표정까지 떠올리기 어려워요.',
    fix: '피드백마다 영상 타임스탬프가 걸려요. 클릭 한 번에 그 순간으로 돌아가 말투·표정까지 복기합니다.',
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
      <div className="mx-auto max-w-5xl px-5 md:px-8">
        <h2
          id="painfix-heading"
          className="text-3xl md:text-[2.5rem] font-bold leading-[1.1] tracking-[-0.025em] text-foreground"
        >
          “연습은 했는데,<br className="md:hidden" /> 뭘 고칠지 모르겠어요.”
        </h2>
        <p className="mt-4 max-w-xl text-[15px] md:text-base font-medium leading-[1.75] text-muted-foreground">
          점수와 한 줄 총평으로는 다음에 무엇을 바꿔야 할지 알기 어렵습니다.
          <br className="hidden md:block" />
          리허설은 <span className="text-foreground font-semibold">세 가지 불만</span>을 한 번에 답합니다.
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
              <div className="text-[15px] font-medium leading-[1.7] text-muted-foreground md:text-[15px]">
                <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wider text-foreground/40">
                  Pain
                </span>
                <p className="italic">“{item.pain}”</p>
              </div>
              <div className="col-start-2 text-[15px] font-bold leading-[1.7] text-foreground md:col-start-3 md:row-start-1 md:text-base">
                <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wider text-foreground/40">
                  Rehearse
                </span>
                <p>{item.fix}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}
