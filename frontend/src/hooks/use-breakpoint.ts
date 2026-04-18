import { useEffect, useState } from 'react'

export const BREAKPOINTS = {
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536,
} as const

export type Breakpoint = keyof typeof BREAKPOINTS

/**
 * Tailwind breakpoint matchMedia hook.
 * Returns true when viewport width >= given breakpoint.
 * SSR-safe: returns false on first render, subscribes after mount.
 */
export function useBreakpoint(bp: Breakpoint): boolean {
  const query = `(min-width: ${BREAKPOINTS[bp]}px)`
  const [matches, setMatches] = useState(false)

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return
    const mql = window.matchMedia(query)
    const handler = (e: MediaQueryListEvent | MediaQueryList) => {
      setMatches('matches' in e ? e.matches : false)
    }
    handler(mql)
    mql.addEventListener('change', handler)
    return () => mql.removeEventListener('change', handler)
  }, [query])

  return matches
}
