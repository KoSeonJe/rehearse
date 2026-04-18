const path = require('node:path')

// puppeteer 가 다운받는 Chromium 캐시를 프로젝트 안에 고정해 CI 캐시와 .gitignore 관리를 단순화.
// CI 에서는 PUPPETEER_SKIP_DOWNLOAD=true + PUPPETEER_EXECUTABLE_PATH 로 시스템 크롬 사용.
module.exports = {
  cacheDirectory: path.join(__dirname, '.cache', 'puppeteer'),
}
