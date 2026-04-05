import { useCallback, useEffect, useRef, useState } from 'react'

const API_BASE_URL = import.meta.env.VITE_API_URL || ''
const IS_ANDROID = typeof navigator !== 'undefined' && /Android/i.test(navigator.userAgent)

interface UseTtsOptions {
  onStart?: () => void
  onEnd?: () => void
}

export const useTts = ({ onStart, onEnd }: UseTtsOptions = {}) => {
  const [isSpeaking, setIsSpeaking] = useState(false)
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const abortControllerRef = useRef<AbortController | null>(null)
  const resumeIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const utteranceIdRef = useRef(0)
  const voiceRef = useRef<SpeechSynthesisVoice | null>(null)
  const onStartRef = useRef(onStart)
  const onEndRef = useRef(onEnd)

  onStartRef.current = onStart
  onEndRef.current = onEnd

  // Web Speech API 음성 초기화 (폴백용)
  useEffect(() => {
    if (typeof window === 'undefined' || !window.speechSynthesis) return
    const selectVoice = () => {
      const voices = window.speechSynthesis.getVoices()
      voiceRef.current = voices.find((v) => v.lang.startsWith('ko')) ?? null
    }
    selectVoice()
    window.speechSynthesis.addEventListener('voiceschanged', selectVoice)
    return () => {
      window.speechSynthesis.removeEventListener('voiceschanged', selectVoice)
      window.speechSynthesis.cancel()
    }
  }, [])

  const stop = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current.src = ''
      audioRef.current = null
    }
    if (resumeIntervalRef.current) {
      clearInterval(resumeIntervalRef.current)
      resumeIntervalRef.current = null
    }
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      window.speechSynthesis.cancel()
    }
    setIsSpeaking(false)
  }, [])

  // Web Speech API 폴백
  const speakWithBrowser = useCallback((text: string) => {
    if (!window.speechSynthesis) {
      onEndRef.current?.()
      return
    }
    const id = ++utteranceIdRef.current
    window.speechSynthesis.cancel()
    setIsSpeaking(true)
    onStartRef.current?.()

    const utterance = new SpeechSynthesisUtterance(text)
    if (voiceRef.current) utterance.voice = voiceRef.current
    utterance.lang = 'ko-KR'
    utterance.rate = 0.95

    utterance.onstart = () => {
      if (!IS_ANDROID) {
        resumeIntervalRef.current = setInterval(() => {
          if (window.speechSynthesis.speaking) {
            window.speechSynthesis.pause()
            window.speechSynthesis.resume()
          } else {
            clearInterval(resumeIntervalRef.current!)
            resumeIntervalRef.current = null
          }
        }, 14000)
      }
    }

    utterance.onend = () => {
      if (utteranceIdRef.current !== id) return
      if (resumeIntervalRef.current) {
        clearInterval(resumeIntervalRef.current)
        resumeIntervalRef.current = null
      }
      setIsSpeaking(false)
      onEndRef.current?.()
    }

    utterance.onerror = () => {
      if (utteranceIdRef.current !== id) return
      if (resumeIntervalRef.current) {
        clearInterval(resumeIntervalRef.current)
        resumeIntervalRef.current = null
      }
      setIsSpeaking(false)
      onEndRef.current?.()
    }

    window.speechSynthesis.speak(utterance)
  }, [])

  const speak = useCallback(
    async (text: string) => {
      if (!text.trim()) return
      stop()

      const abortController = new AbortController()
      abortControllerRef.current = abortController

      try {
        const response = await fetch(`${API_BASE_URL}/api/v1/tts`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ text }),
          signal: abortController.signal,
        })

        if (!response.ok) {
          abortControllerRef.current = null
          speakWithBrowser(text)
          return
        }

        const arrayBuffer = await response.arrayBuffer()
        if (abortController.signal.aborted) return

        const blob = new Blob([arrayBuffer], { type: 'audio/mpeg' })
        const url = URL.createObjectURL(blob)
        const audio = new Audio(url)
        audioRef.current = audio
        abortControllerRef.current = null

        setIsSpeaking(true)
        onStartRef.current?.()

        audio.onended = () => {
          URL.revokeObjectURL(url)
          audioRef.current = null
          setIsSpeaking(false)
          onEndRef.current?.()
        }

        audio.onerror = () => {
          URL.revokeObjectURL(url)
          audioRef.current = null
          speakWithBrowser(text)
        }

        await audio.play()
      } catch (e) {
        if (e instanceof DOMException && e.name === 'AbortError') return
        speakWithBrowser(text)
      }
    },
    [stop, speakWithBrowser],
  )

  return { speak, speakWhenReady: speak, stop, isSpeaking, isAvailable: true }
}