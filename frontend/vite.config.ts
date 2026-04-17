import { defineConfig, loadEnv, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import fs from 'fs'

interface SitemapRoute {
  path: string
  priority: number
  changefreq: 'always' | 'hourly' | 'daily' | 'weekly' | 'monthly' | 'yearly' | 'never'
}

const PUBLIC_ROUTES: SitemapRoute[] = [
  { path: '/', priority: 1.0, changefreq: 'weekly' },
  { path: '/about', priority: 0.7, changefreq: 'monthly' },
  { path: '/faq', priority: 0.7, changefreq: 'monthly' },
  { path: '/guide/ai-mock-interview', priority: 0.8, changefreq: 'monthly' },
  { path: '/guide/developer-interview-prep', priority: 0.8, changefreq: 'monthly' },
  { path: '/guide/resume-based-interview', priority: 0.8, changefreq: 'monthly' },
  { path: '/privacy', priority: 0.3, changefreq: 'yearly' },
]

function seoPlugin(siteUrl: string, isProd: boolean): Plugin {
  return {
    name: 'rehearse-seo',
    apply: 'build',
    transformIndexHtml(html) {
      if (isProd) return html

      const stripped = html
        .replace(/\s*<!-- Google Search Console[^>]*-->\s*/g, '')
        .replace(/\s*<!-- Naver Search Advisor[^>]*-->\s*/g, '')
        .replace(/\s*<meta name="google-site-verification"[^>]*\/?>\s*/g, '')
        .replace(/\s*<meta name="naver-site-verification"[^>]*\/?>\s*/g, '')

      return stripped.replace(
        /<meta name="viewport"[^>]*\/?>/,
        (match) => `${match}\n    <meta name="robots" content="noindex, nofollow" />`,
      )
    },
    closeBundle() {
      const lastmod = new Date().toISOString().split('T')[0]
      const urlEntries = PUBLIC_ROUTES.map(
        (r) => `  <url>
    <loc>${siteUrl}${r.path}</loc>
    <lastmod>${lastmod}</lastmod>
    <changefreq>${r.changefreq}</changefreq>
    <priority>${r.priority.toFixed(1)}</priority>
  </url>`,
      ).join('\n')

      const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urlEntries}
</urlset>
`
      const distDir = path.resolve(__dirname, 'dist')
      fs.writeFileSync(path.join(distDir, 'sitemap.xml'), sitemap)

      const robotsBody = isProd
        ? `User-agent: *
Allow: /
Disallow: /dashboard
Disallow: /interview/

Sitemap: ${siteUrl}/sitemap.xml
`
        : `User-agent: *
Disallow: /

Sitemap: ${siteUrl}/sitemap.xml
`
      fs.writeFileSync(path.join(distDir, 'robots.txt'), robotsBody)
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const siteUrl = env.VITE_SITE_URL || 'https://rehearse.co.kr'
  const isProd = mode === 'production'

  return {
    plugins: [react(), seoPlugin(siteUrl, isProd)],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
