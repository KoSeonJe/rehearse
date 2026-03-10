import { useState, useEffect, useRef, useCallback } from 'react'
import type { DevicePermissions } from '@/types/device'

export const useDeviceTest = (active: boolean) => {
  const [permissions, setPermissions] = useState<DevicePermissions>({
    camera: 'idle',
    microphone: 'idle',
  })
  const [micLevel, setMicLevel] = useState(0)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const audioCtxRef = useRef<AudioContext | null>(null)
  const animFrameRef = useRef<number>(0)

  const cleanup = useCallback(() => {
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current)
      animFrameRef.current = 0
    }
    if (audioCtxRef.current) {
      audioCtxRef.current.close().catch(() => {})
      audioCtxRef.current = null
    }
    if (streamRef.current) {
      for (const track of streamRef.current.getTracks()) {
        track.stop()
      }
      streamRef.current = null
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null
    }
    setMicLevel(0)
  }, [])

  useEffect(() => {
    if (!active) return

    let cancelled = false

    const init = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: true,
        })

        if (cancelled) {
          for (const track of stream.getTracks()) track.stop()
          return
        }

        streamRef.current = stream

        // Camera
        if (videoRef.current) {
          videoRef.current.srcObject = stream
        }
        setPermissions((prev) => ({ ...prev, camera: 'granted' }))

        // Microphone level
        const audioCtx = new AudioContext()
        audioCtxRef.current = audioCtx
        const source = audioCtx.createMediaStreamSource(stream)
        const analyser = audioCtx.createAnalyser()
        analyser.fftSize = 256
        source.connect(analyser)

        const dataArray = new Uint8Array(analyser.frequencyBinCount)
        let prevLevel = 0

        const tick = () => {
          if (cancelled) return
          analyser.getByteFrequencyData(dataArray)
          let sum = 0
          for (let i = 0; i < dataArray.length; i++) {
            sum += dataArray[i]
          }
          const avg = sum / dataArray.length
          const newLevel = Math.round((avg / 255) * 100)
          if (Math.abs(newLevel - prevLevel) >= 2) {
            prevLevel = newLevel
            setMicLevel(newLevel)
          }
          animFrameRef.current = requestAnimationFrame(tick)
        }
        tick()

        setPermissions((prev) => ({ ...prev, microphone: 'granted' }))
      } catch {
        if (cancelled) return
        setPermissions({ camera: 'denied', microphone: 'denied' })
      }
    }

    init()

    return () => {
      cancelled = true
      cleanup()
    }
  }, [active, cleanup])

  return { permissions, micLevel, videoRef }
}
