import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'

const STORAGE_KEY = 'oauth_redirect'

export const usePostLoginRedirect = () => {
  const { isAuthenticated, isLoading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (isLoading) return

    const target = localStorage.getItem(STORAGE_KEY)
    if (!target) return

    localStorage.removeItem(STORAGE_KEY)

    if (isAuthenticated && target !== location.pathname) {
      navigate(target, { replace: true })
    }
  }, [isLoading, isAuthenticated, navigate, location.pathname])
}
