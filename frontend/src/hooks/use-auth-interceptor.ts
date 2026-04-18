import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/stores/auth-store'
import { AUTH_QUERY_KEY } from '@/constants/auth'

export const useAuthInterceptor = () => {
  const queryClient = useQueryClient()
  const openLoginModal = useAuthStore((s) => s.openLoginModal)

  useEffect(() => {
    const handler = () => {
      // 이전에 인증된 사용자가 있었는지 확인 → 없으면 초회 진입이므로 ProtectedRoute에 위임
      // (C2 수정: 미로그인 초회 진입 시 "세션 만료" 오문구 표시 방지)
      const previousAuthData = queryClient.getQueryData(AUTH_QUERY_KEY)
      const hadActiveSession = previousAuthData !== null && previousAuthData !== undefined

      queryClient.setQueryData(AUTH_QUERY_KEY, null)

      if (hadActiveSession) {
        openLoginModal(window.location.pathname, '세션이 만료되었습니다. 다시 로그인해주세요.')
      }
      // 초회 진입은 no-op — ProtectedRoute가 "로그인이 필요합니다" 메시지로 처리
    }
    window.addEventListener('auth:unauthorized', handler)
    return () => window.removeEventListener('auth:unauthorized', handler)
  }, [queryClient, openLoginModal])
}
