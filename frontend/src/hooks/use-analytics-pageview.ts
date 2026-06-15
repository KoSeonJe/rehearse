import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { trackPageview } from '@/lib/analytics-client'

export const useAnalyticsPageview = () => {
  const { pathname, search } = useLocation()

  useEffect(() => {
    trackPageview(`${pathname}${search}`)
  }, [pathname, search])
}
