import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ApiError, apiClient } from '../api-client'

const mockFetch = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', mockFetch)
  mockFetch.mockReset()
})

const createJsonResponse = (body: unknown, status = 200, ok = true) => ({
  ok,
  status,
  statusText: status === 200 ? 'OK' : 'Error',
  json: () => Promise.resolve(body),
})

describe('ApiClient', () => {
  describe('GET', () => {
    it('should send GET request and return parsed JSON', async () => {
      const data = { success: true, data: { id: 1 } }
      mockFetch.mockResolvedValueOnce(createJsonResponse(data))

      const result = await apiClient.get('/api/test')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/test',
        expect.objectContaining({ method: 'GET' }),
      )
      expect(result).toEqual(data)
    })
  })

  describe('POST', () => {
    it('should send POST request with JSON body', async () => {
      const body = { name: 'test' }
      const data = { success: true }
      mockFetch.mockResolvedValueOnce(createJsonResponse(data))

      const result = await apiClient.post('/api/test', body)

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/test',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(body),
          headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
        }),
      )
      expect(result).toEqual(data)
    })
  })

  describe('PATCH', () => {
    it('should send PATCH request with JSON body', async () => {
      const body = { status: 'COMPLETED' }
      const data = { success: true }
      mockFetch.mockResolvedValueOnce(createJsonResponse(data))

      const result = await apiClient.patch('/api/test/1', body)

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/test/1',
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify(body),
        }),
      )
      expect(result).toEqual(data)
    })
  })

  describe('DELETE', () => {
    it('should send DELETE request', async () => {
      const data = { success: true }
      mockFetch.mockResolvedValueOnce(createJsonResponse(data))

      const result = await apiClient.delete('/api/test/1')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/test/1',
        expect.objectContaining({ method: 'DELETE' }),
      )
      expect(result).toEqual(data)
    })
  })

  describe('ApiError', () => {
    it('should throw ApiError on non-ok response with JSON body', async () => {
      const errorBody = {
        success: false as const,
        status: 400,
        code: 'VALIDATION_ERROR',
        message: '유효성 검사 실패',
        errors: [{ field: 'name', value: '', reason: '이름은 필수입니다' }],
        timestamp: '2026-03-10T00:00:00Z',
      }
      mockFetch.mockResolvedValueOnce(createJsonResponse(errorBody, 400, false))

      try {
        await apiClient.get('/api/test')
        expect.fail('should have thrown')
      } catch (error) {
        expect(error).toBeInstanceOf(ApiError)
        const apiError = error as ApiError
        expect(apiError.status).toBe(400)
        expect(apiError.code).toBe('VALIDATION_ERROR')
        expect(apiError.message).toBe('유효성 검사 실패')
        expect(apiError.errors).toHaveLength(1)
      }
    })

    it('should throw ApiError with fallback on JSON parse failure', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: () => Promise.reject(new Error('invalid json')),
      })

      try {
        await apiClient.get('/api/test')
        expect.fail('should have thrown')
      } catch (error) {
        expect(error).toBeInstanceOf(ApiError)
        const apiError = error as ApiError
        expect(apiError.status).toBe(500)
        expect(apiError.code).toBe('UNKNOWN_ERROR')
        expect(apiError.message).toBe('Internal Server Error')
        expect(apiError.errors).toEqual([])
      }
    })
  })

  describe('ApiError construction', () => {
    it('should set name to ApiError', () => {
      const error = new ApiError(404, {
        success: false,
        status: 404,
        code: 'NOT_FOUND',
        message: '리소스를 찾을 수 없습니다',
        errors: [],
        timestamp: '2026-03-10T00:00:00Z',
      })

      expect(error.name).toBe('ApiError')
      expect(error.status).toBe(404)
      expect(error.code).toBe('NOT_FOUND')
      expect(error.message).toBe('리소스를 찾을 수 없습니다')
    })
  })
})
