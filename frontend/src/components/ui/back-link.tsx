import { Link } from 'react-router-dom'

interface BackLinkProps {
  to: string
  label?: string
}

export const BackLink = ({ to, label = '뒤로' }: BackLinkProps) => {
  return (
    <Link
      to={to}
      className="inline-flex items-center text-sm text-text-secondary transition-colors duration-150 hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2"
    >
      <span aria-hidden="true" className="mr-1">
        &larr;
      </span>
      {label}
    </Link>
  )
}
