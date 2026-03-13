import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

export const JourneySection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="mx-auto max-w-5xl px-5 py-32"
    >
      <div className="text-center mb-24">
        <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-4">The Real Experience</p>
        <h2 className="text-3xl font-extrabold tracking-tighter text-text-primary md:text-4xl">
          왜 리허설일까요?
        </h2>
      </div>

      <div className="space-y-40">
        {/* Step 1: Setup Page Mockup */}
        <div className="flex flex-col md:flex-row items-center gap-16">
          <div className="flex-1 space-y-6">
            <div className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-accent text-white font-black">1</div>
            <h3 className="text-2xl font-extrabold tracking-tight text-text-primary md:text-3xl">나만을 위한 면접 설정</h3>
            <p className="text-lg font-medium text-text-secondary leading-relaxed">
              백엔드, 프론트엔드 등 직무를 선택하면<br />
              AI가 이력서를 분석해 1:1 맞춤 질문을 준비합니다.
            </p>
          </div>
          <div className="flex-1 w-full max-w-[440px] rounded-[32px] bg-surface p-6 border border-border shadow-toss rotate-2">
            <div className="space-y-4">
              <div className="h-14 rounded-[20px] bg-white border border-border flex items-center px-5 text-sm font-bold text-text-tertiary">
                예: 시니어 백엔드 엔지니어
              </div>
              <div className="grid grid-cols-3 gap-2">
                <div className="h-16 rounded-[20px] bg-accent flex items-center justify-center text-[10px] font-black text-white">JUNIOR</div>
                <div className="h-16 rounded-[20px] bg-white border border-border flex items-center justify-center text-[10px] font-bold text-text-tertiary">MID</div>
                <div className="h-16 rounded-[20px] bg-white border border-border flex items-center justify-center text-[10px] font-bold text-text-tertiary">SENIOR</div>
              </div>
            </div>
          </div>
        </div>

        {/* Step 2: Conduct Page Mockup */}
        <div className="flex flex-col md:flex-row-reverse items-center gap-16">
          <div className="flex-1 space-y-6 md:text-right">
            <div className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-accent text-white font-black md:ml-auto">2</div>
            <h3 className="text-2xl font-extrabold tracking-tight text-text-primary md:text-3xl">몰입도 높은 AI 면접</h3>
            <p className="text-lg font-medium text-text-secondary leading-relaxed">
              AI 면접관과 실전처럼 대화하며 연습하세요.<br />
              음성 감지 시스템이 당신의 답변을 경청합니다.
            </p>
          </div>
          <div className="flex-1 w-full max-w-[480px] rounded-[32px] bg-white p-6 border border-border shadow-toss-lg -rotate-2">
            <div className="relative aspect-video rounded-2xl bg-gradient-to-br from-slate-50 via-white to-indigo-50/30 overflow-hidden flex items-center justify-center border border-slate-100">
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(99,102,241,0.08),transparent_70%)]" />
              <div className="relative z-10 drop-shadow-2xl">
                <Character mood="happy" size={120} />
              </div>
              <div className="absolute bottom-4 right-4 flex items-center gap-2 h-8 px-3 bg-white/80 backdrop-blur-md rounded-xl border border-slate-200">
                <div className="flex items-end gap-0.5 h-3">
                  {[1, 2, 3, 4].map(i => <div key={i} className="w-[2px] bg-accent rounded-full h-full animate-pulse" />)}
                </div>
                <span className="text-[8px] font-black text-text-primary uppercase tracking-wider">Recording</span>
              </div>
            </div>
            <p className="mt-4 text-center text-xs font-medium text-text-tertiary">
              음성을 감지하면 자동으로 다음 질문으로 넘어갑니다
            </p>
          </div>
        </div>

        {/* Step 3: Report Page Mockup */}
        <div className="flex flex-col md:flex-row items-center gap-16">
          <div className="flex-1 space-y-6">
            <div className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-accent text-white font-black">3</div>
            <h3 className="text-2xl font-extrabold tracking-tight text-text-primary md:text-3xl">매 순간을 분석하는<br />타임스탬프 피드백</h3>
            <p className="text-lg font-medium text-text-secondary leading-relaxed">
              녹화된 영상과 함께 답변의 특정 시점마다 피드백을 제공합니다.<br />
              시선, 표정, 목소리까지 비언어적 요소도 놓치지 않습니다.
            </p>
          </div>
          <div className="flex-1 w-full max-w-[440px] rounded-[32px] bg-white border border-border p-8 shadow-toss rotate-1">
            <div className="space-y-3">
              <div className="rounded-2xl bg-surface border border-border p-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className="inline-flex items-center rounded-lg bg-accent/10 px-2 py-0.5 text-[10px] font-black text-accent">0:42</span>
                  <span className="text-[10px] font-bold text-text-tertiary">답변 분석</span>
                </div>
                <p className="text-xs font-bold text-text-primary leading-relaxed">
                  기술적 부채 해결 경험을 구체적으로 잘 설명하셨어요. STAR 기법이 자연스럽게 녹아있습니다.
                </p>
              </div>
              <div className="rounded-2xl bg-surface border border-border p-4">
                <div className="flex items-center gap-2 mb-2">
                  <span className="inline-flex items-center rounded-lg bg-accent/10 px-2 py-0.5 text-[10px] font-black text-accent">1:15</span>
                  <span className="inline-flex items-center rounded-md bg-amber-50 px-1.5 py-0.5 text-[9px] font-bold text-amber-600">시선 처리</span>
                </div>
                <p className="text-xs font-bold text-text-primary leading-relaxed">
                  이 구간에서 시선이 아래로 향했어요. 카메라를 바라보며 답변하면 자신감이 더 전달됩니다.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
