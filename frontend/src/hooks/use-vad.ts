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

  // Dynamic params as refs — changes don't reset the VAD loop
  const speechThresholdRef = useRef(speechThreshold)
  const speechStartDelayRef = useRef(speechStartDelay)
  const silenceEndDelayRef = useRef(silenceEndDelay)

  useEffect(() => {
    speechThresholdRef.current = speechThreshold
  }, [speechThreshold])

  useEffect(() => {
    speechStartDelayRef.current = speechStartDelay
  }, [speechStartDelay])

  useEffect(() => {
    silenceEndDelayRef.current = silenceEndDelay
  }, [silenceEndDelay])

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
      const isSpeaking = level > speechThresholdRef.current
      const state = vadStateRef.current

      if (isSpeaking) {
        silenceStartTimeRef.current = null

        if (state === 'listening' || state === 'silent') {
          if (!speechStartTimeRef.current) {
            speechStartTimeRef.current = now
          } else if (now - speechStartTimeRef.current >= speechStartDelayRef.current) {
            vadStateRef.current = 'speaking'
            onSpeechStartRef.current?.()
          }
        }
      } else {
        speechStartTimeRef.current = null

        if (state === 'speaking') {
          if (!silenceStartTimeRef.current) {
            silenceStartTimeRef.current = now
          } else if (now - silenceStartTimeRef.current >= silenceEndDelayRef.current) {
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
  }, [enabled])

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
