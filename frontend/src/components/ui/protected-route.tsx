import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { Spinner } from '@/components/ui/spinner'

const REDIRECT_TO_HOME_PATHS = ['/dashboard']

export const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuth()
  const { openLoginModal } = useAuthStore()
  const location = useLocation()

  const shouldRedirectToHome = REDIRECT_TO_HOME_PATHS.includes(location.pathname)

  useEffect(() => {
    if (!isLoading && !isAuthenticated && !shouldRedirectToHome) {
      openLoginModal(location.pathname, '로그인이 필요합니다')
    }
  }, [isLoading, isAuthenticated, openLoginModal, location.pathname, shouldRedirectToHome])

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (!isAuthenticated) {
    if (shouldRedirectToHome) {
      return <Navigate to="/" replace />
    }
    return <div className="min-h-screen bg-surface" />
  }

  return <Outlet />
}
