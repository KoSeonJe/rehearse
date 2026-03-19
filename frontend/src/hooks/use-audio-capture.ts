import { useCallback, useRef } from 'react'

export const useAudioCapture = () => {
  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])

  const start = useCallback((stream: MediaStream) => {
    const audioTracks = stream.getAudioTracks()
    if (audioTracks.length === 0) return

    const audioStream = new MediaStream(audioTracks)
    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : 'audio/webm'

    const recorder = new MediaRecorder(audioStream, {
      mimeType,
      audioBitsPerSecond: 128_000,
    })
    chunksRef.current = []
    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunksRef.current.push(e.data)
    }
    recorder.start()
    recorderRef.current = recorder
  }, [])

  const stop = useCallback((): Promise<Blob> => {
    return new Promise((resolve) => {
      const recorder = recorderRef.current
      if (!recorder || recorder.state === 'inactive') {
        resolve(new Blob([], { type: 'audio/webm' }))
        return
      }
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' })
        chunksRef.current = []
        recorderRef.current = null
        resolve(blob)
      }
      recorder.stop()
    })
  }, [])

  return { start, stop }
}
