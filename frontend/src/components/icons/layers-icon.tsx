interface IconProps {
  size?: number
  className?: string
}

export const LayersIcon = ({ size = 32, className = '' }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    <rect x="4" y="4" width="24" height="10" rx="2" stroke="currentColor" strokeWidth="1.8" />
    <rect x="4" y="18" width="24" height="10" rx="2" stroke="currentColor" strokeWidth="1.8" />
    <path d="M16 14V18" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    <circle cx="8" cy="9" r="1.2" fill="currentColor" />
    <path d="M12 22L10 24L12 26" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M20 22L22 24L20 26" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)
