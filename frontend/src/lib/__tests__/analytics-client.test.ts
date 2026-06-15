import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const MEASUREMENT_ID = 'G-TEST123456'

const importFreshModule = async () => {
  vi.resetModules()
  return import('../analytics-client')
}

const setMeasurementId = (value: string) => {
  vi.stubEnv('VITE_GA4_MEASUREMENT_ID', value)
}

const setWebdriver = (value: boolean) => {
  Object.defineProperty(navigator, 'webdriver', {
    value,
    configurable: true,
  })
}

beforeEach(() => {
  setWebdriver(false)
  document.head.innerHTML = ''
  delete window.gtag
  delete window.dataLayer
})

afterEach(() => {
  vi.unstubAllEnvs()
})

describe('analytics-client', () => {
  it('측정 ID 미설정 시 gtag 스크립트를 주입하지 않는다', async () => {
    setMeasurementId('')
    const { initGA4 } = await importFreshModule()

    initGA4()

    expect(document.head.querySelector('script')).toBeNull()
    expect(window.gtag).toBeUndefined()
  })

  it('navigator.webdriver=true(prerender) 시 gtag를 주입하지 않는다', async () => {
    setMeasurementId(MEASUREMENT_ID)
    setWebdriver(true)
    const { initGA4 } = await importFreshModule()

    initGA4()

    expect(document.head.querySelector('script')).toBeNull()
    expect(window.gtag).toBeUndefined()
  })

  it('정상 설정 시 gtag 스크립트 주입 + config 호출한다', async () => {
    setMeasurementId(MEASUREMENT_ID)
    const { initGA4 } = await importFreshModule()

    initGA4()

    const script = document.head.querySelector('script')
    expect(script?.getAttribute('src')).toContain(MEASUREMENT_ID)
    expect(window.dataLayer).toContainEqual(['config', MEASUREMENT_ID, { send_page_view: false }])
  })

  it('initGA4를 두 번 호출해도 스크립트는 1회만 주입한다', async () => {
    setMeasurementId(MEASUREMENT_ID)
    const { initGA4 } = await importFreshModule()

    initGA4()
    initGA4()

    expect(document.head.querySelectorAll('script')).toHaveLength(1)
  })

  it('trackEvent는 gtag 미존재 시 no-op이고, 존재 시 event를 전송한다', async () => {
    setMeasurementId(MEASUREMENT_ID)
    const { initGA4, trackEvent } = await importFreshModule()

    trackEvent('sign_up')
    expect(window.dataLayer).toBeUndefined()

    initGA4()
    trackEvent('sign_up', { method: 'github' })
    expect(window.dataLayer).toContainEqual(['event', 'sign_up', { method: 'github' }])
  })

  it('trackPageview는 page_path와 함께 page_view 이벤트를 전송한다', async () => {
    setMeasurementId(MEASUREMENT_ID)
    const { initGA4, trackPageview } = await importFreshModule()

    initGA4()
    trackPageview('/dashboard')

    expect(window.dataLayer).toContainEqual(['event', 'page_view', { page_path: '/dashboard' }])
  })
})
