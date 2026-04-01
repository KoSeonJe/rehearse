import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { Logo } from '@/components/ui/logo'
import { HeroSection } from '@/components/home/hero-section'
import { HowItWorksSection } from '@/components/home/how-it-works-section'
import { KeyFeaturesSection } from '@/components/home/key-features-section'
import { BeforeYouStartSection } from '@/components/home/before-you-start-section'
import { FaqSection } from '@/components/home/faq-section'
import { CtaSection } from '@/components/home/cta-section'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { apiClient } from '@/lib/api-client'

export const HomePage = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { user, isAuthenticated, isLoading } = useAuth()
  const { logout, openLoginModal } = useAuthStore()

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      navigate('/dashboard', { replace: true })
    }
  }, [isLoading, isAuthenticated, navigate])

  const handleOpenLogin = () => openLoginModal()
  const handleStartLogin = () => openLoginModal('/interview/setup', '로그인이 필요합니다')

  const handleLogout = async () => {
    try {
      await apiClient.post('/api/v1/auth/logout')
    } finally {
      logout()
      queryClient.removeQueries({ queryKey: ['auth', 'me'] })
    }
  }

  return (
    <div className="min-h-screen bg-white text-text-primary selection:bg-accent/10">
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-border/50">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-5 md:px-8">
          <div className="flex items-center gap-2">
            <Logo size={80} />
            <span className="text-xl font-extrabold tracking-tight text-text-primary">
              리허설
            </span>
          </div>

          <div className="flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <span className="hidden text-sm font-medium text-text-secondary sm:block">
                  {user?.name}
                </span>
                <button
                  onClick={handleLogout}
                  className="rounded-xl border border-border px-4 py-2 text-sm font-medium text-text-secondary transition-colors hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-border"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <button
                onClick={handleOpenLogin}
                className="rounded-xl border border-border px-4 py-2 text-sm font-medium text-text-secondary transition-colors hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-border"
              >
                로그인
              </button>
            )}
          </div>
        </div>
      </header>

      <main>
        <HeroSection onNavigate={handleStartLogin} isAuthenticated={isAuthenticated} />
        <HowItWorksSection />
        <KeyFeaturesSection />
        <BeforeYouStartSection />
        <FaqSection />
        <CtaSection onNavigate={handleStartLogin} />
      </main>

      <footer className="border-t border-border py-12 text-center">
        <p className="text-xs font-bold text-text-tertiary">
          &copy; 2026 리허설. 당신의 합격을 위해 만들었습니다.
        </p>
      </footer>
    </div>
  )
}
