import { useCallback, useEffect, useRef, useState } from 'react'

interface UseMediaStreamOptions {
  video?: boolean | MediaTrackConstraints
  audio?: boolean | MediaTrackConstraints
}

interface UseMediaStreamReturn {
  stream: MediaStream | null
  isActive: boolean
  error: string | null
  start: () => Promise<void>
  stop: () => void
}

const DEFAULT_VIDEO_CONSTRAINTS: MediaTrackConstraints = {
  width: { ideal: 1280 },
  height: { ideal: 720 },
  facingMode: 'user',
}

const DEFAULT_AUDIO_CONSTRAINTS: MediaTrackConstraints = {
  echoCancellation: true,
  noiseSuppression: true,
}

export const useMediaStream = (options?: UseMediaStreamOptions): UseMediaStreamReturn => {
  const [stream, setStream] = useState<MediaStream | null>(null)
  const [isActive, setIsActive] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const streamRef = useRef<MediaStream | null>(null)

  const stop = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }
    setStream(null)
    setIsActive(false)
  }, [])

  const start = useCallback(async () => {
    try {
      setError(null)
      const constraints: MediaStreamConstraints = {
        video: options?.video ?? DEFAULT_VIDEO_CONSTRAINTS,
        audio: options?.audio ?? DEFAULT_AUDIO_CONSTRAINTS,
      }

      const mediaStream = await navigator.mediaDevices.getUserMedia(constraints)
      streamRef.current = mediaStream
      setStream(mediaStream)
      setIsActive(true)
    } catch (err) {
      const message =
        err instanceof DOMException
          ? err.name === 'NotAllowedError'
            ? '카메라/마이크 권한이 거부되었습니다. 브라우저 설정에서 권한을 허용해주세요.'
            : err.name === 'NotFoundError'
              ? '카메라 또는 마이크를 찾을 수 없습니다.'
              : `미디어 접근 오류: ${err.message}`
          : '알 수 없는 오류가 발생했습니다.'
      setError(message)
    }
  }, [options?.video, options?.audio])

  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop())
      }
    }
  }, [])

  return { stream, isActive, error, start, stop }
}

