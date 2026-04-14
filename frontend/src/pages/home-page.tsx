import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'
import { HeroSection } from '@/components/home/hero-section'
import { PainPointsSection } from '@/components/home/pain-points-section'
import { VideoFeedbackSection } from '@/components/home/video-feedback-section'
import { MetricsSection } from '@/components/home/metrics-section'
import { DevTailoredSection } from '@/components/home/dev-tailored-section'
import { JourneySection } from '@/components/home/journey-section'
import { FaqSection } from '@/components/home/faq-section'
import { CtaSection } from '@/components/home/cta-section'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { useLogout } from '@/hooks/use-logout'

export const HomePage = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { user, isAuthenticated, isLoading } = useAuth()
  const { openLoginModal } = useAuthStore()
  const logout = useLogout()
  const hasOauthError = useMemo(() => searchParams.get('error') === 'auth_failed', [searchParams])
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      navigate('/dashboard', { replace: true })
    }
  }, [isLoading, isAuthenticated, navigate])

  useEffect(() => {
    if (!hasOauthError) return
    localStorage.removeItem('oauth_redirect')
    navigate('/', { replace: true })
    const timer = setTimeout(() => setDismissed(true), 4000)
    return () => clearTimeout(timer)
  }, [hasOauthError, navigate])

  const handleOpenLogin = () => openLoginModal()
  const handleStartLogin = () => openLoginModal('/interview/setup', '로그인이 필요합니다')

  return (
    <div className="min-h-screen bg-white text-text-primary selection:bg-accent/10">
      {hasOauthError && !dismissed && (
        <div className="fixed top-4 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-red-50 border border-red-200 px-5 py-3 text-sm font-medium text-red-700 shadow-md">
          로그인에 실패했습니다. 다시 시도해주세요.
        </div>
      )}
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
                  onClick={logout}
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
        <PainPointsSection />
        <VideoFeedbackSection />
        <MetricsSection />
        <DevTailoredSection />
        <JourneySection />
        <FaqSection />
        <CtaSection onNavigate={handleStartLogin} />
      </main>

      <footer className="border-t border-border bg-white">
        <div className="mx-auto max-w-5xl px-5 md:px-8 py-6 flex items-center justify-between gap-4">
          <p className="text-xs font-medium text-text-tertiary">
            &copy; 2026 KoSeonJe. All rights reserved.
          </p>
          <a
            href="https://github.com/KoSeonJe/rehearse"
            target="_blank"
            rel="noopener noreferrer"
            className="text-text-tertiary transition-colors hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 rounded-sm"
            aria-label="GitHub 저장소 열기"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M12 .5C5.73.5.5 5.73.5 12c0 5.08 3.29 9.39 7.86 10.91.58.11.79-.25.79-.56 0-.28-.01-1.02-.02-2-3.2.69-3.87-1.54-3.87-1.54-.52-1.33-1.28-1.68-1.28-1.68-1.05-.72.08-.7.08-.7 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.7 1.26 3.36.96.1-.75.4-1.26.73-1.55-2.56-.29-5.25-1.28-5.25-5.71 0-1.26.45-2.29 1.19-3.1-.12-.29-.52-1.47.11-3.06 0 0 .97-.31 3.18 1.18A11.07 11.07 0 0 1 12 6.8c.98.01 1.97.13 2.89.39 2.21-1.49 3.18-1.18 3.18-1.18.63 1.59.23 2.77.11 3.06.74.81 1.19 1.84 1.19 3.1 0 4.44-2.69 5.42-5.26 5.71.41.35.78 1.05.78 2.11 0 1.52-.01 2.75-.01 3.12 0 .31.21.67.8.56C20.21 21.39 23.5 17.08 23.5 12 23.5 5.73 18.27.5 12 .5z" />
            </svg>
          </a>
        </div>
      </footer>
    </div>
  )
}
