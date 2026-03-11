import { useCallback, useEffect, useRef, useState } from 'react'

interface UseTtsOptions {
  lang?: string
  rate?: number
  onStart?: () => void
  onEnd?: () => void
}

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

      utterance.onstart = () => {
        setIsSpeaking(true)
        onStartRef.current?.()
      }

      utterance.onend = () => {
        setIsSpeaking(false)
        onEndRef.current?.()
      }

      utterance.onerror = () => {
        setIsSpeaking(false)
        onEndRef.current?.()
      }

      window.speechSynthesis.speak(utterance)
    },
    [lang, rate],
  )

  const stop = useCallback(() => {
    window.speechSynthesis.cancel()
    setIsSpeaking(false)
  }, [])

  // 음성 로드 완료 대기 후 speak (초기 로드 경쟁 조건 해결)
  const speakWhenReady = useCallback(
    (text: string) => {
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
      }

      window.speechSynthesis.addEventListener('voiceschanged', onVoicesLoaded)
    },
    [speak],
  )

  return { speak, speakWhenReady, stop, isSpeaking, isAvailable }
}
