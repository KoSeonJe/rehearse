import { useEffect } from 'react'
import { initGA4 } from '@/lib/analytics-client'

export const useAnalyticsInit = () => {
  useEffect(() => {
    initGA4()
  }, [])
}
