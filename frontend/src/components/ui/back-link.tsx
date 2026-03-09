import { Link } from 'react-router-dom'

interface BackLinkProps {
  to: string
  label?: string
}

export const BackLink = ({ to, label = '뒤로' }: BackLinkProps) => {
  return (
    <Link
      to={to}
      className="inline-flex items-center text-sm text-gray-600 transition-colors duration-150 hover:text-gray-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-500 focus-visible:ring-offset-2"
    >
      <span aria-hidden="true" className="mr-1">
        &larr;
      </span>
      {label}
    </Link>
  )
}
