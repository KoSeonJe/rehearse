import { useCallback, useRef, useState } from 'react'

interface UseMediaRecorderReturn {
  isRecording: boolean
  start: (stream: MediaStream) => void
  stop: () => Promise<Blob>
  pause: () => void
  resume: () => void
}

export const useMediaRecorder = (): UseMediaRecorderReturn => {
  const [isRecording, setIsRecording] = useState(false)
  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])

  const start = useCallback((stream: MediaStream) => {
    chunksRef.current = []

    const mimeType = MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
      ? 'video/webm;codecs=vp9'
      : 'video/webm'

    const recorder = new MediaRecorder(stream, { mimeType })

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunksRef.current.push(event.data)
      }
    }

    recorderRef.current = recorder
    recorder.start(1000)
    setIsRecording(true)
  }, [])

  const stop = useCallback((): Promise<Blob> => {
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
  }, [])

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

  return { isRecording, start, stop, pause, resume }
}

