import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { Spinner } from '@/components/ui/spinner'

export const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuth()
  const { openLoginModal } = useAuthStore()
  const location = useLocation()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      openLoginModal(location.pathname, '로그인이 필요합니다')
    }
  }, [isLoading, isAuthenticated, openLoginModal, location.pathname])

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <div className="min-h-screen bg-surface" />
  }

  return <Outlet />
}
