type GtagArguments = [string, ...unknown[]]

declare global {
  interface Window {
    dataLayer?: GtagArguments[]
    gtag?: (...args: GtagArguments) => void
  }
}

const MEASUREMENT_ID = import.meta.env.VITE_GA4_MEASUREMENT_ID
const GTAG_SRC = 'https://www.googletagmanager.com/gtag/js'

let isInitialized = false

const isTrackingEnabled = (): boolean => {
  if (!MEASUREMENT_ID) return false
  if (typeof navigator !== 'undefined' && navigator.webdriver) return false
  return true
}

export const initGA4 = (): void => {
  if (isInitialized) return
  if (!isTrackingEnabled()) return

  isInitialized = true

  const script = document.createElement('script')
  script.async = true
  script.src = `${GTAG_SRC}?id=${MEASUREMENT_ID}`
  document.head.appendChild(script)

  window.dataLayer = window.dataLayer ?? []
  const gtag: Window['gtag'] = (...args) => {
    window.dataLayer?.push(args)
  }
  window.gtag = gtag

  gtag('js', new Date().toISOString())
  gtag('config', MEASUREMENT_ID, { send_page_view: false })
}

export const trackPageview = (path: string): void => {
  if (!window.gtag) return
  window.gtag('event', 'page_view', { page_path: path })
}

export const trackEvent = (name: string, params?: Record<string, unknown>): void => {
  if (!window.gtag) return
  window.gtag('event', name, params)
}
