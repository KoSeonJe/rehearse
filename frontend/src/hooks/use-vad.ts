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

const CALIBRATION_DURATION_MS = 2000
const NOISE_EMA_ALPHA = 0.05
const NOISE_MARGIN = 1.5
const MIN_THRESHOLD = 0.03
const MAX_THRESHOLD = 0.85

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

  // 적응형 threshold
  const calibrationStartRef = useRef<number | null>(null)
  const calibrationSamplesRef = useRef<number[]>([])
  const noiseEstimateRef = useRef(0)
  const adaptiveThresholdRef = useRef(speechThreshold)

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
    calibrationStartRef.current = Date.now()
    calibrationSamplesRef.current = []
    adaptiveThresholdRef.current = speechThresholdRef.current

    const tick = () => {
      const now = Date.now()
      const level = audioLevelRef.current

      // 초기 캘리브레이션 (첫 2초간 배경 소음 측정)
      if (calibrationStartRef.current && now - calibrationStartRef.current < CALIBRATION_DURATION_MS) {
        calibrationSamplesRef.current.push(level)
        // 캘리브레이션 중에는 음성 감지하지 않음
        rafRef.current = requestAnimationFrame(tick)
        return
      }

      // 캘리브레이션 완료 → 적응형 threshold 설정
      if (calibrationStartRef.current && calibrationSamplesRef.current.length > 0) {
        const avgNoise = calibrationSamplesRef.current.reduce((a, b) => a + b, 0) / calibrationSamplesRef.current.length
        noiseEstimateRef.current = avgNoise
        adaptiveThresholdRef.current = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, avgNoise * NOISE_MARGIN + speechThresholdRef.current * 0.5))
        calibrationStartRef.current = null
        calibrationSamplesRef.current = []
      }

      // EMA로 노이즈 추정값 지속 갱신 (순수 대기 상태에서만)
      const state = vadStateRef.current
      if (state === 'listening') {
        noiseEstimateRef.current = noiseEstimateRef.current * (1 - NOISE_EMA_ALPHA) + level * NOISE_EMA_ALPHA
        adaptiveThresholdRef.current = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, noiseEstimateRef.current * NOISE_MARGIN + speechThresholdRef.current * 0.5))
      }

      const isSpeaking = level > adaptiveThresholdRef.current

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
