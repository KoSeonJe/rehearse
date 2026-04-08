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
  // 세대(generation) 카운터 — speak() 호출 / stop() 시 증가.
  // 모든 비동기 이벤트(audio.onerror, fetch resolve, play() reject, utterance.onend 등)는
  // 발급 당시 id를 캡처하여 현재 세대와 다르면 무시한다 → stop 이후 유령 발화 차단.
  const utteranceIdRef = useRef(0)
  const voiceRef = useRef<SpeechSynthesisVoice | null>(null)
  const onStartRef = useRef(onStart)
  const onEndRef = useRef(onEnd)

  useEffect(() => {
    onStartRef.current = onStart
    onEndRef.current = onEnd
  })

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
    // 세대 증가 → 진행 중인 speak()의 모든 후속 콜백이 가드에 걸려 무시됨.
    utteranceIdRef.current += 1

    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }
    if (audioRef.current) {
      // 핸들러를 먼저 분리한 뒤 src 제거 → audio.onerror 폴백 재발화 원천 차단.
      // src='' 대신 removeAttribute('src') + load() 패턴 사용 (MDN 권장).
      // src='' 은 일부 브라우저에서 MEDIA_ERR_SRC_NOT_SUPPORTED 로 error 이벤트를 유발.
      const a = audioRef.current
      audioRef.current = null
      a.onerror = null
      a.onended = null
      a.onabort = null
      a.onstalled = null
      a.onsuspend = null
      try {
        a.pause()
      } catch {
        // noop
      }
      try {
        a.removeAttribute('src')
        a.load()
      } catch {
        // noop
      }
    }
    if (resumeIntervalRef.current) {
      clearInterval(resumeIntervalRef.current)
      resumeIntervalRef.current = null
    }
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      // Chrome race condition 회피 — 두 번 호출
      window.speechSynthesis.cancel()
      window.speechSynthesis.cancel()
    }
    setIsSpeaking(false)
  }, [])

  // Web Speech API 폴백
  // ownerId 미지정 → 새 세대 발급 (외부 단독 호출용)
  // ownerId 지정 → 지정 세대가 현재 세대와 동일할 때만 진행 (speak() 경로에서 폴백 체인용)
  const speakWithBrowser = useCallback((text: string, ownerId?: number) => {
    if (!window.speechSynthesis) {
      if (ownerId === undefined || utteranceIdRef.current === ownerId) {
        setIsSpeaking(false)
        onEndRef.current?.()
      }
      return
    }

    let id: number
    if (ownerId === undefined) {
      id = ++utteranceIdRef.current
    } else {
      if (utteranceIdRef.current !== ownerId) return
      id = ownerId
    }

    window.speechSynthesis.cancel()
    setIsSpeaking(true)
    onStartRef.current?.()

    const utterance = new SpeechSynthesisUtterance(text)
    if (voiceRef.current) utterance.voice = voiceRef.current
    utterance.lang = 'ko-KR'
    utterance.rate = 0.95

    utterance.onstart = () => {
      if (utteranceIdRef.current !== id) return
      if (!IS_ANDROID) {
        resumeIntervalRef.current = setInterval(() => {
          if (window.speechSynthesis.speaking) {
            window.speechSynthesis.pause()
            window.speechSynthesis.resume()
          } else {
            if (resumeIntervalRef.current) {
              clearInterval(resumeIntervalRef.current)
              resumeIntervalRef.current = null
            }
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
      // stop() 이 이미 세대를 +1 했으므로 그 값이 곧 새 speak 의 id.
      // 여기서 또 증가시키면 stop 호출 직전에 큐잉된 onended 가 가드에
      // 의해 무효화되어 pendingTtsActionRef 예약 액션이 누락될 수 있다.
      const id = utteranceIdRef.current

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

        if (utteranceIdRef.current !== id) return

        if (!response.ok) {
          abortControllerRef.current = null
          speakWithBrowser(text, id)
          return
        }

        const arrayBuffer = await response.arrayBuffer()
        if (abortController.signal.aborted) return
        if (utteranceIdRef.current !== id) return

        const blob = new Blob([arrayBuffer], { type: 'audio/mpeg' })
        const url = URL.createObjectURL(blob)
        const audio = new Audio(url)
        audioRef.current = audio
        abortControllerRef.current = null

        setIsSpeaking(true)
        onStartRef.current?.()

        audio.onended = () => {
          if (utteranceIdRef.current !== id) return
          URL.revokeObjectURL(url)
          audioRef.current = null
          setIsSpeaking(false)
          onEndRef.current?.()
        }

        audio.onerror = () => {
          // stop() 에 의해 유발된 error 이면 세대가 이미 다르므로 폴백 차단
          if (utteranceIdRef.current !== id) {
            URL.revokeObjectURL(url)
            return
          }
          URL.revokeObjectURL(url)
          audioRef.current = null
          setIsSpeaking(false)
          speakWithBrowser(text, id)
        }

        try {
          await audio.play()
        } catch (playErr) {
          if (utteranceIdRef.current !== id) return
          if (playErr instanceof DOMException && playErr.name === 'AbortError') return
          // play() reject 시 폴백
          URL.revokeObjectURL(url)
          audioRef.current = null
          setIsSpeaking(false)
          speakWithBrowser(text, id)
        }
      } catch (e) {
        if (utteranceIdRef.current !== id) return
        if (e instanceof DOMException && e.name === 'AbortError') return
        speakWithBrowser(text, id)
      }
    },
    [stop, speakWithBrowser],
  )

  return { speak, speakWhenReady: speak, stop, isSpeaking, isAvailable: true }
}
