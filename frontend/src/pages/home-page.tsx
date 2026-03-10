import { useNavigate } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

export const HomePage = () => {
  const navigate = useNavigate()
  const { ref: heroRef, style: heroStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: journeyRef, style: journeyStyle } = useFadeInOnScroll<HTMLElement>()
  const { ref: ctaRef, style: ctaStyle } = useFadeInOnScroll<HTMLElement>()

  return (
    <div className="min-h-screen bg-white text-text-primary selection:bg-accent/10">
      {/* Navigation */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-5 md:px-8">
          <div className="flex items-center gap-2">
            <Logo size={80} />
            <span className="text-xl font-extrabold tracking-tight text-text-primary">
              리허설
            </span>
          </div>
          <button 
            className="rounded-xl bg-accent px-5 py-2.5 text-sm font-bold text-white transition-transform active:scale-95"
            onClick={() => navigate('/interview/setup')}
          >
            시작하기
          </button>
        </div>
      </header>

      <main>
        {/* Hero Section */}
        <section
          ref={heroRef}
          style={heroStyle}
          className="mx-auto max-w-4xl px-5 pt-20 text-center md:pt-32"
        >
          <div className="flex flex-col items-center justify-center gap-4">
            <Logo size={120} />
            <h1 className="mt-4 text-[40px] font-extrabold leading-[1.2] tracking-tighter text-text-primary md:text-[64px]">
              준비한 만큼 보여줄 수 있게<br />
              <span className="text-accent">면접 연습, 리허설해 보세요.</span>
            </h1>
          </div>
          <p className="mt-10 text-lg font-medium leading-relaxed text-text-secondary md:text-xl">
            단순한 연습을 넘어, 합격의 경험을 미리 시뮬레이션합니다.<br className="hidden md:block" />
            <span className="text-text-primary font-bold">실제 우리가 만든 화면들</span>과 함께 면접 여정을 확인해 보세요.
          </p>
          <div className="mt-12">
            <button 
              className="rounded-2xl bg-accent px-12 py-5 text-lg font-bold text-white transition-all hover:bg-accent-hover active:scale-95 shadow-lg shadow-accent/20"
              onClick={() => navigate('/interview/setup')}
            >
              면접 여정 시작하기
            </button>
          </div>
        </section>

        {/* The Real Journey Section — 실제 만든 페이지 디자인 재현 */}
        <section
          ref={journeyRef}
          style={journeyStyle}
          className="mx-auto max-w-5xl px-5 py-32"
        >
          <div className="text-center mb-24">
            <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-4">The Real Experience</p>
            <h2 className="text-3xl font-extrabold tracking-tighter text-text-primary md:text-4xl">
              합격까지의 모든 과정,<br />
              <span className="text-accent">직접 보여드릴게요.</span>
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
                    <div className="h-16 rounded-[20px] bg-accent flex items-center justify-center text-[10px] font-black text-white">MID</div>
                    <div className="h-16 rounded-[20px] bg-white border border-border flex items-center justify-center text-[10px] font-bold text-text-tertiary">SENIOR</div>
                    <div className="h-16 rounded-[20px] bg-white border border-border flex items-center justify-center text-[10px] font-bold text-text-tertiary">LEAD</div>
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
                  친절한 AI 캐릭터와 대화하며 실전처럼 연습하세요.<br />
                  음성 감지 시스템이 당신의 답변을 경청합니다.
                </p>
              </div>
              <div className="flex-1 w-full max-w-[480px] rounded-[32px] bg-white p-6 border border-border shadow-toss-lg -rotate-2">
                <div className="relative aspect-video rounded-2xl bg-gradient-to-br from-slate-50 via-white to-indigo-50/30 overflow-hidden flex items-center justify-center border border-slate-100">
                  {/* Spotlight */}
                  <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(99,102,241,0.08),transparent_70%)]" />
                  
                  {/* Character */}
                  <div className="relative z-10 drop-shadow-2xl">
                    <Character mood="happy" size={120} />
                  </div>
                  
                  {/* HUD */}
                  <div className="absolute bottom-4 right-4 flex items-center gap-2 h-8 px-3 bg-white/80 backdrop-blur-md rounded-xl border border-slate-200">
                    <div className="flex items-end gap-0.5 h-3">
                      {[1, 2, 3, 4].map(i => <div key={i} className="w-[2px] bg-accent rounded-full h-full animate-pulse" />)}
                    </div>
                    <span className="text-[8px] font-black text-text-primary uppercase tracking-wider">Recording</span>
                  </div>
                </div>
                <div className="mt-6 flex justify-center">
                  <div className="h-12 w-32 rounded-[20px] bg-text-primary flex items-center justify-center text-xs font-black text-white shadow-lg">답변 완료</div>
                </div>
              </div>
            </div>

            {/* Step 3: Report Page Mockup */}
            <div className="flex flex-col md:flex-row items-center gap-16">
              <div className="flex-1 space-y-6">
                <div className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-accent text-white font-black">3</div>
                <h3 className="text-2xl font-extrabold tracking-tight text-text-primary md:text-3xl">데이터로 증명되는 성장</h3>
                <p className="text-lg font-medium text-text-secondary leading-relaxed">
                  면접이 끝나면 종합 점수와 정밀 피드백을 받습니다.<br />
                  합격을 위한 구체적인 가이드를 확인하세요.
                </p>
              </div>
              <div className="flex-1 w-full max-w-[440px] rounded-[32px] bg-white border border-border p-8 shadow-toss rotate-1">
                <div className="text-center mb-8">
                  <span className="text-6xl font-black tracking-tighter text-text-primary">88</span>
                  <span className="text-xl font-bold text-text-tertiary ml-1">/ 100</span>
                </div>
                <div className="space-y-3">
                  <div className="rounded-2xl bg-success/5 border border-success/10 p-4 flex items-center gap-3">
                    <span className="text-success">✓</span>
                    <span className="text-xs font-bold text-text-primary">기술적 부채 해결 경험을 잘 설명하셨어요.</span>
                  </div>
                  <div className="rounded-2xl bg-accent/5 border border-accent/10 p-4 flex items-center gap-3">
                    <span className="text-accent">!</span>
                    <span className="text-xs font-bold text-text-primary">시선 처리를 조금 더 안정적으로 개선해보세요.</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Final CTA */}
        <section
          ref={ctaRef}
          style={ctaStyle}
          className="mx-auto max-w-4xl px-5 py-32 text-center"
        >
          <h2 className="text-3xl font-extrabold leading-snug text-text-primary md:text-4xl">
            준비는 끝났습니다.<br />
            이제 합격할 차례예요.
          </h2>
          <div className="mt-12">
            <button 
              className="rounded-2xl bg-accent px-12 py-5 text-lg font-bold text-white transition-all hover:bg-accent-hover active:scale-95"
              onClick={() => navigate('/interview/setup')}
            >
              지금 바로 리허설 시작하기
            </button>
          </div>
        </section>
      </main>

      <footer className="border-t border-border py-12 text-center">
        <p className="text-xs font-bold text-text-tertiary">
          &copy; 2026 리허설. 당신의 합격을 위해 만들었습니다.
        </p>
      </footer>
    </div>
  )
}
