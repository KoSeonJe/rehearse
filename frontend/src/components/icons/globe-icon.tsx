interface IconProps {
  size?: number
  className?: string
}

export const GlobeIcon = ({ size = 32, className = '' }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    <circle cx="16" cy="16" r="11" stroke="currentColor" strokeWidth="1.8" />
    <path d="M16 5C20 9 22 12.5 22 16C22 19.5 20 23 16 27" stroke="currentColor" strokeWidth="1.8" />
    <path d="M16 5C12 9 10 12.5 10 16C10 19.5 12 23 16 27" stroke="currentColor" strokeWidth="1.8" />
    <path d="M5 16H27" stroke="currentColor" strokeWidth="1.8" />
  </svg>
)
