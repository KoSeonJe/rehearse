import { useNavigate } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'
import { HeroSection } from '@/components/home/hero-section'
import { HowItWorksSection } from '@/components/home/how-it-works-section'
import { KeyFeaturesSection } from '@/components/home/key-features-section'
import { BeforeYouStartSection } from '@/components/home/before-you-start-section'
import { FaqSection } from '@/components/home/faq-section'
import { CtaSection } from '@/components/home/cta-section'

export const HomePage = () => {
  const navigate = useNavigate()
  const handleNavigateSetup = () => navigate('/interview/setup')

  return (
    <div className="min-h-screen bg-white text-text-primary selection:bg-accent/10">
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-border/50">
        <div className="mx-auto flex h-16 max-w-5xl items-center px-5 md:px-8">
          <div className="flex items-center gap-2">
            <Logo size={80} />
            <span className="text-xl font-extrabold tracking-tight text-text-primary">
              리허설
            </span>
          </div>
        </div>
      </header>

      <main>
        <HeroSection onNavigate={handleNavigateSetup} />
        <HowItWorksSection />
        <KeyFeaturesSection />
        <BeforeYouStartSection />
        <FaqSection />
        <CtaSection onNavigate={handleNavigateSetup} />
      </main>

      <footer className="border-t border-border py-12 text-center">
        <p className="text-xs font-bold text-text-tertiary">
          &copy; 2026 리허설. 당신의 합격을 위해 만들었습니다.
        </p>
      </footer>
    </div>
  )
}
