interface IconProps {
  size?: number
  className?: string
}

export const CodeIcon = ({ size = 32, className = '' }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    <rect x="3" y="5" width="26" height="20" rx="2" stroke="currentColor" strokeWidth="1.8" />
    <path d="M10 14L7 17L10 20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M22 14L25 17L22 20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M18 12L14 22" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
)
