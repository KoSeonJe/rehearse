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
const CONTENT_READY_MIN_LENGTH = 50
// index.html 폴백이 보여주는 기본 title — 라우트별 Helmet title 이 이 값에서 벗어났는지로 commit 완료 판정.
const FALLBACK_TITLE = '리허설 | AI 모의면접 · 개발자 면접 연습 플랫폼'

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
      const urlPath = decodeURIComponent(new URL(req.url, `http://localhost`).pathname)
      // API 호출은 prerender 중 네트워크 성공 처리 — networkidle 대기가 실 API 응답을 기다리다 timeout 되는 것 방지.
      if (urlPath.startsWith('/api/')) {
        res.writeHead(204, { 'Cache-Control': 'no-store' })
        res.end()
        return
      }
      const hasExtension = path.extname(urlPath) !== ''
      const relPath = hasExtension ? urlPath : '/index.html'
      const filePath = path.join(distDir, relPath)
      const realDist = await fs.realpath(distDir)
      const realTarget = await fs.realpath(filePath).catch(() => filePath)
      if (!realTarget.startsWith(realDist)) {
        res.writeHead(403)
        res.end('Forbidden')
        return
      }

      const data = await fs.readFile(realTarget)
      const mime = MIME_TYPES[path.extname(realTarget)] ?? 'application/octet-stream'
      res.writeHead(200, { 'Content-Type': mime })
      res.end(data)
    } catch (err) {
      res.writeHead(err.code === 'ENOENT' ? 404 : 500)
      res.end(String(err.message ?? err))
    }
  })
}

async function prerenderRoute(browser, port, route) {
  const page = await browser.newPage()
  try {
    // 외부 API/CDN 로 빠지는 네트워크는 즉시 중단 — 정적 dev 서버가 커버하지 않는 origin 호출로 networkidle 이 지연되는 것 방지.
    await page.setRequestInterception(true)
    page.on('request', (req) => {
      const reqUrl = req.url()
      if (reqUrl.startsWith(`http://localhost:${port}`) || reqUrl.startsWith('data:')) {
        req.continue()
      } else {
        req.abort()
      }
    })

    const url = `http://localhost:${port}${route.path}`
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30_000 })

    await page.waitForFunction(
      (minLength) => {
        const root = document.querySelector('#root')
        return !!root && (root.textContent?.length ?? 0) > minLength
      },
      { timeout: 20_000 },
      CONTENT_READY_MIN_LENGTH,
    )

    // Helmet 이 head 를 commit 했는지 — title 이 폴백에서 벗어났고 Helmet 이 주입한 canonical
    // (data-rh="true") 이 존재할 때만 스냅샷. 베이스 템플릿의 canonical 과 혼동 방지.
    await page.waitForFunction(
      (fallback) => {
        const title = document.title
        const canonical = document.head.querySelector('link[rel="canonical"][data-rh="true"]')
        return !!title && title !== fallback && !!canonical
      },
      { timeout: 15_000 },
      FALLBACK_TITLE,
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

async function resolveExecutablePath() {
  const envPath = process.env.PUPPETEER_EXECUTABLE_PATH
  if (!envPath) return undefined
  try {
    await fs.access(envPath)
    return envPath
  } catch {
    // GHA 이미지 업데이트로 google-chrome 경로가 이동한 경우 폴백 후보 탐색.
    const fallbacks = ['/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser']
    for (const candidate of fallbacks) {
      try {
        await fs.access(candidate)
        console.warn(`[prerender] PUPPETEER_EXECUTABLE_PATH '${envPath}' not found, falling back to ${candidate}`)
        return candidate
      } catch {
        // keep trying
      }
    }
    throw new Error(
      `PUPPETEER_EXECUTABLE_PATH '${envPath}' not found and no fallback Chrome binary located. ` +
        `Install google-chrome or unset PUPPETEER_SKIP_DOWNLOAD to use puppeteer's bundled Chromium.`,
    )
  }
}

async function main() {
  await fs.access(distDir).catch(() => {
    console.error(`[prerender] dist not found: ${distDir}. Run \`vite build\` first.`)
    process.exit(1)
  })

  const server = createStaticServer()
  await new Promise((resolve) => server.listen(0, resolve))
  const port = server.address().port
  console.log(`[prerender] static server on :${port}`)

  const executablePath = await resolveExecutablePath()
  const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  })
  console.log(`[prerender] chrome launched${executablePath ? ` (${executablePath})` : ' (bundled)'}`)

  try {
    for (const route of PUBLIC_ROUTES) {
      await prerenderRoute(browser, port, route)
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
