import { useCallback, useEffect, useRef } from 'react'

interface UseVadOptions {
  enabled: boolean
  speechThreshold?: number
  speechStartDelay?: number
  silenceEndDelay?: number
  onSpeechStart?: () => void
  onSpeechEnd?: () => void
}

type VadState = 'idle' | 'listening' | 'speaking' | 'silent'

export const useVad = ({
  enabled,
  speechThreshold = 0.08,
  speechStartDelay = 500,
  silenceEndDelay = 3000,
  onSpeechStart,
  onSpeechEnd,
}: UseVadOptions) => {
  const vadStateRef = useRef<VadState>('idle')
  const speechStartTimeRef = useRef<number | null>(null)
  const silenceStartTimeRef = useRef<number | null>(null)
  const audioLevelRef = useRef(0)
  const onSpeechStartRef = useRef(onSpeechStart)
  const onSpeechEndRef = useRef(onSpeechEnd)
  const rafRef = useRef<number | null>(null)

  useEffect(() => {
    onSpeechStartRef.current = onSpeechStart
  })

  useEffect(() => {
    onSpeechEndRef.current = onSpeechEnd
  })

  useEffect(() => {
    if (!enabled) {
      vadStateRef.current = 'idle'
      speechStartTimeRef.current = null
      silenceStartTimeRef.current = null
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current)
        rafRef.current = null
      }
      return
    }

    vadStateRef.current = 'listening'

    const tick = () => {
      const now = Date.now()
      const level = audioLevelRef.current
      const isSpeaking = level > speechThreshold
      const state = vadStateRef.current

      if (isSpeaking) {
        silenceStartTimeRef.current = null

        if (state === 'listening' || state === 'silent') {
          if (!speechStartTimeRef.current) {
            speechStartTimeRef.current = now
          } else if (now - speechStartTimeRef.current >= speechStartDelay) {
            vadStateRef.current = 'speaking'
            onSpeechStartRef.current?.()
          }
        }
      } else {
        speechStartTimeRef.current = null

        if (state === 'speaking') {
          if (!silenceStartTimeRef.current) {
            silenceStartTimeRef.current = now
          } else if (now - silenceStartTimeRef.current >= silenceEndDelay) {
            vadStateRef.current = 'silent'
            onSpeechEndRef.current?.()
          }
        }
      }

      rafRef.current = requestAnimationFrame(tick)
    }

    rafRef.current = requestAnimationFrame(tick)

    return () => {
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current)
        rafRef.current = null
      }
    }
  }, [enabled, speechThreshold, speechStartDelay, silenceEndDelay])

  const updateAudioLevel = useCallback((level: number) => {
    audioLevelRef.current = level
  }, [])

  const reset = useCallback(() => {
    vadStateRef.current = enabled ? 'listening' : 'idle'
    speechStartTimeRef.current = null
    silenceStartTimeRef.current = null
  }, [enabled])

  return {
    isActive: enabled,
    updateAudioLevel,
    reset,
  }
}
