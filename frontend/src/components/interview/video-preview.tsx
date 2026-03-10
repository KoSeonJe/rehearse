import { useEffect, useRef } from 'react'

interface VideoPreviewProps {
  stream: MediaStream | null
  isRecording: boolean
}

export const VideoPreview = ({ stream, isRecording }: VideoPreviewProps) => {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    if (videoRef.current && stream) {
      videoRef.current.srcObject = stream
    }
  }, [stream])

  return (
    <div className="relative overflow-hidden rounded-card bg-text-primary">
      <video
        ref={videoRef}
        autoPlay
        muted
        playsInline
        className="aspect-video w-full -scale-x-100 object-cover"
      />
      {isRecording && (
        <div className="absolute top-4 right-4 flex items-center gap-2 rounded-badge bg-error/90 px-3 py-1.5 text-xs font-medium text-white">
          <span className="h-2 w-2 animate-pulse rounded-full bg-white" />
          REC
        </div>
      )}
      {!stream && (
        <div className="absolute inset-0 flex items-center justify-center bg-text-primary/90">
          <p className="text-sm text-text-tertiary">카메라를 준비하고 있습니다...</p>
        </div>
      )}
    </div>
  )
}

