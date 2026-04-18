import { forwardRef, useCallback, useImperativeHandle, useRef, useState } from 'react'

export interface VideoPlayerHandle {
  seekTo: (ms: number) => void
  getCurrentTimeMs: () => number
  getDurationMs: () => number
}

interface VideoPlayerProps {
  streamingUrl: string | null
  fallbackUrl: string | null
  onUrlExpired?: () => void
}

export const VideoPlayer = forwardRef<VideoPlayerHandle, VideoPlayerProps>(
  ({ streamingUrl, fallbackUrl, onUrlExpired }, ref) => {
    const videoRef = useRef<HTMLVideoElement>(null)
    const [currentSrc, setCurrentSrc] = useState<string | null>(streamingUrl ?? fallbackUrl)
    const [isPlaying, setIsPlaying] = useState(false)
    const [currentTime, setCurrentTime] = useState(0)
    const [duration, setDuration] = useState(0)
    const [usedFallback, setUsedFallback] = useState(false)

    const [prevUrls, setPrevUrls] = useState({ streamingUrl, fallbackUrl })
    if (streamingUrl !== prevUrls.streamingUrl || fallbackUrl !== prevUrls.fallbackUrl) {
      setPrevUrls({ streamingUrl, fallbackUrl })
      setCurrentSrc(streamingUrl ?? fallbackUrl)
      setUsedFallback(false)
    }

    useImperativeHandle(ref, () => ({
      seekTo: (ms: number) => {
        if (videoRef.current) {
          videoRef.current.currentTime = ms / 1000
        }
      },
      getCurrentTimeMs: () => (videoRef.current ? videoRef.current.currentTime * 1000 : 0),
      getDurationMs: () => (videoRef.current && isFinite(videoRef.current.duration) ? videoRef.current.duration * 1000 : 0),
    }))

    const handleError = useCallback(() => {
      // Presigned URL 만료 (403) 감지 시 재발급 요청
      if (videoRef.current) {
        const mediaError = videoRef.current.error
        if (mediaError && onUrlExpired) {
          onUrlExpired()
          return
        }
      }
      if (!usedFallback && fallbackUrl && currentSrc !== fallbackUrl) {
        setCurrentSrc(fallbackUrl)
        setUsedFallback(true)
      }
    }, [usedFallback, fallbackUrl, currentSrc, onUrlExpired])

    const togglePlay = useCallback(() => {
      if (!videoRef.current) return
      if (videoRef.current.paused) {
        void videoRef.current.play()
      } else {
        videoRef.current.pause()
      }
    }, [])

    const handleSeekBarChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
      if (!videoRef.current) return
      const time = Number(e.target.value)
      videoRef.current.currentTime = time
      setCurrentTime(time)
    }, [])

    const formatTime = (seconds: number): string => {
      const m = Math.floor(seconds / 60)
      const s = Math.floor(seconds % 60)
      return `${m}:${s.toString().padStart(2, '0')}`
    }

    if (!currentSrc) {
      return (
        <div className="aspect-video w-full rounded-2xl bg-surface flex items-center justify-center">
          <p className="text-sm font-bold text-text-tertiary">영상을 불러올 수 없습니다</p>
        </div>
      )
    }

    return (
      <div className="w-full">
        <div className="relative aspect-video w-full overflow-hidden rounded-2xl bg-black">
          <video
            ref={videoRef}
            src={currentSrc}
            preload="auto"
            className="h-full w-full object-contain"
            onError={handleError}
            onPlay={() => setIsPlaying(true)}
            onPause={() => setIsPlaying(false)}
            onTimeUpdate={() => {
              if (videoRef.current) {
                const ct = videoRef.current.currentTime
                setCurrentTime(ct)
                // WebM duration 부정확 시 보정
                if (ct > duration && ct > 0) {
                  setDuration(ct)
                }
              }
            }}
            onLoadedMetadata={() => {
              if (videoRef.current) {
                const d = videoRef.current.duration
                if (d && isFinite(d)) {
                  setDuration(d)
                } else {
                  videoRef.current.currentTime = Number.MAX_SAFE_INTEGER
                }
              }
            }}
            onDurationChange={() => {
              if (videoRef.current) {
                const d = videoRef.current.duration
                if (isFinite(d)) {
                  setDuration(d)
                  if (videoRef.current.currentTime > d) {
                    videoRef.current.currentTime = 0
                  }
                }
              }
            }}
            playsInline
          />
        </div>

        {/* Controls */}
        <div className="mt-3 flex items-center gap-3">
          <button
            onClick={togglePlay}
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-surface text-text-primary transition-[background-color,transform] hover:bg-border active:scale-95"
            aria-label={isPlaying ? '일시정지' : '재생'}
          >
            {isPlaying ? (
              <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor">
                <rect x="2" y="1" width="3.5" height="12" rx="1" />
                <rect x="8.5" y="1" width="3.5" height="12" rx="1" />
              </svg>
            ) : (
              <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor">
                <path d="M3 1.5v11l9-5.5z" />
              </svg>
            )}
          </button>

          <input
            type="range"
            min={0}
            max={duration || 0}
            step={0.1}
            value={currentTime}
            onChange={handleSeekBarChange}
            className="h-1.5 flex-1 cursor-pointer appearance-none rounded-full bg-border accent-accent [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-primary"
          />

          <span className="shrink-0 text-xs font-bold tabular-nums text-text-tertiary">
            {formatTime(currentTime)} / {formatTime(duration)}
          </span>
        </div>
      </div>
    )
  },
)

VideoPlayer.displayName = 'VideoPlayer'
