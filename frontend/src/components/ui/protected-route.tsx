import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { Spinner } from '@/components/ui/spinner'

const REDIRECT_TO_HOME_PATHS = ['/dashboard']

export const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuth()
  const { openLoginModal, showLoginModal } = useAuthStore()
  const location = useLocation()

  const shouldRedirectToHome = REDIRECT_TO_HOME_PATHS.includes(location.pathname)

  useEffect(() => {
    if (!isLoading && !isAuthenticated && !shouldRedirectToHome) {
      openLoginModal(location.pathname, '로그인이 필요합니다')
    }
  }, [isLoading, isAuthenticated, openLoginModal, location.pathname, shouldRedirectToHome])

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (!isAuthenticated) {
    if (shouldRedirectToHome || !showLoginModal) {
      return <Navigate to="/" replace />
    }
    /* E9 walkthrough fix: 기존 `<div className="min-h-screen bg-surface" />`는
       빈 배경만 보여 사용자에게 "페이지가 깨졌나?" 인상을 준다.
       미인증 상태에서 모달이 열리는 중이라는 신호를 Spinner로 제공한다. */
    return (
      <div
        className="flex min-h-screen items-center justify-center bg-background"
        aria-busy="true"
        aria-label="로그인 대기 중"
      >
        <Spinner className="h-6 w-6" />
      </div>
    )
  }

  return <Outlet />
}
