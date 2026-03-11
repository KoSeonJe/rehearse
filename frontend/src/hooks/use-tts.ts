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
      if (!isAvailable || !voiceRef.current) return

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
    [isAvailable, lang, rate],
  )

  const stop = useCallback(() => {
    window.speechSynthesis.cancel()
    setIsSpeaking(false)
  }, [])

  return { speak, stop, isSpeaking, isAvailable }
}
