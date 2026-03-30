import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/lib/api-client'
import { useAuthStore, type AuthUser } from '@/stores/auth-store'
import type { ApiResponse } from '@/types/interview'

export const useAuth = () => {
  const { user, setUser, setLoading } = useAuthStore()

  const { data, isPending } = useQuery({
    queryKey: ['auth', 'me'],
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

  useEffect(() => {
    if (!isPending) {
      setUser(data ?? null)
      setLoading(false)
    }
  }, [data, isPending, setUser, setLoading])

  return {
    user,
    isLoading: isPending,
    isAuthenticated: data != null,
  }
}
