// Puppeteer 기반 prerender 스크립트.
// dist/ 디렉토리를 static HTTP 서버로 띄운 뒤, 공개 라우트를 실제 브라우저로 열어
// 콘텐츠 + Helmet 메타가 DOM 에 동기화된 시점의 HTML 을 캡처해 저장한다.
//
// 산출물: dist/{route}/index.html (root 는 dist/index.html 덮어쓰기).

import http from 'node:http'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import puppeteer from 'puppeteer'

import { PUBLIC_ROUTES } from './public-routes.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const distDir = path.resolve(__dirname, '..', 'dist')
const PORT = 4173
const CONTENT_READY_MIN_LENGTH = 50
const DEFAULT_TITLE = 'Vite + React'

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
  '.xml': 'application/xml; charset=utf-8',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
}

function createStaticServer() {
  return http.createServer(async (req, res) => {
    try {
      const urlPath = decodeURIComponent(new URL(req.url, `http://localhost:${PORT}`).pathname)
      const hasExtension = path.extname(urlPath) !== ''
      // SPA 폴백: 확장자 없는 경로는 index.html 로 — puppeteer 가 실제 React 라우터를 타도록.
      const relPath = hasExtension ? urlPath : '/index.html'
      const filePath = path.join(distDir, relPath)

      if (!filePath.startsWith(distDir)) {
        res.writeHead(403)
        res.end('Forbidden')
        return
      }

      const data = await fs.readFile(filePath)
      const mime = MIME_TYPES[path.extname(filePath)] ?? 'application/octet-stream'
      res.writeHead(200, { 'Content-Type': mime })
      res.end(data)
    } catch (err) {
      res.writeHead(err.code === 'ENOENT' ? 404 : 500)
      res.end(String(err.message ?? err))
    }
  })
}

async function prerenderRoute(browser, route) {
  const page = await browser.newPage()
  try {
    const url = `http://localhost:${PORT}${route.path}`
    await page.goto(url, { waitUntil: 'networkidle0', timeout: 30_000 })

    await page.waitForFunction(
      (minLength) => {
        const root = document.querySelector('#root')
        return !!root && (root.textContent?.length ?? 0) > minLength
      },
      { timeout: 20_000 },
      CONTENT_READY_MIN_LENGTH,
    )

    await page.waitForFunction(
      (defaultTitle) => document.title && document.title !== defaultTitle,
      { timeout: 10_000 },
      DEFAULT_TITLE,
    )

    const html = await page.evaluate(() => `<!DOCTYPE html>\n${document.documentElement.outerHTML}`)

    const outPath =
      route.path === '/'
        ? path.join(distDir, 'index.html')
        : path.join(distDir, route.path.replace(/^\//, ''), 'index.html')

    await fs.mkdir(path.dirname(outPath), { recursive: true })
    await fs.writeFile(outPath, html, 'utf8')
    console.log(`  ✓ ${route.path} → ${path.relative(distDir, outPath)}`)
  } finally {
    await page.close()
  }
}

async function main() {
  // dist 존재 확인 — vite build 이전이면 즉시 실패.
  await fs.access(distDir).catch(() => {
    console.error(`[prerender] dist not found: ${distDir}. Run \`vite build\` first.`)
    process.exit(1)
  })

  const server = createStaticServer()
  await new Promise((resolve) => server.listen(PORT, resolve))
  console.log(`[prerender] static server on :${PORT}`)

  const executablePath = process.env.PUPPETEER_EXECUTABLE_PATH || undefined
  const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  })
  console.log(`[prerender] chrome launched${executablePath ? ` (${executablePath})` : ''}`)

  try {
    for (const route of PUBLIC_ROUTES) {
      await prerenderRoute(browser, route)
    }
  } finally {
    await browser.close()
    await new Promise((resolve) => server.close(resolve))
  }
  console.log(`[prerender] done (${PUBLIC_ROUTES.length} routes)`)
}

main().catch((err) => {
  console.error('[prerender] failed:', err)
  process.exit(1)
})
