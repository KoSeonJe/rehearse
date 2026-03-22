interface AudioWaveformProps {
  isSpeaking: boolean
  size?: number
}

const BAR_COUNT = 5
const HEIGHT_SCALE_STEP = 0.15
const ANIMATION_DELAY_STEP = 0.12
const ANIMATION_BASE_DURATION = 0.8
const ANIMATION_DURATION_INCREMENT = 0.1

export const AudioWaveform = ({ isSpeaking, size = 240 }: AudioWaveformProps) => {
  const barWidth = size * 0.06
  const maxBarHeight = size * 0.5
  const minBarHeight = size * 0.08
  const gap = size * 0.04

  const totalWidth = BAR_COUNT * barWidth + (BAR_COUNT - 1) * gap
  const viewBoxWidth = totalWidth + barWidth
  const viewBoxHeight = maxBarHeight + minBarHeight

  return (
    <div
      className="flex items-center justify-center"
      style={{ width: size, height: size }}
      role="img"
      aria-label={isSpeaking ? 'AI 면접관이 말하는 중' : 'AI 면접관 대기 중'}
    >
      <svg
        width={viewBoxWidth}
        height={viewBoxHeight}
        viewBox={`0 0 ${viewBoxWidth} ${viewBoxHeight}`}
        className="overflow-visible"
      >
        {Array.from({ length: BAR_COUNT }).map((_, i) => {
          const centerIndex = Math.floor(BAR_COUNT / 2)
          const distFromCenter = Math.abs(i - centerIndex)
          const heightScale = 1 - distFromCenter * HEIGHT_SCALE_STEP

          const x = (viewBoxWidth - totalWidth) / 2 + i * (barWidth + gap)
          const barHeight = isSpeaking
            ? maxBarHeight * heightScale
            : minBarHeight

          const animationDelay = i * ANIMATION_DELAY_STEP

          return (
            <rect
              key={i}
              x={x}
              y={(viewBoxHeight - barHeight) / 2}
              width={barWidth}
              height={barHeight}
              rx={barWidth / 2}
              className={isSpeaking ? 'fill-accent' : 'fill-accent/20'}
              style={
                isSpeaking
                  ? {
                      animation: `waveform-bar ${ANIMATION_BASE_DURATION + i * ANIMATION_DURATION_INCREMENT}s ease-in-out ${animationDelay}s infinite alternate`,
                      transformOrigin: `${x + barWidth / 2}px ${viewBoxHeight / 2}px`,
                    }
                  : {
                      animation: `waveform-idle 2s ease-in-out ${animationDelay}s infinite alternate`,
                      transformOrigin: `${x + barWidth / 2}px ${viewBoxHeight / 2}px`,
                    }
              }
            />
          )
        })}
        <style>{`
          @keyframes waveform-bar {
            0% { transform: scaleY(0.4); }
            50% { transform: scaleY(1); }
            100% { transform: scaleY(0.6); }
          }
          @keyframes waveform-idle {
            0% { transform: scaleY(0.8); }
            50% { transform: scaleY(1.2); }
            100% { transform: scaleY(0.8); }
          }
        `}</style>
      </svg>
    </div>
  )
}
