import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { AUTH_QUERY_KEY, LOGOUT_SIGNAL_KEY } from '@/constants/auth'

export const useCrossTabSync = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  useEffect(() => {
    const handler = (e: StorageEvent) => {
      if (e.key === LOGOUT_SIGNAL_KEY && e.newValue) {
        queryClient.setQueryData(AUTH_QUERY_KEY, null)
        navigate('/', { replace: true })
        localStorage.removeItem(LOGOUT_SIGNAL_KEY)
      }
    }
    window.addEventListener('storage', handler)
    return () => window.removeEventListener('storage', handler)
  }, [queryClient, navigate])
}
