import { useCallback, useEffect, useRef, useState } from 'react'
import type { VoiceEvent } from '../types/interview'

interface UseAudioAnalyzerReturn {
  audioLevel: number
  start: (stream: MediaStream) => void
  stop: () => void
  onVoiceEvent: (callback: (event: VoiceEvent) => void) => void
}

const SILENCE_THRESHOLD = -50
const SILENCE_DURATION_MS = 3000
const ANALYSIS_INTERVAL_MS = 100

const useAudioAnalyzer = (): UseAudioAnalyzerReturn => {
  const [audioLevel, setAudioLevel] = useState(0)
  const contextRef = useRef<AudioContext | null>(null)
  const analyzerRef = useRef<AnalyserNode | null>(null)
  const sourceRef = useRef<MediaStreamAudioSourceNode | null>(null)
  const rafRef = useRef<number | null>(null)
  const callbackRef = useRef<((event: VoiceEvent) => void) | null>(null)
  const silenceStartRef = useRef<number | null>(null)
  const lastAnalysisRef = useRef(0)

  const start = useCallback((stream: MediaStream) => {
    const context = new AudioContext()
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

      analyzerRef.current.getFloatTimeDomainData(dataArray)

      let sum = 0
      for (let i = 0; i < dataArray.length; i++) {
        sum += dataArray[i] * dataArray[i]
      }
      const rms = Math.sqrt(sum / dataArray.length)
      const db = rms > 0 ? 20 * Math.log10(rms) : -100
      const normalized = Math.max(0, Math.min(1, (db + 60) / 60))

      setAudioLevel(normalized)

      const now = Date.now()
      if (now - lastAnalysisRef.current >= ANALYSIS_INTERVAL_MS) {
        lastAnalysisRef.current = now

        if (db < SILENCE_THRESHOLD) {
          if (!silenceStartRef.current) {
            silenceStartRef.current = now
          } else if (now - silenceStartRef.current >= SILENCE_DURATION_MS) {
            if (callbackRef.current) {
              callbackRef.current({
                timestamp: silenceStartRef.current,
                type: 'silence',
                duration: (now - silenceStartRef.current) / 1000,
                value: db,
              })
            }
            silenceStartRef.current = now
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
    setAudioLevel(0)
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

  return { audioLevel, start, stop, onVoiceEvent }
}

export default useAudioAnalyzer
