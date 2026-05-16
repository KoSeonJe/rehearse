import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { ApiResponse } from '@/types/interview'
import type {
  AdminQuestionPoolFilters,
  AdminQuestionPoolListResponse,
  CreateQuestionPoolRequest,
} from '@/types/question-pool'

const ADMIN_PASSWORD_KEY = 'admin-password'
const ADMIN_QUESTION_POOLS_QUERY_KEY = 'admin-question-pools'

const appendIfPresent = (params: URLSearchParams, key: string, value: string) => {
  const trimmed = value.trim()
  if (trimmed) {
    params.set(key, trimmed)
  }
}

const buildQueryString = (filters: AdminQuestionPoolFilters, page: number, size: number) => {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  })

  appendIfPresent(params, 'cacheKey', filters.cacheKey)
  appendIfPresent(params, 'category', filters.category)
  appendIfPresent(params, 'keyword', filters.keyword)
  if (filters.isActive) {
    params.set('isActive', filters.isActive)
  }

  return params.toString()
}

const adminHeaders = () => ({
  'X-Admin-Password': sessionStorage.getItem(ADMIN_PASSWORD_KEY) ?? '',
})

export const useAdminQuestionPools = (
  filters: AdminQuestionPoolFilters,
  page: number,
  size: number,
) => {
  return useQuery({
    queryKey: [ADMIN_QUESTION_POOLS_QUERY_KEY, filters, page, size],
    queryFn: () =>
      apiClient.get<ApiResponse<AdminQuestionPoolListResponse>>(
        `/api/v1/admin/question-pools?${buildQueryString(filters, page, size)}`,
        { headers: adminHeaders() },
      ),
  })
}

export const useCreateAdminQuestionPool = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateQuestionPoolRequest) =>
      apiClient.post<ApiResponse<void>>('/api/v1/admin/question-pools', request, {
        headers: adminHeaders(),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [ADMIN_QUESTION_POOLS_QUERY_KEY] })
    },
  })
}
