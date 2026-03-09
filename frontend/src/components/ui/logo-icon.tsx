interface LogoIconProps {
  size?: number
  className?: string
}

export const LogoIcon = ({ size = 24, className = '' }: LogoIconProps) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      role="img"
      aria-label="리허설 아이콘"
    >
      <g
        stroke="#1e293b"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {/* 얼굴 */}
        <circle cx="16" cy="15" r="11" />
        {/* 삐침 머리카락 */}
        <path d="M10 6 Q8 3 11 4" />
        <path d="M14 5 Q13 1 16 3" />
        {/* 눈 — 활기찬 */}
        <circle cx="12" cy="13" r="1.4" fill="#1e293b" stroke="none" />
        <circle cx="20" cy="13" r="1.4" fill="#1e293b" stroke="none" />
        {/* 눈 하이라이트 */}
        <circle cx="12.8" cy="12.2" r="0.5" fill="white" stroke="none" />
        <circle cx="20.8" cy="12.2" r="0.5" fill="white" stroke="none" />
        {/* 자신감 있는 미소 */}
        <path d="M11 18 Q16 23 21 18" />
        {/* 볼 터치 */}
        <line x1="7.5" y1="16" x2="9" y2="16" opacity="0.4" />
        <line x1="23" y1="16" x2="24.5" y2="16" opacity="0.4" />
      </g>
    </svg>
  )
}
