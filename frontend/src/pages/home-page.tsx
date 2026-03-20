import { useNavigate } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'
import { HeroSection } from '@/components/home/hero-section'

export const HomePage = () => {
  const navigate = useNavigate()
  const handleNavigateSetup = () => navigate('/interview/setup')

  return (
    <div className="min-h-screen bg-[#0B0C0E] text-white selection:bg-accent/30 selection:text-white">
      {/* Navigation */}
      <header className="fixed top-0 z-50 w-full border-b border-white/[0.05] bg-[#0B0C0E]/60 backdrop-blur-xl">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-6 md:px-8">
          <div className="flex items-center gap-2.5 transition-opacity hover:opacity-80" onClick={() => navigate('/')} role="button">
            <Logo size={60} />
            <span className="text-sm font-bold tracking-tight">Rehearse</span>
          </div>
          <nav className="hidden md:block">
            <ul className="flex items-center gap-8 text-[13px] font-medium text-text-secondary">
              <li className="transition-colors hover:text-white"><a href="#features">Features</a></li>
              <li className="transition-colors hover:text-white"><a href="#method">Method</a></li>
              <li>
                <button 
                  onClick={handleNavigateSetup}
                  className="group relative flex h-8 items-center justify-center rounded-full border border-white/10 bg-white/5 px-4 text-xs font-bold transition-all hover:bg-white hover:text-black"
                >
                  Get Started
                </button>
              </li>
            </ul>
          </nav>
        </div>
      </header>

      <main>
        <HeroSection onNavigate={handleNavigateSetup} />

        {/* Linear Style Method Section (Integrated How it works) */}
        <section id="method" className="relative py-24 md:py-40">
          <div className="mx-auto max-w-5xl px-6 md:px-8">
            <div className="mb-20 text-center">
              <h2 className="text-3xl font-bold tracking-tight text-white md:text-5xl">
                준비부터 분석까지,<br />단 3단계면 충분합니다
              </h2>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              {[
                { step: '01', title: '맞춤형 설정', desc: '직무를 선택하고 이력서를 업로드하세요. AI가 당신만을 위한 기술 질문을 설계합니다.' },
                { step: '02', title: '실전 면접 녹화', desc: '브라우저에서 바로 면접을 진행하세요. 모든 답변과 표정은 실시간으로 기록됩니다.' },
                { step: '03', title: '정밀 복기 분석', desc: 'AI가 생성한 타임스탬프 피드백과 함께 자신의 면접 영상을 다시 확인하세요.' },
              ].map((item, i) => (
                <div key={i} className="group relative rounded-card border border-white/[0.05] bg-white/[0.02] p-8 transition-colors hover:bg-white/[0.04]">
                  <span className="mb-4 block font-mono text-xs font-bold text-accent">{item.step}</span>
                  <h3 className="mb-3 text-lg font-bold text-white">{item.title}</h3>
                  <p className="text-sm leading-relaxed text-text-secondary">{item.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Feature Focus Section */}
        <section id="features" className="relative border-t border-white/[0.05] py-24 md:py-40">
          <div className="mx-auto max-w-5xl px-6 md:px-8">
            <div className="flex flex-col items-center gap-20 md:flex-row">
              <div className="flex-1 space-y-6">
                <span className="text-[10px] font-black tracking-[0.2em] text-accent uppercase">AI Vision Analysis</span>
                <h2 className="text-3xl font-bold leading-tight tracking-tight text-white md:text-5xl">
                  눈빛과 자세까지<br />당신의 태도를 읽습니다
                </h2>
                <p className="text-lg font-medium leading-relaxed text-text-secondary">
                  GPT-4o Vision 기반의 엔진이 시선 처리, 표정의 변화, 자세의 안정성을 프레임 단위로 분석합니다. 비언어적 커뮤니케이션의 완성이 합격을 결정합니다.
                </p>
                <ul className="space-y-3">
                  {['시선 추적', '표정 긍정성 분석', '자세 안정도 체크'].map((t) => (
                    <li key={t} className="flex items-center gap-3 text-sm font-medium text-white/70">
                      <div className="h-1 w-1 rounded-full bg-accent" />
                      {t}
                    </li>
                  ))}
                </ul>
              </div>
              <div className="relative aspect-square flex-1 rounded-card border border-white/10 bg-gradient-to-br from-white/5 to-transparent p-1">
                 <div className="h-full w-full rounded-[11px] bg-[#0B0C0E] flex items-center justify-center">
                    {/* Placeholder for a cool feature visual */}
                    <div className="relative">
                      <div className="absolute -inset-10 rounded-full bg-accent/20 blur-3xl" />
                      <div className="relative h-40 w-40 rounded-full border border-white/10 flex items-center justify-center">
                        <div className="h-24 w-24 rounded-full border border-accent/30 flex items-center justify-center animate-pulse">
                          <div className="h-12 w-12 rounded-full bg-accent" />
                        </div>
                      </div>
                    </div>
                 </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-white/[0.05] py-20">
        <div className="mx-auto max-w-5xl px-8 flex flex-col items-center gap-8">
          <div className="flex items-center gap-2 opacity-50">
            <Logo size={40} />
            <span className="text-xs font-bold tracking-tight">Rehearse</span>
          </div>
          <p className="text-[11px] font-medium text-text-tertiary">
            &copy; 2026 Rehearse. Built for the next generation of engineers.
          </p>
        </div>
      </footer>
    </div>
  )
}
