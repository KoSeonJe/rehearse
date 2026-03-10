interface AudioLevelIndicatorProps {
  level: number
}

export const AudioLevelIndicator = ({ level }: AudioLevelIndicatorProps) => {
  const bars = 12
  const activeBars = Math.round(level * bars)

  return (
    <div className="flex items-center gap-1.5" role="meter" aria-label="마이크 레벨" aria-valuenow={Math.round(level * 100)} aria-valuemin={0} aria-valuemax={100}>
      <svg
        className="h-4 w-4 text-text-tertiary"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2}
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M12 18.75a6 6 0 006-6v-1.5m-6 7.5a6 6 0 01-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 01-3-3V4.5a3 3 0 116 0v8.25a3 3 0 01-3 3z"
        />
      </svg>
      <div className="flex items-end gap-0.5">
        {Array.from({ length: bars }).map((_, i) => (
          <div
            key={i}
            className={`w-1 rounded-full transition-all duration-75 ${
              i < activeBars ? 'bg-success' : 'bg-border'
            }`}
            style={{ height: `${8 + i * 1.5}px` }}
          />
        ))}
      </div>
    </div>
  )
}

