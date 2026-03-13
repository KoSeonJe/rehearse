import { useEffect, useState, type RefObject } from 'react'
import { useInterviewStore } from '@/stores/interview-store'
import { loadVideoBlob } from '@/lib/video-storage'

interface VideoPlayerProps {
  videoRef: RefObject<HTMLVideoElement | null>
  interviewId?: string
}

export const VideoPlayer = ({ videoRef, interviewId }: VideoPlayerProps) => {
  const storeBlobUrl = useInterviewStore((s) => s.videoBlobUrl)
  const [indexedDbUrl, setIndexedDbUrl] = useState<string | null>(null)

  useEffect(() => {
    if (storeBlobUrl || !interviewId) return

    let url: string | null = null
    let cancelled = false

    loadVideoBlob(interviewId).then((blob) => {
      if (blob && !cancelled) {
        url = URL.createObjectURL(blob)
        setIndexedDbUrl(url)
      }
    })

    return () => {
      cancelled = true
      if (url) {
        URL.revokeObjectURL(url)
      }
    }
  }, [storeBlobUrl, interviewId])

  const videoUrl = storeBlobUrl ?? indexedDbUrl

  if (!videoUrl) {
    return (
      <div className="flex aspect-video items-center justify-center rounded-card bg-background">
        <p className="text-sm text-text-secondary">녹화 영상이 없습니다</p>
      </div>
    )
  }

  return (
    <video
      ref={videoRef as React.RefObject<HTMLVideoElement>}
      src={videoUrl}
      controls
      aria-label="면접 녹화 영상"
      className="w-full rounded-card bg-black"
    />
  )
}
