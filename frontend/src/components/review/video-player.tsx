import { type RefObject } from 'react'
import { useInterviewStore } from '../../stores/interview-store'

interface VideoPlayerProps {
  videoRef: RefObject<HTMLVideoElement | null>
}

export const VideoPlayer = ({ videoRef }: VideoPlayerProps) => {
  const { videoBlobUrl } = useInterviewStore()

  if (!videoBlobUrl) {
    return (
      <div className="flex aspect-video items-center justify-center rounded-card bg-background">
        <p className="text-sm text-text-secondary">녹화 영상이 없습니다</p>
      </div>
    )
  }

  return (
    <video
      ref={videoRef as React.RefObject<HTMLVideoElement>}
      src={videoBlobUrl}
      controls
      aria-label="면접 녹화 영상"
      className="w-full rounded-card bg-black"
    />
  )
}

