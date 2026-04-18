import { memo, useEffect, useRef } from 'react'

interface VideoPreviewProps {
  stream: MediaStream | null
}

export const VideoPreview = memo(({ stream }: VideoPreviewProps) => {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    if (videoRef.current && stream) {
      videoRef.current.srcObject = stream
    }
  }, [stream])

  return (
    <div className="relative h-full w-full overflow-hidden bg-interview-stage">
      <video
        ref={videoRef}
        autoPlay
        muted
        playsInline
        className="h-full w-full -scale-x-100 object-cover"
      />
      {!stream && (
        <div className="absolute inset-0 flex items-center justify-center bg-interview-stage/90">
          <p className="text-xs text-foreground/60">카메라 준비 중...</p>
        </div>
      )}
    </div>
  )
})

VideoPreview.displayName = 'VideoPreview'
