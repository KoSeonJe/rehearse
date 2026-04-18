import { memo, useId } from 'react'

interface InterviewerAvatarProps {
  mood: 'neutral' | 'speaking' | 'listening' | 'thinking'
  size?: number
}

export const InterviewerAvatar = memo(({ mood, size = 180 }: InterviewerAvatarProps) => {
  const clipId = useId()

  return (
    <div
      className="relative flex items-center justify-center"
      style={{ width: size * 1.6, height: size * 1.6 }}
      role="img"
      aria-label={`AI 면접관 - ${mood === 'speaking' ? '말하는 중' : mood === 'listening' ? '듣는 중' : mood === 'thinking' ? '생각하는 중' : '대기 중'}`}
    >
      {/* Speaking — subtle expanding ring, no color gimmick */}
      {mood === 'speaking' && (
        <div
          className="absolute rounded-full border border-foreground/20 opacity-0 animate-[fade-in_1.6s_ease-out_infinite_alternate]"
          style={{ width: size * 1.15, height: size * 1.15 }}
        />
      )}

      {/* Listening — faint warm halo */}
      {mood === 'listening' && (
        <div
          className="absolute rounded-full bg-signal-record/8 transition-opacity duration-[var(--duration-slow)]"
          style={{ width: size * 1.2, height: size * 1.2 }}
        />
      )}

      {/* Border ring — speaking: foreground/40, listening: signal-record/40 */}
      <div
        className={`absolute rounded-full transition-[border-color,opacity] duration-[var(--duration-normal)] ${
          mood === 'speaking' ? 'border-[2px] border-foreground/40'
            : mood === 'listening' ? 'border-[2px] border-signal-record/40'
            : 'border-[2px] border-transparent'
        }`}
        style={{ width: size + 8, height: size + 8 }}
      />

      {/* AI 면접관 캐릭터 — 원형 clip */}
      <svg
        width={size}
        height={size}
        viewBox="0 0 120 120"
        className="relative z-10"
      >
        <defs>
          <clipPath id={clipId}>
            <circle cx="60" cy="60" r="60" />
          </clipPath>
        </defs>
        <g clipPath={`url(#${clipId})`}>
          <rect width="120" height="120" fill="hsl(var(--muted))" />
          <image
            href="/images/interviewer-avatar.png"
            x="0"
            y="0"
            width="120"
            height="120"
            preserveAspectRatio="xMidYMid slice"
          />
        </g>
      </svg>

      {/* Thinking dots */}
      {mood === 'thinking' && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-1.5 z-20">
          {[0, 1, 2].map(i => (
            <div
              key={i}
              className="h-2 w-2 rounded-full bg-blue-400 animate-bounce"
              style={{ animationDelay: `${i * 0.15}s` }}
            />
          ))}
        </div>
      )}
    </div>
  )
})

InterviewerAvatar.displayName = 'InterviewerAvatar'
