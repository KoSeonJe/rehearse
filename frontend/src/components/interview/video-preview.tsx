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
        <div className="absolute inset-0 flex flex-col items-center justify-end bg-white">
          {/* 스트림 대기 중일 때 면접자 캐릭터 일러스트로 영역 채움.
              video 요소 아래 배치되므로 stream 활성화 시 자동으로 가려진다. */}
          <img
            src="/images/interviewee-placeholder.png"
            alt=""
            className="absolute inset-0 h-full w-full object-cover"
            aria-hidden="true"
          />
          <p className="relative z-10 mb-1.5 text-[10px] text-foreground/70 bg-background/70 px-2 py-0.5 rounded">
            카메라 준비 중...
          </p>
        </div>
      )}
    </div>
  )
})

VideoPreview.displayName = 'VideoPreview'
