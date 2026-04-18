interface MockAvatarProps {
  className?: string
}

/**
 * 모노크롬 인터뷰이 실루엣 — 랜딩 FeedbackPreviewMock의 비디오 자리에 사용.
 * DESIGN.md §2 semantic 토큰만 사용 (foreground / muted-foreground).
 */
export const MockAvatar = ({ className = '' }: MockAvatarProps) => (
  <svg
    viewBox="0 0 160 120"
    className={className}
    role="img"
    aria-hidden="true"
  >
    {/* 배경 톤 (warm off-neutral) */}
    <rect width="160" height="120" fill="currentColor" fillOpacity="0.04" />

    {/* 뒷벽 책장 라인 — 홈오피스 힌트 */}
    <g stroke="currentColor" strokeOpacity="0.08" strokeWidth="1">
      <line x1="16" y1="28" x2="48" y2="28" />
      <line x1="16" y1="36" x2="40" y2="36" />
      <line x1="112" y1="24" x2="144" y2="24" />
      <line x1="118" y1="32" x2="144" y2="32" />
    </g>

    {/* 의자 등받이 */}
    <path
      d="M44 120 L44 96 Q80 84 116 96 L116 120 Z"
      fill="currentColor"
      fillOpacity="0.12"
    />

    {/* 어깨/토르소 */}
    <path
      d="M32 120 L32 104 Q42 86 80 84 Q118 86 128 104 L128 120 Z"
      fill="currentColor"
      fillOpacity="0.55"
    />

    {/* 셔츠 옷깃 — 간단한 V 라인 */}
    <path
      d="M72 92 L80 104 L88 92"
      stroke="currentColor"
      strokeOpacity="0.25"
      strokeWidth="1.5"
      fill="none"
      strokeLinecap="round"
    />

    {/* 목 */}
    <rect x="74" y="74" width="12" height="14" fill="currentColor" fillOpacity="0.45" />

    {/* 머리 */}
    <ellipse cx="80" cy="60" rx="18" ry="20" fill="currentColor" fillOpacity="0.65" />

    {/* 머리카락 — 앞머리 */}
    <path
      d="M62 58 Q62 44 80 42 Q98 44 98 58 Q94 50 80 50 Q66 50 62 58 Z"
      fill="currentColor"
      fillOpacity="0.82"
    />
  </svg>
)
