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
      {/* Ripple waves - speaking animation */}
      {mood === 'speaking' && (
        <>
          <div
            className="absolute rounded-full border-2 border-meet-green/30 animate-[ripple_2s_ease-out_infinite]"
            style={{ width: size, height: size }}
          />
          <div
            className="absolute rounded-full border-2 border-meet-green/20 animate-[ripple_2s_ease-out_0.6s_infinite]"
            style={{ width: size, height: size }}
          />
          <div
            className="absolute rounded-full border-2 border-meet-green/10 animate-[ripple_2s_ease-out_1.2s_infinite]"
            style={{ width: size, height: size }}
          />
        </>
      )}

      {/* Listening — 부드러운 빨간 glow */}
      {mood === 'listening' && (
        <div
          className="absolute rounded-full bg-meet-red/10 animate-pulse"
          style={{ width: size * 1.3, height: size * 1.3 }}
        />
      )}

      {/* Border ring — speaking: 초록, listening: 빨강 */}
      <div
        className={`absolute rounded-full transition-all duration-300 ${
          mood === 'speaking' ? 'border-[3px] border-meet-green'
            : mood === 'listening' ? 'border-[3px] border-meet-red/60'
            : 'border-[3px] border-transparent'
        }`}
        style={{ width: size + 8, height: size + 8 }}
      />

      {/* Google-style default profile circle */}
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
          <circle cx="60" cy="60" r="60" fill="#5F6368" />
          <circle cx="60" cy="44" r="16" fill="#E8EAED" />
          <path
            d="M60 64 C40 64 26 78 26 94 L26 120 L94 120 L94 94 C94 78 80 64 60 64Z"
            fill="#E8EAED"
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
