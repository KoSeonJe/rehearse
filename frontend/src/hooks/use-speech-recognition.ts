import { useCallback, useEffect, useRef, useState } from 'react'
import type { TranscriptSegment } from '@/types/interview'

interface UseSpeechRecognitionReturn {
  isListening: boolean
  isSupported: boolean
  interimText: string
  start: (questionIndex: number) => void
  stop: () => void
  onFinalResult: (callback: (segment: TranscriptSegment) => void) => void
}

interface SpeechRecognitionEvent {
  resultIndex: number
  results: SpeechRecognitionResultList
}

interface SpeechRecognitionInstance extends EventTarget {
  continuous: boolean
  interimResults: boolean
  lang: string
  start: () => void
  stop: () => void
  abort: () => void
  onresult: ((event: SpeechRecognitionEvent) => void) | null
  onerror: ((event: { error: string }) => void) | null
  onend: (() => void) | null
}

declare global {
  interface Window {
    SpeechRecognition: new () => SpeechRecognitionInstance
    webkitSpeechRecognition: new () => SpeechRecognitionInstance
  }
}

const MAX_NETWORK_RETRIES = 3
const SESSION_TIMEOUT_MS = 65000
const BACKOFF_BASE_MS = 1000

export const useSpeechRecognition = (): UseSpeechRecognitionReturn => {
  const [isListening, setIsListening] = useState(false)
  const [interimText, setInterimText] = useState('')
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null)
  const callbackRef = useRef<((segment: TranscriptSegment) => void) | null>(null)
  const questionIndexRef = useRef(0)
  const startTimeRef = useRef(0)
  const shouldRestartRef = useRef(false)
  const networkRetryCountRef = useRef(0)
  const sessionTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const isSupported =
    typeof window !== 'undefined' &&
    ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)

  const createRecognition = useCallback((): SpeechRecognitionInstance | null => {
    if (!isSupported) return null

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    const recognition = new SpeechRecognition()
    recognition.continuous = true
    recognition.interimResults = true
    recognition.lang = 'ko-KR'

    recognition.onresult = (event: SpeechRecognitionEvent) => {
      let interim = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          const text = result[0].transcript.trim()
          if (text && callbackRef.current) {
            callbackRef.current({
              questionIndex: questionIndexRef.current,
              text,
              startTime: startTimeRef.current,
              endTime: Date.now(),
              isFinal: true,
            })
            startTimeRef.current = Date.now()
          }
          setInterimText('')
        } else {
          interim += result[0].transcript
        }
      }

      if (interim) {
        setInterimText(interim)
      }
    }

    recognition.onerror = (event: { error: string }) => {
      if (event.error === 'no-speech' || event.error === 'aborted') return

      if (event.error === 'network') {
        // 네트워크 에러: 지수 백오프 재시도 (최대 3회)
        if (networkRetryCountRef.current < MAX_NETWORK_RETRIES) {
          const delay = Math.pow(2, networkRetryCountRef.current) * BACKOFF_BASE_MS
          networkRetryCountRef.current++
          setTimeout(() => {
            if (shouldRestartRef.current && recognitionRef.current) {
              try {
                recognitionRef.current.start()
              } catch {
                setIsListening(false)
              }
            }
          }, delay)
          return
        }
      }

      setIsListening(false)
    }

    recognition.onend = () => {
      // 세션 타임아웃 타이머 초기화 (재시작 시 새로 설정)
      if (sessionTimeoutRef.current) {
        clearTimeout(sessionTimeoutRef.current)
        sessionTimeoutRef.current = null
      }

      if (shouldRestartRef.current) {
        networkRetryCountRef.current = 0 // 정상 재시작 시 카운터 리셋
        try {
          recognition.start()
          // 65초 백업 타이머 (onend 미호출 대비)
          sessionTimeoutRef.current = setTimeout(() => {
            if (shouldRestartRef.current && recognitionRef.current) {
              try {
                recognitionRef.current.stop()
              } catch {
                // stop 실패 시 무시
              }
            }
          }, SESSION_TIMEOUT_MS)
        } catch {
          setIsListening(false)
        }
      } else {
        setIsListening(false)
      }
    }

    return recognition
  }, [isSupported])

  const start = useCallback(
    (questionIndex: number) => {
      if (!isSupported) return

      // 기존 인스턴스 정리 (누수 방지)
      if (recognitionRef.current) {
        shouldRestartRef.current = false
        recognitionRef.current.abort()
        recognitionRef.current = null
      }
      if (sessionTimeoutRef.current) {
        clearTimeout(sessionTimeoutRef.current)
        sessionTimeoutRef.current = null
      }

      questionIndexRef.current = questionIndex
      startTimeRef.current = Date.now()
      shouldRestartRef.current = true
      networkRetryCountRef.current = 0

      const recognition = createRecognition()
      if (!recognition) return

      recognitionRef.current = recognition

      try {
        recognition.start()
        setIsListening(true)
        setInterimText('')

        // 65초 백업 타이머 (onend 미호출 대비)
        sessionTimeoutRef.current = setTimeout(() => {
          if (shouldRestartRef.current && recognitionRef.current) {
            try {
              recognitionRef.current.stop()
            } catch {
              // stop 실패 시 무시
            }
          }
        }, SESSION_TIMEOUT_MS)
      } catch {
        setIsListening(false)
      }
    },
    [isSupported, createRecognition],
  )

  const stop = useCallback(() => {
    shouldRestartRef.current = false
    if (sessionTimeoutRef.current) {
      clearTimeout(sessionTimeoutRef.current)
      sessionTimeoutRef.current = null
    }
    if (recognitionRef.current) {
      recognitionRef.current.stop()
      recognitionRef.current = null
    }
    setIsListening(false)
    setInterimText('')
  }, [])

  const onFinalResult = useCallback((callback: (segment: TranscriptSegment) => void) => {
    callbackRef.current = callback
  }, [])

  useEffect(() => {
    return () => {
      shouldRestartRef.current = false
      if (sessionTimeoutRef.current) {
        clearTimeout(sessionTimeoutRef.current)
      }
      if (recognitionRef.current) {
        recognitionRef.current.abort()
      }
    }
  }, [])

  return { isListening, isSupported, interimText, start, stop, onFinalResult }
}

