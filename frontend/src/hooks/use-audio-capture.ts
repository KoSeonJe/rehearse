import { useCallback, useRef } from 'react'

export const useAudioCapture = () => {
  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const clonedTracksRef = useRef<MediaStreamTrack[]>([])

  const start = useCallback((stream: MediaStream) => {
    const audioTracks = stream.getAudioTracks()
    if (audioTracks.length === 0) return

    // 이전 세션 정리 (방어적)
    if (recorderRef.current && recorderRef.current.state !== 'inactive') {
      recorderRef.current.stop()
    }
    clonedTracksRef.current.forEach((t) => t.stop())
    clonedTracksRef.current = []

    // 오디오 트랙을 clone하여 MediaRecorder 2개의 스트림 경합 방지
    const clonedTracks = audioTracks.map((t) => t.clone())
    clonedTracksRef.current = clonedTracks
    const audioStream = new MediaStream(clonedTracks)
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
        // 클론된 트랙 정리 (리소스 누수 방지)
        clonedTracksRef.current.forEach((t) => t.stop())
        clonedTracksRef.current = []
        resolve(blob)
      }
      recorder.stop()
    })
  }, [])

  return { start, stop }
}
