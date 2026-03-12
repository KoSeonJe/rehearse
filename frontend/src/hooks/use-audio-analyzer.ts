import { useCallback, useEffect, useRef } from 'react'
import type { VoiceEvent } from '../types/interview'

interface UseAudioAnalyzerReturn {
  audioLevelRef: React.RefObject<number>
  start: (stream: MediaStream) => Promise<void>
  stop: () => void
  onVoiceEvent: (callback: (event: VoiceEvent) => void) => void
}

const SILENCE_THRESHOLD = -50
const SILENCE_DURATION_MS = 3000
const ANALYSIS_INTERVAL_MS = 100
const FRAME_INTERVAL_MS = 33 // ~30fps

export const useAudioAnalyzer = (): UseAudioAnalyzerReturn => {
  const audioLevelRef = useRef(0)
  const contextRef = useRef<AudioContext | null>(null)
  const analyzerRef = useRef<AnalyserNode | null>(null)
  const sourceRef = useRef<MediaStreamAudioSourceNode | null>(null)
  const rafRef = useRef<number | null>(null)
  const callbackRef = useRef<((event: VoiceEvent) => void) | null>(null)
  const silenceStartRef = useRef<number | null>(null)
  const lastAnalysisRef = useRef(0)
  const lastTickRef = useRef(0)

  const start = useCallback(async (stream: MediaStream) => {
    if (contextRef.current) return // 이미 시작됨 — 중복 초기화 방지

    const context = new AudioContext()
    if (context.state === 'suspended') {
      await context.resume()
    }
    const analyzer = context.createAnalyser()
    analyzer.fftSize = 256
    analyzer.smoothingTimeConstant = 0.8

    const source = context.createMediaStreamSource(stream)
    source.connect(analyzer)

    contextRef.current = context
    analyzerRef.current = analyzer
    sourceRef.current = source
    silenceStartRef.current = null

    const dataArray = new Float32Array(analyzer.fftSize)

    const tick = () => {
      if (!analyzerRef.current) return

      // 30fps 스로틀
      const now = performance.now()
      if (now - lastTickRef.current < FRAME_INTERVAL_MS) {
        rafRef.current = requestAnimationFrame(tick)
        return
      }
      lastTickRef.current = now

      analyzerRef.current.getFloatTimeDomainData(dataArray)

      let sum = 0
      for (let i = 0; i < dataArray.length; i++) {
        sum += dataArray[i] * dataArray[i]
      }
      const rms = Math.sqrt(sum / dataArray.length)
      const db = rms > 0 ? 20 * Math.log10(rms) : -100
      const normalized = Math.max(0, Math.min(1, (db + 60) / 60))

      // ref만 업데이트 (Zustand/setState 호출 없음 → 리렌더 0)
      audioLevelRef.current = normalized

      const dateNow = Date.now()
      if (dateNow - lastAnalysisRef.current >= ANALYSIS_INTERVAL_MS) {
        lastAnalysisRef.current = dateNow

        if (db < SILENCE_THRESHOLD) {
          if (!silenceStartRef.current) {
            silenceStartRef.current = dateNow
          } else if (dateNow - silenceStartRef.current >= SILENCE_DURATION_MS) {
            if (callbackRef.current) {
              callbackRef.current({
                timestamp: silenceStartRef.current,
                type: 'silence',
                duration: (dateNow - silenceStartRef.current) / 1000,
                value: db,
              })
            }
            silenceStartRef.current = dateNow
          }
        } else {
          silenceStartRef.current = null
        }
      }

      rafRef.current = requestAnimationFrame(tick)
    }

    tick()
  }, [])

  const stop = useCallback(() => {
    if (rafRef.current) {
      cancelAnimationFrame(rafRef.current)
      rafRef.current = null
    }
    if (sourceRef.current) {
      sourceRef.current.disconnect()
      sourceRef.current = null
    }
    if (contextRef.current) {
      contextRef.current.close()
      contextRef.current = null
    }
    analyzerRef.current = null
    audioLevelRef.current = 0
  }, [])

  const onVoiceEvent = useCallback((callback: (event: VoiceEvent) => void) => {
    callbackRef.current = callback
  }, [])

  useEffect(() => {
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
      if (sourceRef.current) sourceRef.current.disconnect()
      if (contextRef.current) contextRef.current.close()
    }
  }, [])

  return { audioLevelRef, start, stop, onVoiceEvent }
}
