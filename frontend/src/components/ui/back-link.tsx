import { Link } from 'react-router-dom'

interface BackLinkProps {
  to: string
  label?: string
  replace?: boolean
}

export const BackLink = ({ to, label = '뒤로', replace = false }: BackLinkProps) => {
  return (
    <Link
      to={to}
      replace={replace}
      className="inline-flex items-center text-sm text-text-secondary transition-colors duration-150 hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-legacy focus-visible:ring-offset-2"
    >
      <span aria-hidden="true" className="mr-1">
        &larr;
      </span>
      {label}
    </Link>
  )
}
