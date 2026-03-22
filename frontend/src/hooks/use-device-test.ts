import { useState, useRef, useCallback, useEffect } from 'react'
import type { DeviceTestState, DeviceTestStatus } from '@/types/device'

const MIC_THRESHOLD = 8

export const useDeviceTest = () => {
  const [state, setState] = useState<DeviceTestState>({
    camera: 'idle',
    microphone: 'idle',
    speaker: 'idle',
  })
  const [micLevel, setMicLevel] = useState(0)

  const videoRef = useRef<HTMLVideoElement | null>(null)
  const cameraStreamRef = useRef<MediaStream | null>(null)
  const micStreamRef = useRef<MediaStream | null>(null)
  const audioCtxRef = useRef<AudioContext | null>(null)
  const animFrameRef = useRef<number>(0)

  const updateStatus = useCallback((device: keyof DeviceTestState, status: DeviceTestStatus) => {
    setState((prev) => ({ ...prev, [device]: status }))
  }, [])

  const stopCameraStream = useCallback(() => {
    if (cameraStreamRef.current) {
      for (const track of cameraStreamRef.current.getTracks()) track.stop()
      cameraStreamRef.current = null
    }
    if (videoRef.current) videoRef.current.srcObject = null
  }, [])

  const stopMicStream = useCallback(() => {
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current)
      animFrameRef.current = 0
    }
    if (audioCtxRef.current) {
      audioCtxRef.current.close().catch(() => {})
      audioCtxRef.current = null
    }
    if (micStreamRef.current) {
      for (const track of micStreamRef.current.getTracks()) track.stop()
      micStreamRef.current = null
    }
    setMicLevel(0)
  }, [])

  const startCameraTest = useCallback(async () => {
    updateStatus('camera', 'testing')
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true })
      cameraStreamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
      }
      updateStatus('camera', 'passed')
    } catch {
      updateStatus('camera', 'denied')
    }
  }, [updateStatus])

  const startMicTest = useCallback(async () => {
    updateStatus('microphone', 'testing')
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      micStreamRef.current = stream

      const audioCtx = new AudioContext()
      audioCtxRef.current = audioCtx
      const source = audioCtx.createMediaStreamSource(stream)
      const analyser = audioCtx.createAnalyser()
      analyser.fftSize = 256
      source.connect(analyser)

      const dataArray = new Uint8Array(analyser.frequencyBinCount)
      let detected = false

      const tick = () => {
        analyser.getByteFrequencyData(dataArray)
        let sum = 0
        for (let i = 0; i < dataArray.length; i++) sum += dataArray[i]
        const avg = sum / dataArray.length
        const level = Math.round((avg / 255) * 100)
        setMicLevel(level)

        if (!detected && level > MIC_THRESHOLD) {
          detected = true
          updateStatus('microphone', 'passed')
        }

        animFrameRef.current = requestAnimationFrame(tick)
      }
      tick()
    } catch {
      updateStatus('microphone', 'denied')
    }
  }, [updateStatus])

  const startSpeakerTest = useCallback(() => {
    updateStatus('speaker', 'testing')
    const ctx = new AudioContext()
    const oscillator = ctx.createOscillator()
    const gain = ctx.createGain()
    oscillator.type = 'sine'
    oscillator.frequency.setValueAtTime(440, ctx.currentTime)
    gain.gain.setValueAtTime(0.15, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.5)
    oscillator.connect(gain)
    gain.connect(ctx.destination)
    oscillator.start()
    oscillator.stop(ctx.currentTime + 1.5)
    oscillator.onended = () => ctx.close().catch(() => {})
  }, [updateStatus])

  const confirmSpeaker = useCallback(() => {
    updateStatus('speaker', 'passed')
  }, [updateStatus])

  const resetMicTest = useCallback(() => {
    stopMicStream()
    updateStatus('microphone', 'idle')
  }, [stopMicStream, updateStatus])

  const resetSpeakerTest = useCallback(() => {
    updateStatus('speaker', 'idle')
  }, [updateStatus])

  const allPassed = state.camera === 'passed' && state.microphone === 'passed' && state.speaker === 'passed'

  useEffect(() => {
    return () => {
      stopCameraStream()
      stopMicStream()
    }
  }, [stopCameraStream, stopMicStream])

  return {
    state,
    micLevel,
    videoRef,
    allPassed,
    startCameraTest,
    startMicTest,
    startSpeakerTest,
    confirmSpeaker,
    resetMicTest,
    resetSpeakerTest,
  }
}
