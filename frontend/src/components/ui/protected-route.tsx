import { useEffect } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { useAuthStore } from '@/stores/auth-store'
import { Spinner } from '@/components/ui/spinner'

export const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuth()
  const { openLoginModal } = useAuthStore()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      openLoginModal()
    }
  }, [isLoading, isAuthenticated, openLoginModal])

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
