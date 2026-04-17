import { Link } from 'react-router-dom'
import { Helmet } from 'react-helmet-async'
import { Logo } from '@/components/ui/logo'
import { BetaBadge } from '@/components/ui/beta-badge'

interface ContentPageShellProps {
  title: string
  description: string
  canonicalPath: string
  jsonLd?: object
  children: React.ReactNode
}

export const ContentPageShell = ({
  title,
  description,
  canonicalPath,
  jsonLd,
  children,
}: ContentPageShellProps) => {
  const siteUrl = import.meta.env.VITE_SITE_URL || 'https://rehearse.co.kr'
  const canonical = `${siteUrl}${canonicalPath}`

  return (
    <div className="min-h-screen bg-background text-text-primary">
      <Helmet>
        <title>{title}</title>
        <meta name="description" content={description} />
        <link rel="canonical" href={canonical} />
        <meta property="og:title" content={title} />
        <meta property="og:description" content={description} />
        <meta property="og:url" content={canonical} />
        <meta property="og:type" content="article" />
        {jsonLd && <script type="application/ld+json">{JSON.stringify(jsonLd)}</script>}
      </Helmet>

      <header className="sticky top-0 z-50 bg-background/80 backdrop-blur-md border-b border-border/50">
        <div className="mx-auto flex h-16 max-w-3xl items-center justify-between px-5 md:px-8">
          <Link
            to="/"
            className="flex items-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-legacy focus-visible:ring-offset-2 rounded-sm"
          >
            <Logo size={80} />
            <span className="text-xl font-extrabold tracking-tight text-text-primary">리허설</span>
            <BetaBadge size="md" />
          </Link>
          <Link
            to="/"
            className="text-sm font-medium text-text-secondary transition-colors hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-legacy focus-visible:ring-offset-2 rounded-sm"
          >
            ← 홈으로
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-5 md:px-8 py-12">{children}</main>
    </div>
  )
}
