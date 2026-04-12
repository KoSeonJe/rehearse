import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '@/lib/api-client'
import { AUTH_QUERY_KEY, LOGOUT_SIGNAL_KEY } from '@/constants/auth'

export const useLogout = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return async () => {
    // 1. 클라이언트 상태 먼저 정리 (API 실패해도 로그아웃 유지)
    queryClient.setQueryData(AUTH_QUERY_KEY, null)

    // 2. 서버 쿠키 삭제
    try {
      await apiClient.post('/api/v1/auth/logout')
    } catch {
      // 서버 에러여도 클라이언트는 이미 로그아웃 상태
    }

    // 3. 탭 간 동기화 시그널
    localStorage.setItem(LOGOUT_SIGNAL_KEY, Date.now().toString())

    // 4. 홈으로 이동
    navigate('/', { replace: true })
  }
}
