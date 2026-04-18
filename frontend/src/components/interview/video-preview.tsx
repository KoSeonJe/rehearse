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
        <div className="dark absolute inset-0 flex items-center justify-center bg-interview-stage">
          <div className="flex items-center gap-2">
            <div
              className="h-3 w-3 animate-spin rounded-full border border-foreground/40 border-t-transparent"
              aria-hidden="true"
            />
            <p className="text-xs font-medium text-foreground/80">카메라 준비 중</p>
          </div>
        </div>
      )}
    </div>
  )
})

VideoPreview.displayName = 'VideoPreview'
