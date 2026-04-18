# Release Rules
<!-- last-analyzed: 2026-04-19T00:50:00Z -->

## Version Sources

- `frontend/package.json` — `"version"` field (현재 `0.1.0`, 동기화하지 않음)
- `backend/build.gradle.kts` — `version = "0.1.0"` (동기화하지 않음)
- **Git tags** (`vX.Y.Z`, annotated) — **유일한 진실 공급원**. package.json/build.gradle 의 `version` 은 부트스트랩 이후 수정되지 않음 (v0.1.0 ~ v0.1.2 모두 0.1.0).
- GitHub Releases — `gh release` 로 태그에 부착

## Release Trigger

- **`push: main`** → `.github/workflows/deploy-prod.yml` 자동 트리거
- 태그 푸시는 배포를 트리거하지 않음 (메타데이터/히스토리 용도)
- `workflow_dispatch` 도 가능 (수동)
- 흐름: `develop → main` PR → 머지 → main 푸시로 자동 배포 → 배포 성공 후 별도로 `git tag v0.1.X` + `gh release create`

## Test Gate

- **Backend**: `./gradlew test --no-daemon` (job: `backend-test`, `SPRING_PROFILES_ACTIVE=test`)
- **Frontend**: `npm run lint` + `npm run build` (job: `frontend-build`)
- **paths-filter**: `backend/**` 또는 `frontend/**` 변경 시에만 해당 job 실행
- **environment: production** gate — `deploy` job 시작 전 reviewer 승인 필수
- 헬스체크: `https://api.rehearse.co.kr/actuator/health` 6회 재시도

## Registry / Distribution

- **Frontend**: S3 (`rehearse-frontend-prod`) → CloudFront (`E2UWW3KP4S5VOV`) → `https://rehearse.co.kr`
  - asset 1년 캐시 immutable / `index.html` no-cache (모든 prerender 하위 경로 포함)
  - CloudFront Function `rehearse-spa-rewrite` (viewer-request) — `/about` → `/about/index.html` rewrite
  - 배포 후 `aws cloudfront create-invalidation --paths "/*"` 자동 실행
- **Backend**: ECR (`rehearse-backend:prod`, `:prod-<sha>`) → EC2 (`43.201.187.118`) docker compose pull/up

## Release Notes Strategy

- **GitHub Release** 본문에 한국어 변경사항 요약 (Conventional Commits 분류 + 운영 이슈 코멘트)
- `CHANGELOG.md` 없음
- 릴리즈 태그 메시지 = release 본문 첫 단락
- PR 라벨링/PR 본문 활용 — 머지된 FE/BE PR 제목을 카테고리별 정리

## CI Workflow Files

- `.github/workflows/deploy-prod.yml` — main 푸시 시 BE/FE 동시 배포
- `.github/workflows/deploy-dev.yml` — develop 푸시 시 dev 배포
- `.github/workflows/ci.yml` (있다면) — PR 검증

## Special: SEO Prerender

- `frontend/scripts/prerender.mjs` — Puppeteer로 7개 공개 라우트 빌드 후 정적 HTML 생성
- CI에서 시스템 Chrome 사용 (`PUPPETEER_SKIP_DOWNLOAD=true`, `PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome`, job-level env)
- `dist/sitemap.xml` + `dist/robots.txt` 자동 생성 (vite seoPlugin)
- **수동 후속작업**: Naver Search Advisor sitemap 제출, Google Search Console URL inspection

## First-Time Setup Gaps

- none (모든 게이트와 자동화 완비)
