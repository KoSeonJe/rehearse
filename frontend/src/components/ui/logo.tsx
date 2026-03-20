interface LogoProps {
  size?: number
  className?: string
}

export const Logo = ({ size = 40, className = '' }: LogoProps) => {
  const scale = size / 120

  return (
    <svg
      width={size}
      height={size * 0.75}
      viewBox="0 0 120 90"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      role="img"
      aria-label="리허설 로고"
    >
      <g
        stroke="#1A1A2E"
        strokeWidth={2 / scale > 3 ? 3 : 2}
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {/* 거울 프레임 — 둥근 직사각형 */}
        <rect x="58" y="8" width="54" height="72" rx="6" strokeDasharray="0" />
        {/* 거울 상단 장식 */}
        <line x1="70" y1="4" x2="100" y2="4" />
        {/* 거울 받침대 */}
        <line x1="85" y1="80" x2="85" y2="88" />
        <line x1="76" y1="88" x2="94" y2="88" />

        {/* 거울 속 캐릭터 (자신감 있는 미소) */}
        <circle cx="85" cy="38" r="14" />
        {/* 거울 속 — 눈 (활기찬) */}
        <circle cx="80" cy="35" r="1.5" fill="#1A1A2E" stroke="none" />
        <circle cx="90" cy="35" r="1.5" fill="#1A1A2E" stroke="none" />
        {/* 거울 속 — 자신감 있는 미소 */}
        <path d="M79 42 Q85 48 91 42" />
        {/* 거울 속 — 몸통 */}
        <path d="M75 55 Q85 50 95 55" />

        {/* 거울 밖 캐릭터 (긴장된 표정, 뒷모습 + 옆얼굴) */}
        {/* 머리 */}
        <circle cx="38" cy="40" r="16" />
        {/* 뒷머리 삐침 */}
        <path d="M24 30 Q20 24 26 26" />
        <path d="M22 36 Q17 33 21 30" />
        {/* 옆얼굴 — 눈 (긴장) */}
        <circle cx="44" cy="37" r="1.5" fill="#1A1A2E" stroke="none" />
        {/* 옆얼굴 — 긴장된 입 (물결) */}
        <path d="M43 44 Q45 43 47 44 Q49 45 51 44" />
        {/* 옆얼굴 — 땀방울 */}
        <path d="M26 32 Q24 28 26 26" />

        {/* 몸통 */}
        <path d="M26 58 Q28 52 38 56 Q48 52 50 58" />
        {/* 팔 (어색하게 내린) */}
        <path d="M28 58 Q24 66 22 72" />
        <path d="M48 58 Q52 66 54 72" />
      </g>
    </svg>
  )
}
