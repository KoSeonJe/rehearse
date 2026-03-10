interface IconProps {
  size?: number
  className?: string
}

export const ServerIcon = ({ size = 32, className = '' }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    <rect x="4" y="6" width="24" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.8" />
    <rect x="4" y="14" width="24" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.8" />
    <rect x="4" y="22" width="24" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.8" />
    <circle cx="8" cy="9" r="1.2" fill="currentColor" />
    <circle cx="8" cy="17" r="1.2" fill="currentColor" />
    <circle cx="8" cy="25" r="1.2" fill="currentColor" />
  </svg>
)
