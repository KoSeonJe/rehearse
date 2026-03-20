import { useMemo } from 'react'

export type CharacterMood = 'default' | 'happy' | 'thinking' | 'confused' | 'recording'

interface CharacterProps {
  mood?: CharacterMood
  size?: number
  className?: string
}

export const Character = ({ mood = 'default', size = 120, className = '' }: CharacterProps) => {
  const moodStyles = useMemo(() => {
    switch (mood) {
      case 'happy':
        return {
          eyePath: 'M 45 45 Q 50 38 55 45 M 65 45 Q 70 38 75 45',
          mouthPath: 'M 48 58 Q 60 68 72 58',
          color: '#2DD4A8',
          animation: 'animate-bounce-subtle'
        }
      case 'thinking':
        return {
          eyePath: 'M 45 45 L 55 45 M 65 45 L 75 45',
          mouthPath: 'M 52 62 Q 60 62 68 62',
          color: '#1A1A2E',
          animation: 'animate-pulse-subtle'
        }
      case 'confused':
        return {
          eyePath: 'M 45 48 Q 50 42 55 48 M 65 42 Q 70 48 75 42',
          mouthPath: 'M 54 65 Q 60 58 66 65',
          color: '#FF8A6C',
          animation: 'animate-wiggle'
        }
      case 'recording':
        return {
          eyePath: 'M 48 45 A 3 3 0 1 0 54 45 A 3 3 0 1 0 48 45 M 66 45 A 3 3 0 1 0 72 45 A 3 3 0 1 0 66 45',
          mouthPath: 'M 50 60 Q 60 65 70 60',
          color: '#FF6B6B',
          animation: 'animate-pulse'
        }
      default:
        return {
          eyePath: 'M 48 45 A 2 2 0 1 0 52 45 A 2 2 0 1 0 48 45 M 68 45 A 2 2 0 1 0 72 45 A 2 2 0 1 0 68 45',
          mouthPath: 'M 50 60 Q 60 68 70 60',
          color: '#2DD4A8',
          animation: 'animate-float'
        }
    }
  }, [mood])

  return (
    <div
      className={`relative inline-flex items-center justify-center transition-all duration-500 ${className} ${moodStyles.animation}`}
      style={{ width: size, height: size }}
    >
      <div
        className="absolute inset-0 rounded-full blur-3xl opacity-20 transition-colors duration-700"
        style={{ backgroundColor: moodStyles.color }}
      />

      <svg
        viewBox="0 0 120 120"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="relative z-10 w-full h-full"
      >
        <g stroke="#1A1A2E" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="60" cy="45" r="28" fill="white" />
          <path d={moodStyles.eyePath} stroke={moodStyles.color} strokeWidth="3.5" />
          <path d={moodStyles.mouthPath} stroke="#1A1A2E" strokeWidth="3" />
          <path d="M30 90 Q35 75 60 80 Q85 75 90 90" fill="white" />
          <circle cx="85" cy="25" r="4" fill={moodStyles.color} className="opacity-40" />
        </g>
      </svg>
    </div>
  )
}
