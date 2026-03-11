import { useCallback, useEffect, useRef, useState } from 'react'

interface UseTtsOptions {
  lang?: string
  rate?: number
  onStart?: () => void
  onEnd?: () => void
}

const IS_ANDROID = typeof navigator !== 'undefined' && /Android/i.test(navigator.userAgent)

export const useTts = ({
  lang = 'ko-KR',
  rate = 0.95,
  onStart,
  onEnd,
}: UseTtsOptions = {}) => {
  const [isSpeaking, setIsSpeaking] = useState(false)
  const [isAvailable, setIsAvailable] = useState(false)
  const voiceRef = useRef<SpeechSynthesisVoice | null>(null)
  const isAvailableRef = useRef(false)
  const onStartRef = useRef(onStart)
  const onEndRef = useRef(onEnd)
  const resumeIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const pendingVoicesListenerRef = useRef<(() => void) | null>(null)

  useEffect(() => {
    onStartRef.current = onStart
    onEndRef.current = onEnd
  })

  useEffect(() => {
    if (typeof window === 'undefined' || !window.speechSynthesis) return

    const selectKoreanVoice = () => {
      const voices = window.speechSynthesis.getVoices()
      const koreanVoice = voices.find((v) => v.lang.startsWith('ko'))
      voiceRef.current = koreanVoice ?? null
      isAvailableRef.current = !!koreanVoice
      setIsAvailable(!!koreanVoice)
    }

    selectKoreanVoice()

    window.speechSynthesis.addEventListener('voiceschanged', selectKoreanVoice)
    return () => {
      window.speechSynthesis.removeEventListener('voiceschanged', selectKoreanVoice)
      if (pendingVoicesListenerRef.current) {
        window.speechSynthesis.removeEventListener('voiceschanged', pendingVoicesListenerRef.current)
        pendingVoicesListenerRef.current = null
      }
      window.speechSynthesis.cancel()
    }
  }, [])

  const speak = useCallback(
    (text: string) => {
      if (!isAvailableRef.current || !voiceRef.current) return

      window.speechSynthesis.cancel()

      const utterance = new SpeechSynthesisUtterance(text)
      utterance.voice = voiceRef.current
      utterance.lang = lang
      utterance.rate = rate

      const clearResumeInterval = () => {
        if (resumeIntervalRef.current) {
          clearInterval(resumeIntervalRef.current)
          resumeIntervalRef.current = null
        }
      }

      utterance.onstart = () => {
        setIsSpeaking(true)
        onStartRef.current?.()

        // Chrome 15초 자동 중단 버그 워크어라운드
        // Android에서는 pause()가 완전 종료로 작동하므로 스킵
        if (!IS_ANDROID) {
          clearResumeInterval()
          resumeIntervalRef.current = setInterval(() => {
            if (window.speechSynthesis.speaking) {
              window.speechSynthesis.pause()
              window.speechSynthesis.resume()
            } else {
              clearResumeInterval()
            }
          }, 14000)
        }
      }

      utterance.onend = () => {
        clearResumeInterval()
        setIsSpeaking(false)
        onEndRef.current?.()
      }

      utterance.onerror = () => {
        clearResumeInterval()
        setIsSpeaking(false)
        onEndRef.current?.()
      }

      window.speechSynthesis.speak(utterance)
    },
    [lang, rate],
  )

  const stop = useCallback(() => {
    if (resumeIntervalRef.current) {
      clearInterval(resumeIntervalRef.current)
      resumeIntervalRef.current = null
    }
    window.speechSynthesis.cancel()
    setIsSpeaking(false)
  }, [])

  // 음성 로드 완료 대기 후 speak (초기 로드 경쟁 조건 해결)
  const speakWhenReady = useCallback(
    (text: string) => {
      // 이전 대기 리스너 정리
      if (pendingVoicesListenerRef.current) {
        window.speechSynthesis.removeEventListener('voiceschanged', pendingVoicesListenerRef.current)
        pendingVoicesListenerRef.current = null
      }

      if (isAvailableRef.current && voiceRef.current) {
        speak(text)
        return
      }

      // 음성이 아직 로드되지 않았으면 voiceschanged 이벤트 대기
      const onVoicesLoaded = () => {
        const voices = window.speechSynthesis.getVoices()
        const koreanVoice = voices.find((v) => v.lang.startsWith('ko'))
        if (koreanVoice) {
          voiceRef.current = koreanVoice
          isAvailableRef.current = true
          setIsAvailable(true)
          speak(text)
        }
        window.speechSynthesis.removeEventListener('voiceschanged', onVoicesLoaded)
        pendingVoicesListenerRef.current = null
      }

      pendingVoicesListenerRef.current = onVoicesLoaded
      window.speechSynthesis.addEventListener('voiceschanged', onVoicesLoaded)
    },
    [speak],
  )

  return { speak, speakWhenReady, stop, isSpeaking, isAvailable }
}
