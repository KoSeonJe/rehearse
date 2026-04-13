import { useQuery } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/lib/api-client'
import type { AuthUser } from '@/stores/auth-store'
import type { ApiResponse } from '@/types/interview'
import { AUTH_QUERY_KEY } from '@/constants/auth'

export const useAuth = () => {
  const { data, isPending } = useQuery({
    queryKey: AUTH_QUERY_KEY,
    queryFn: async () => {
      try {
        const res = await apiClient.get<ApiResponse<AuthUser>>('/api/v1/auth/me')
        return res.data
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          return null
        }
        return null
      }
    },
    staleTime: 5 * 60 * 1000,
    retry: false,
  })

  return {
    user: data ?? null,
    isLoading: isPending,
    isAuthenticated: data != null,
  }
}
