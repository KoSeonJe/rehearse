type GtagArguments = [string, ...unknown[]]

declare global {
  interface Window {
    dataLayer?: (GtagArguments | IArguments)[]
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
  // gtag.js 는 dataLayer 항목이 arguments 객체일 때만 커맨드로 인식한다 (배열 push 는 레거시 경로로 폐기됨).
  function gtag(..._args: GtagArguments): void {
    // eslint-disable-next-line prefer-rest-params -- gtag.js 는 arguments 객체만 커맨드로 인식 (rest 배열 불가)
    window.dataLayer?.push(arguments)
  }
  window.gtag = gtag

  gtag('js', new Date())
  gtag('config', MEASUREMENT_ID, { send_page_view: false })
}

export const trackPageview = (path: string): void => {
  if (!window.gtag) return
  window.gtag('event', 'page_view', { page_location: `${window.location.origin}${path}` })
}

export const trackEvent = (name: string, params?: Record<string, unknown>): void => {
  if (!window.gtag) return
  window.gtag('event', name, params)
}
