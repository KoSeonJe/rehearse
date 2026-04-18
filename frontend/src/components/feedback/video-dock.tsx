import { type Ref } from 'react'
import { cn } from '@/lib/utils'
import { StickyRail } from '@/components/layout/sticky-rail'
import { VideoPlayer, type VideoPlayerHandle } from '@/components/feedback/video-player'
import { TimelineBar } from '@/components/feedback/timeline-bar'
import type { TimestampFeedback } from '@/types/interview'

interface VideoDockProps {
  streamingUrl: string | null
  fallbackUrl: string | null
  feedbacks: TimestampFeedback[]
  durationMs: number
  currentTimeMs: number
  activeFeedbackId: number | null
  onSeek: (ms: number) => void
  onUrlExpired?: () => void
  videoRef?: Ref<VideoPlayerHandle>
  state?: 'loading' | 'empty' | 'error'
  className?: string
}

/**
 * Sticky video + timeline dock (spec §5.4-B).
 * Composes StickyRail + VideoPlayer + TimelineBar.
 * Handles 3 null states: loading, empty, error.
 */
export const VideoDock = ({
  streamingUrl,
  fallbackUrl,
  feedbacks,
  durationMs,
  currentTimeMs,
  activeFeedbackId,
  onSeek,
  onUrlExpired,
  videoRef,
  state,
  className,
}: VideoDockProps) => {
  return (
    <StickyRail
      col="col-span-4"
      offset="top-[var(--utility-bar-height)]"
      className={cn('space-y-4 py-4', className)}
    >
      {state === 'loading' && (
        <div className="aspect-video w-full rounded-sm bg-muted animate-pulse flex items-center justify-center">
          <p className="text-xs font-semibold text-muted-foreground">영상 로딩 중</p>
        </div>
      )}

      {state === 'empty' && (
        <div className="aspect-video w-full rounded-sm border border-foreground/10 flex items-center justify-center">
          <p className="text-xs font-semibold text-muted-foreground">영상이 없습니다</p>
        </div>
      )}

      {state === 'error' && (
        <div className="aspect-video w-full rounded-sm border border-signal-error/20 bg-signal-error/5 flex items-center justify-center">
          <p className="text-xs font-semibold text-signal-error">영상을 불러오지 못했습니다</p>
        </div>
      )}

      {!state && (
        <VideoPlayer
          ref={videoRef}
          streamingUrl={streamingUrl}
          fallbackUrl={fallbackUrl}
          onUrlExpired={onUrlExpired}
        />
      )}

      <TimelineBar
        feedbacks={feedbacks}
        durationMs={durationMs}
        currentTimeMs={currentTimeMs}
        activeFeedbackId={activeFeedbackId}
        onSeek={onSeek}
      />
    </StickyRail>
  )
}
