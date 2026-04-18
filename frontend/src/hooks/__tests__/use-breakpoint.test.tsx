import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useBreakpoint } from '@/hooks/use-breakpoint'

type Listener = (e: Partial<MediaQueryListEvent>) => void

function mockMatchMedia(matchesFor: Record<string, boolean>) {
  const listeners = new Map<string, Set<Listener>>()
  const listMap = new Map<string, { matches: boolean; add: (l: Listener) => void; remove: (l: Listener) => void }>()

  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => {
      const entry = listMap.get(query) ?? {
        matches: matchesFor[query] ?? false,
        add: (l: Listener) => {
          const set = listeners.get(query) ?? new Set()
          set.add(l)
          listeners.set(query, set)
        },
        remove: (l: Listener) => listeners.get(query)?.delete(l),
      }
      listMap.set(query, entry)
      return {
        matches: entry.matches,
        media: query,
        addEventListener: (_: string, l: Listener) => entry.add(l),
        removeEventListener: (_: string, l: Listener) => entry.remove(l),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }
    }),
  })
}

describe('useBreakpoint', () => {
  beforeEach(() => {
    mockMatchMedia({ '(min-width: 1280px)': true, '(min-width: 768px)': false })
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('xl 쿼리 매치 시 true 반환', () => {
    const { result } = renderHook(() => useBreakpoint('xl'))
    expect(result.current).toBe(true)
  })

  it('md 쿼리 미매치 시 false 반환', () => {
    const { result } = renderHook(() => useBreakpoint('md'))
    expect(result.current).toBe(false)
  })
})
