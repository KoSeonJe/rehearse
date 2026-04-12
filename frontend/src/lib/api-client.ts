const API_BASE_URL = import.meta.env.VITE_API_URL || ''

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
}

interface ApiErrorBody {
  success: false
  status: number
  code: string
  message: string
  errors: Array<{ field: string; value: string; reason: string }>
  timestamp: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly errors: Array<{ field: string; value: string; reason: string }>

  constructor(status: number, body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code
    this.errors = body.errors ?? []
  }
}

class ApiClient {
  private baseUrl: string

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl
  }

  private async request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    const { body, headers, ...rest } = options

    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...headers,
      },
      credentials: 'include',
      body: body ? JSON.stringify(body) : undefined,
      ...rest,
    })

    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    }

    if (!response.ok) {
      let errorBody: ApiErrorBody
      try {
        errorBody = (await response.json()) as ApiErrorBody
      } catch {
        throw new ApiError(response.status, {
          success: false,
          status: response.status,
          code: 'UNKNOWN_ERROR',
          message: response.statusText || '알 수 없는 오류가 발생했습니다.',
          errors: [],
          timestamp: new Date().toISOString(),
        })
      }
      throw new ApiError(response.status, errorBody)
    }

    return response.json() as Promise<T>
  }

  get<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'GET' })
  }

  post<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'POST', body })
  }

  put<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'PUT', body })
  }

  patch<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'PATCH', body })
  }

  delete<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'DELETE' })
  }
}

export const apiClient = new ApiClient(API_BASE_URL)
