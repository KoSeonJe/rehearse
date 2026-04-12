import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/stores/auth-store'
import { AUTH_QUERY_KEY } from '@/constants/auth'

export const useAuthInterceptor = () => {
  const queryClient = useQueryClient()
  const openLoginModal = useAuthStore((s) => s.openLoginModal)

  useEffect(() => {
    const handler = () => {
      queryClient.setQueryData(AUTH_QUERY_KEY, null)
      openLoginModal(window.location.pathname, '세션이 만료되었습니다. 다시 로그인해주세요.')
    }
    window.addEventListener('auth:unauthorized', handler)
    return () => window.removeEventListener('auth:unauthorized', handler)
  }, [queryClient, openLoginModal])
}
