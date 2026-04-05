import { memo, useEffect, useRef } from 'react'

interface InterviewerVideoProps {
  mood: 'neutral' | 'speaking' | 'listening' | 'thinking'
}

const VIDEO_URLS: Record<InterviewerVideoProps['mood'], string> = {
  speaking: 'https://dev.rehearse.co.kr/assets/interviewer/questioning.mp4',
  listening: 'https://dev.rehearse.co.kr/assets/interviewer/listening.mp4',
  thinking: 'https://dev.rehearse.co.kr/assets/interviewer/thinking.mp4',
  neutral: 'https://dev.rehearse.co.kr/assets/interviewer/listening.mp4',
}

export const InterviewerVideo = memo(({ mood }: InterviewerVideoProps) => {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return
    video.load()
    video.play().catch(() => {})
  }, [mood])

  return (
    <div className="relative flex items-center justify-center w-full h-full">
      <video
        ref={videoRef}
        key={mood}
        src={VIDEO_URLS[mood]}
        loop
        autoPlay
        muted
        playsInline
        className="w-full h-full object-cover"
      />
    </div>
  )
})

InterviewerVideo.displayName = 'InterviewerVideo'