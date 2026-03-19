import { useCallback, useRef, useState } from 'react'

interface UseMediaRecorderReturn {
  isRecording: boolean
  start: (stream: MediaStream) => void
  stop: () => Promise<Blob>
  pause: () => void
  resume: () => void
  restart: (stream: MediaStream) => Promise<Blob>
}

const SUPPORTED_CODECS = [
  'video/webm;codecs=vp9,opus',
  'video/webm;codecs=vp8,opus',
  'video/webm',
]

export const useMediaRecorder = (): UseMediaRecorderReturn => {
  const [isRecording, setIsRecording] = useState(false)
  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const dataIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const clearDataInterval = useCallback(() => {
    if (dataIntervalRef.current) {
      clearInterval(dataIntervalRef.current)
      dataIntervalRef.current = null
    }
  }, [])

  const start = useCallback((stream: MediaStream) => {
    chunksRef.current = []

    const mimeType = SUPPORTED_CODECS.find((c) => MediaRecorder.isTypeSupported(c)) ?? 'video/webm'

    const recorder = new MediaRecorder(stream, {
      mimeType,
      audioBitsPerSecond: 128_000,
      videoBitsPerSecond: 2_500_000,
    })

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunksRef.current.push(event.data)
      }
    }

    recorderRef.current = recorder
    recorder.start()
    setIsRecording(true)

    dataIntervalRef.current = setInterval(() => {
      if (recorder.state === 'recording') recorder.requestData()
    }, 5000)
  }, [])

  const stop = useCallback((): Promise<Blob> => {
    clearDataInterval()
    return new Promise((resolve) => {
      const recorder = recorderRef.current
      if (!recorder || recorder.state === 'inactive') {
        resolve(new Blob(chunksRef.current, { type: 'video/webm' }))
        return
      }

      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType })
        setIsRecording(false)
        recorderRef.current = null
        resolve(blob)
      }

      recorder.stop()
    })
  }, [clearDataInterval])

  const pause = useCallback(() => {
    if (recorderRef.current?.state === 'recording') {
      recorderRef.current.pause()
    }
  }, [])

  const resume = useCallback(() => {
    if (recorderRef.current?.state === 'paused') {
      recorderRef.current.resume()
    }
  }, [])

  // 질문세트 전환: 현재 녹화 stop → blob 반환 → 새 녹화 start
  const restart = useCallback(async (stream: MediaStream): Promise<Blob> => {
    const blob = await stop()
    start(stream)
    return blob
  }, [stop, start])

  return { isRecording, start, stop, pause, resume, restart }
}
