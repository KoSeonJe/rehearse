interface IconProps {
  size?: number
  className?: string
}

export const NetworkIcon = ({ size = 32, className = '' }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    <circle cx="16" cy="16" r="4" stroke="currentColor" strokeWidth="1.8" />
    <circle cx="8" cy="8" r="2.5" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="24" cy="8" r="2.5" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="8" cy="24" r="2.5" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="24" cy="24" r="2.5" stroke="currentColor" strokeWidth="1.5" />
    <path d="M10 10L13 13" stroke="currentColor" strokeWidth="1.5" />
    <path d="M22 10L19 13" stroke="currentColor" strokeWidth="1.5" />
    <path d="M10 22L13 19" stroke="currentColor" strokeWidth="1.5" />
    <path d="M22 22L19 19" stroke="currentColor" strokeWidth="1.5" />
  </svg>
)
