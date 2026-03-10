import { useCallback, useEffect, useRef, useState } from 'react'
import { PoseLandmarker, FilesetResolver } from '@mediapipe/tasks-vision'
import { analyzePosture } from '../lib/mediapipe/pose-analyzer'
import type { NonVerbalEvent } from '../types/interview'
import type { EventDetector } from '../lib/mediapipe/event-detector'

interface UsePoseDetectionReturn {
  isLoaded: boolean
  isRunning: boolean
  start: (video: HTMLVideoElement, detector: EventDetector) => void
  stop: () => void
  onEvent: (callback: (event: NonVerbalEvent) => void) => void
}

export const usePoseDetection = (): UsePoseDetectionReturn => {
  const [isLoaded, setIsLoaded] = useState(false)
  const [isRunning, setIsRunning] = useState(false)
  const landmarkerRef = useRef<PoseLandmarker | null>(null)
  const rafRef = useRef<number | null>(null)
  const callbackRef = useRef<((event: NonVerbalEvent) => void) | null>(null)
  const lastTimeRef = useRef(-1)

  useEffect(() => {
    let cancelled = false

    const init = async () => {
      const filesetResolver = await FilesetResolver.forVisionTasks(
        'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm',
      )
      if (cancelled) return

      const landmarker = await PoseLandmarker.createFromOptions(filesetResolver, {
        baseOptions: {
          modelAssetPath:
            'https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task',
          delegate: 'GPU',
        },
        runningMode: 'VIDEO',
        numPoses: 1,
      })
      if (cancelled) return

      landmarkerRef.current = landmarker
      setIsLoaded(true)
    }

    init()

    return () => {
      cancelled = true
    }
  }, [])

  const start = useCallback((video: HTMLVideoElement, detector: EventDetector) => {
    if (!landmarkerRef.current) return
    setIsRunning(true)

    const tick = () => {
      if (!landmarkerRef.current || video.readyState < 2) {
        rafRef.current = requestAnimationFrame(tick)
        return
      }

      const now = performance.now()
      if (now === lastTimeRef.current) {
        rafRef.current = requestAnimationFrame(tick)
        return
      }
      lastTimeRef.current = now

      const result = landmarkerRef.current.detectForVideo(video, now)
      const posture = analyzePosture(result)

      if (posture) {
        const event = detector.checkPosture(posture.shoulderTiltDegrees)
        if (event && callbackRef.current) {
          callbackRef.current(event)
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
    setIsRunning(false)
    lastTimeRef.current = -1
  }, [])

  const onEvent = useCallback((callback: (event: NonVerbalEvent) => void) => {
    callbackRef.current = callback
  }, [])

  useEffect(() => {
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
      landmarkerRef.current?.close()
    }
  }, [])

  return { isLoaded, isRunning, start, stop, onEvent }
}

