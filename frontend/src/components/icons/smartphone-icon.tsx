interface IconProps {
  size?: number
  className?: string
}

export const SmartphoneIcon = ({ size = 32, className = '' }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    <rect x="9" y="3" width="14" height="26" rx="3" stroke="currentColor" strokeWidth="1.8" />
    <path d="M9 7H23" stroke="currentColor" strokeWidth="1.5" />
    <path d="M9 25H23" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="16" cy="27.5" r="1" fill="currentColor" />
  </svg>
)
