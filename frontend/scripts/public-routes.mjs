// 공개 라우트 단일 출처 (vite.config.ts 의 sitemap 생성과 prerender 스크립트가 함께 import).
// 비공개 라우트(dashboard, interview/*)는 포함하지 않는다 — 화이트리스트 원칙.

export const PUBLIC_ROUTES = [
  { path: '/', priority: 1.0, changefreq: 'weekly' },
  { path: '/about', priority: 0.7, changefreq: 'monthly' },
  { path: '/faq', priority: 0.7, changefreq: 'monthly' },
  { path: '/guide/ai-mock-interview', priority: 0.8, changefreq: 'monthly' },
  { path: '/guide/developer-interview-prep', priority: 0.8, changefreq: 'monthly' },
  { path: '/guide/resume-based-interview', priority: 0.8, changefreq: 'monthly' },
  { path: '/privacy', priority: 0.3, changefreq: 'yearly' },
]
