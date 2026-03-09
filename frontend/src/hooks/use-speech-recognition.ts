import { useCallback, useEffect, useRef, useState } from 'react'
import type { TranscriptSegment } from '../types/interview'

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

const useSpeechRecognition = (): UseSpeechRecognitionReturn => {
  const [isListening, setIsListening] = useState(false)
  const [interimText, setInterimText] = useState('')
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null)
  const callbackRef = useRef<((segment: TranscriptSegment) => void) | null>(null)
  const questionIndexRef = useRef(0)
  const startTimeRef = useRef(0)
  const shouldRestartRef = useRef(false)

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
      setIsListening(false)
    }

    recognition.onend = () => {
      if (shouldRestartRef.current) {
        try {
          recognition.start()
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

      questionIndexRef.current = questionIndex
      startTimeRef.current = Date.now()
      shouldRestartRef.current = true

      const recognition = createRecognition()
      if (!recognition) return

      recognitionRef.current = recognition

      try {
        recognition.start()
        setIsListening(true)
        setInterimText('')
      } catch {
        setIsListening(false)
      }
    },
    [isSupported, createRecognition],
  )

  const stop = useCallback(() => {
    shouldRestartRef.current = false
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
      if (recognitionRef.current) {
        recognitionRef.current.abort()
      }
    }
  }, [])

  return { isListening, isSupported, interimText, start, stop, onFinalResult }
}

export default useSpeechRecognition
