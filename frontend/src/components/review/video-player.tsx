import { type RefObject } from 'react'
import { useInterviewStore } from '../../stores/interview-store'

interface VideoPlayerProps {
  videoRef: RefObject<HTMLVideoElement | null>
}

const VideoPlayer = ({ videoRef }: VideoPlayerProps) => {
  const { videoBlobUrl } = useInterviewStore()

  if (!videoBlobUrl) {
    return (
      <div className="flex aspect-video items-center justify-center rounded-xl bg-slate-100">
        <p className="text-sm text-slate-500">녹화 영상이 없습니다</p>
      </div>
    )
  }

  return (
    <video
      ref={videoRef}
      src={videoBlobUrl}
      controls
      className="w-full rounded-xl bg-black"
    />
  )
}

export default VideoPlayer
