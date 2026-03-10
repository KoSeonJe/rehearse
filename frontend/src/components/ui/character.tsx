type CharacterMood = 'default' | 'thinking' | 'confused' | 'happy' | 'nervous'

interface CharacterProps {
  mood?: CharacterMood
  size?: number
  className?: string
}

const Eyes = ({ mood }: { mood: CharacterMood }) => {
  switch (mood) {
    case 'thinking':
      return (
        <>
          {/* 위를 바라보는 눈 */}
          <circle cx="37" cy="38" r="2.5" fill="#1e293b" stroke="none" />
          <circle cx="63" cy="38" r="2.5" fill="#1e293b" stroke="none" />
          <circle cx="38" cy="37" r="0.8" fill="white" stroke="none" />
          <circle cx="64" cy="37" r="0.8" fill="white" stroke="none" />
        </>
      )
    case 'confused':
      return (
        <>
          {/* 당황 — X 눈과 동그라미 눈 */}
          <line x1="34" y1="38" x2="40" y2="44" strokeWidth="2.5" />
          <line x1="40" y1="38" x2="34" y2="44" strokeWidth="2.5" />
          <circle cx="63" cy="41" r="4" strokeWidth="2" />
        </>
      )
    case 'happy':
      return (
        <>
          {/* 초승달 눈 (눈웃음) */}
          <path d="M33 40 Q37 36 41 40" strokeWidth="2.5" fill="none" />
          <path d="M59 40 Q63 36 67 40" strokeWidth="2.5" fill="none" />
        </>
      )
    case 'nervous':
      return (
        <>
          {/* 불안한 눈 — 약간 찌푸린 */}
          <circle cx="37" cy="41" r="2.5" fill="#1e293b" stroke="none" />
          <circle cx="63" cy="41" r="2.5" fill="#1e293b" stroke="none" />
          <circle cx="38" cy="40" r="0.8" fill="white" stroke="none" />
          <circle cx="64" cy="40" r="0.8" fill="white" stroke="none" />
          {/* 걱정 눈썹 */}
          <path d="M32 34 Q37 31 42 34" strokeWidth="2" fill="none" />
          <path d="M58 34 Q63 31 68 34" strokeWidth="2" fill="none" />
        </>
      )
    default:
      return (
        <>
          {/* 기본 눈 */}
          <circle cx="37" cy="41" r="2.5" fill="#1e293b" stroke="none" />
          <circle cx="63" cy="41" r="2.5" fill="#1e293b" stroke="none" />
          <circle cx="38" cy="40" r="0.8" fill="white" stroke="none" />
          <circle cx="64" cy="40" r="0.8" fill="white" stroke="none" />
        </>
      )
  }
}

const Mouth = ({ mood }: { mood: CharacterMood }) => {
  switch (mood) {
    case 'thinking':
      return (
        <>
          {/* 옆으로 삐죽 — 생각 중 */}
          <path d="M44 56 Q50 54 56 56 Q58 57 60 56" fill="none" />
        </>
      )
    case 'confused':
      return (
        <>
          {/* 물결 입 — 당황 */}
          <path d="M40 58 Q44 55 48 58 Q52 61 56 58 Q60 55 64 58" fill="none" />
        </>
      )
    case 'happy':
      return (
        <>
          {/* 큰 미소 */}
          <path d="M38 54 Q50 66 62 54" fill="none" />
        </>
      )
    case 'nervous':
      return (
        <>
          {/* 지그재그 긴장 입 */}
          <path d="M40 57 L44 55 L48 57 L52 55 L56 57 L60 55" fill="none" />
        </>
      )
    default:
      return (
        <>
          {/* 살짝 미소 */}
          <path d="M42 55 Q50 60 58 55" fill="none" />
        </>
      )
  }
}

const Extras = ({ mood }: { mood: CharacterMood }) => {
  switch (mood) {
    case 'thinking':
      return (
        <>
          {/* 생각 말풍선 점 */}
          <circle cx="76" cy="22" r="2" fill="#1e293b" stroke="none" opacity="0.5" />
          <circle cx="82" cy="14" r="3" fill="#1e293b" stroke="none" opacity="0.4" />
          <circle cx="90" cy="8" r="4" fill="#1e293b" stroke="none" opacity="0.3" />
        </>
      )
    case 'confused':
      return (
        <>
          {/* 물음표 */}
          <text
            x="74"
            y="22"
            fontSize="18"
            fontWeight="bold"
            fill="#1e293b"
            opacity="0.6"
            fontFamily="sans-serif"
          >
            ?
          </text>
          {/* 땀방울 */}
          <path d="M22 30 Q19 24 22 20" opacity="0.5" />
          <circle cx="22" cy="30" r="1.5" fill="#1e293b" stroke="none" opacity="0.3" />
        </>
      )
    case 'happy':
      return (
        <>
          {/* 반짝 이펙트 */}
          <line x1="18" y1="20" x2="14" y2="16" opacity="0.4" />
          <line x1="16" y1="24" x2="11" y2="24" opacity="0.4" />
          <line x1="82" y1="20" x2="86" y2="16" opacity="0.4" />
          <line x1="84" y1="24" x2="89" y2="24" opacity="0.4" />
          {/* 볼 터치 */}
          <line x1="24" y1="50" x2="28" y2="50" opacity="0.35" strokeWidth="2.5" />
          <line x1="72" y1="50" x2="76" y2="50" opacity="0.35" strokeWidth="2.5" />
        </>
      )
    case 'nervous':
      return (
        <>
          {/* 양쪽 땀방울 */}
          <path d="M20 32 Q17 26 20 22" opacity="0.5" />
          <circle cx="20" cy="32" r="1.5" fill="#1e293b" stroke="none" opacity="0.3" />
          <path d="M80 32 Q83 26 80 22" opacity="0.5" />
          <circle cx="80" cy="32" r="1.5" fill="#1e293b" stroke="none" opacity="0.3" />
        </>
      )
    default:
      return null
  }
}

export const Character = ({
  mood = 'default',
  size = 120,
  className = '',
}: CharacterProps) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      role="img"
      aria-label={`캐릭터 - ${mood} 표정`}
    >
      <g
        stroke="#1e293b"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {/* 얼굴 */}
        <circle cx="50" cy="46" r="30" />

        {/* 삐침 머리카락 */}
        <path d="M34 18 Q30 12 36 14" />
        <path d="M42 16 Q40 9 46 12" />
        <path d="M52 15 Q53 8 58 12" />

        {/* 눈 */}
        <Eyes mood={mood} />

        {/* 입 */}
        <Mouth mood={mood} />

        {/* 볼 터치 (기본/thinking만) */}
        {(mood === 'default' || mood === 'thinking') && (
          <>
            <line x1="24" y1="48" x2="28" y2="48" opacity="0.3" />
            <line x1="72" y1="48" x2="76" y2="48" opacity="0.3" />
          </>
        )}

        {/* 몸통 */}
        <path d="M30 78 Q34 72 50 76 Q66 72 70 78" />

        {/* 무드별 추가 요소 */}
        <Extras mood={mood} />
      </g>
    </svg>
  )
}
