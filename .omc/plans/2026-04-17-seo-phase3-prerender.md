# SEO Phase 3 — SSG/Prerender 도입 상세 계획

## Status: Proposed (사용자 승인 대기)

## Why

Phase 1·2 완료로 메타·JSON-LD·콘텐츠 페이지가 갖춰졌지만, **현재는 CSR(Vite SPA)이라 검색엔진이 콘텐츠를 못 볼 가능성이 큼.**

- 구글봇: JS 렌더링 지원하지만 렌더 큐 지연/실패 사례 다수. 신규 도메인일수록 색인 지체 큼.
- **네이버 Yeti: JS 실행 매우 제한적** → 정적 HTML이 없으면 색인 거의 불가능.
- 카카오/페북 OG 크롤러: JS 미실행 → 페이지별 Helmet 메타가 SNS 공유 미리보기에 반영되지 않음.

→ 이번 Phase로 **공개 라우트만 빌드 타임에 정적 HTML로 생성**해서 위 세 가지 문제를 동시에 해결.

## Goal

- 공개 라우트(`/`, `/about`, `/faq`, `/guide/*`, `/privacy`)에 `curl`로 접속 시 **본문 HTML이 그대로 반환**되어야 함.
- 비공개 라우트(`/dashboard`, `/interview/*`)는 SPA 그대로 유지.
- 페이지별 Helmet 메타가 정적 HTML에 인라인되어 SNS 공유/네이버 색인 가능.

## Trade-offs / 대안 비교

| 옵션 | 변경 범위 | 빌드 시간 | 인프라 영향 | 평가 |
|------|----------|----------|------------|------|
| A. **react-snap** (postbuild puppeteer) | 추가 의존성 1개 + script | +30~60s | **CloudFront 라우팅 변경 필수** | 코드 단순 |
| B. **vite-react-ssg** (proper SSG) | 앱 진입점 리팩토링 | +20s | A와 동일 | 가장 표준이나 침습 큼 |
| C. **Vike (구 vite-plugin-ssr)** | 라우터/페이지 디렉토리 구조 변경 | +30s | A와 동일 | 마이그레이션 비용 큼 |
| D. **커스텀 puppeteer 스크립트** | scripts/ 신규 1개 | +30~60s | A와 동일 | 의존성/통제 균형 ★ |
| E. **Next.js 마이그레이션** | 전면 재작성 | — | 별도 호스팅 | 본 프로젝트엔 과함 |

**권장: D (커스텀 puppeteer)** — 가장 적은 코드 변경으로 결과물 통제 가능. 의존성도 puppeteer 한 개.

## 인프라 결정 사항 (사용자 승인 필요)

현재 CloudFront 설정: 403/404 → `/index.html` (200) 폴백 — 전형적인 SPA 라우팅.

prerender 결과물을 정상 서빙하려면 다음 중 하나 필요.

### 옵션 1. CloudFront Function (viewer-request URL rewrite) — 권장

```javascript
// CloudFront Function (JavaScript runtime)
function handler(event) {
  const request = event.request;
  const uri = request.uri;
  if (uri.endsWith('/')) {
    request.uri = uri + 'index.html';
  } else if (!uri.includes('.')) {
    request.uri = uri + '/index.html';
  }
  return request;
}
```

- 장점: AWS-native, 추가 비용 거의 0, 1ms 미만 latency
- 단점: AWS Console/CLI에서 직접 배포 필요 (IaC 없음)
- 호환성: 기존 SPA 폴백과 충돌 없음 — 리소스가 없을 때만 폴백 동작

### 옵션 2. CloudFront Error Response 비활성 + S3 Website 모드 전환
- S3 정적 웹사이트 호스팅 모드는 디렉토리 인덱스 자동 처리
- 단점: OAC 보호 해제(공개 버킷) 필요 → **보안 후퇴**, 비추천

### 옵션 3. 빌드 시 flat 파일로 출력 + CI에서 Content-Type 강제
- `dist/guide/ai-mock-interview` (확장자 없음) 파일 + `aws s3 cp --content-type text/html`
- CloudFront 변경 불필요
- 단점: 디렉토리/파일 충돌 가능성, S3 sync에서 Content-Type 일괄 설정 어려움

### 권장 결정
**옵션 1 (CloudFront Function)** — 가장 클린. 사용자가 AWS Console에서 1회 배포 + Distribution에 attach.

## Implementation 단계

### Sub-phase 3A: 프리렌더 파이프라인 (코드만, infra 무관)

1. **라우트 목록 공유 모듈로 추출** (전제 작업)
   - 현재 `vite.config.ts`의 `PUBLIC_ROUTES`는 `const` (export 안 됨) + TS + Vite 전용 의존성. `.mjs` 스크립트에서 import 불가.
   - `frontend/scripts/public-routes.mjs` (또는 `.json`)에 라우트 목록을 정규 위치로 두고, vite.config.ts와 prerender 스크립트가 둘 다 import.

2. **의존성 추가**: `puppeteer` (frontend devDependencies)
   - `.puppeteerrc.cjs`에 `cacheDirectory` 명시(또는 `frontend/.gitignore`에 `.cache/puppeteer/` 추가)

3. **스크립트 추가**: `frontend/scripts/prerender.mjs`
   - dist/ 디렉토리에 정적 HTTP 서버 띄움 (Node http)
   - puppeteer launch args: **`--no-sandbox --disable-setuid-sandbox` 필수** (CI ubuntu-latest sandbox 미지원)
   - 각 라우트:
     - puppeteer navigate
     - `await page.waitForFunction(() => document.querySelector('#root')?.innerText?.length > 50)` 로 콘텐츠 렌더 대기
     - `await page.waitForFunction(() => document.title && document.title !== 'Vite + React')` 로 Helmet 동기화 대기
     - `document.documentElement.outerHTML` 캡처
     - 출력: 6개 신규 + 1개 덮어쓰기 (총 7 라우트, root는 dist/index.html 덮어쓰기)
   - puppeteer + 서버 종료

4. **하이드레이션 정책 결정**
   - 현재 `main.tsx`는 `createRoot()` 사용 → prerender HTML이 와도 React가 throw away 후 재렌더 (콘텐츠 깜빡임 가능).
   - **결정 A (권장)**: 그대로 둔다. 이유: (1) 사용자 체감엔 거의 영향 없음(즉시 동일 콘텐츠 재렌더), (2) `hydrateRoot` 전환 시 마크업 mismatch 디버깅 부담 큼, (3) prerender 목적은 크롤러용.
   - 결정 B: 공개 라우트만 `hydrateRoot` 사용. main.tsx 분기 필요. 본 spec 범위 외.

5. **package.json 업데이트**:
   - `"build": "tsc -b && vite build && node scripts/prerender.mjs"`

6. **CI 영향 (deploy-dev.yml / deploy-prod.yml frontend-build job)**
   - `frontend-build` 잡은 `ubuntu-latest`에서 실행 → 시스템에 Chrome 사전 설치됨.
   - **PUPPETEER_SKIP_DOWNLOAD를 `npm ci` 시점부터 적용**해야 Chromium 다운로드(~280MB) 회피 가능 → `env:` 를 step이 아니라 **job-level**에 선언:
     ```yaml
     frontend-build:
       runs-on: ubuntu-latest
       env:
         PUPPETEER_SKIP_DOWNLOAD: "true"
         PUPPETEER_EXECUTABLE_PATH: /usr/bin/google-chrome
     ```

7. **Helmet 처리**: react-helmet-async는 클라이언트에서 마운트 후 비동기로 head를 갱신. puppeteer는 위 4번의 `waitForFunction`으로 마운트 완료까지 대기 후 캡처 → Helmet 메타가 정적 HTML에 인라인됨.

8. **배포 워크플로우 캐시 헤더 수정 (필수)** ⚠️
   - `deploy-prod.yml` 현재는 `dist/`를 `--exclude "index.html"` + 1년 immutable 캐시로 sync, 그 후 root `index.html`만 no-cache로 별도 업로드.
   - prerender 도입 후 `dist/guide/ai-mock-interview/index.html` 등 **하위 디렉토리 index.html이 1년 캐시로 업로드되어 콘텐츠 갱신 시 stale** 발생.
   - 수정: sync 단계에 `--exclude "*/index.html" --exclude "index.html"` + 별도 단계로 모든 `index.html`을 no-cache 업로드(예: `find dist -name index.html | xargs ...`).
   - deploy-dev.yml은 현재 sync만 하므로 영향 적지만 일관성 위해 동일 수정 권장.

### Sub-phase 3B: CloudFront 라우팅 (인프라)

1. AWS Console → CloudFront Functions → 새 함수 생성 (코드는 위 옵션 1 참조)
2. Publish
3. Distribution `d2n8xljv54hfw0` 의 default behavior에 viewer-request로 attach
4. 캐시 무효화 (`/*`)

**SPA 폴백과의 상호작용 (명시)**
- 공개 라우트 `/about`: CF Function이 `/about/index.html`로 rewrite → S3에 prerender된 파일 존재 → 200 OK 정적 HTML 반환.
- 비공개 라우트 `/dashboard`: CF Function이 `/dashboard/index.html`로 rewrite → S3에 객체 없음 → 403 → 기존 CloudFront error response가 `/index.html` 200으로 폴백 → CSR로 React Router가 `/dashboard` 처리.
- 두 경로 모두 정상 동작. 기존 SPA 폴백 제거하지 말 것.

**검증**
- `curl https://rehearse.co.kr/guide/ai-mock-interview` → 본문 HTML 200 OK
- `curl https://rehearse.co.kr/dashboard` → 폴백으로 SPA 셸 (CSR로 로그인 페이지)

### Sub-phase 3C: 검증 및 모니터링

- Lighthouse SEO 점수 측정 (전후 비교)
- Google Search Console URL 검사 → 색인 가능 여부
- Naver Search Advisor 사이트맵 재제출
- 4주간 색인 페이지 수 추이 모니터링

## 변경/검토 대상 파일

### Sub-phase 3A
- `frontend/scripts/public-routes.mjs` (신규 — 라우트 목록 단일 출처)
- `frontend/vite.config.ts` (PUBLIC_ROUTES를 위 모듈에서 import)
- `frontend/scripts/prerender.mjs` (신규)
- `frontend/.puppeteerrc.cjs` (신규 — cache 디렉토리 명시)
- `frontend/package.json` (puppeteer 추가, build 스크립트 변경)
- `.github/workflows/deploy-dev.yml` (frontend-build job-level env로 PUPPETEER_SKIP_DOWNLOAD/EXECUTABLE_PATH)
- `.github/workflows/deploy-prod.yml` (위 + sync 캐시 헤더 단계 수정)
- `frontend/.gitignore` (puppeteer 캐시 무시)

### Sub-phase 3B (인프라, 코드 외)
- AWS CloudFront Function 1개
- AWS CloudFront Distribution behavior 1회 수정

## 리스크 및 완화

| 리스크 | 완화 |
|--------|------|
| 빌드 시간 증가 (~30~60s) | CI 캐시 + 라우트 병렬 prerender |
| puppeteer/Chrome 설치 실패 | GitHub Actions ubuntu-latest 사전 설치 Chrome 사용 |
| Helmet 메타 누락 (타이밍) | 명시적 `await page.waitForFunction(...)` |
| CloudFront Function 미적용 시 prerender 무용 | 3A 머지 전에 3B 인프라 작업 완료 권장 (또는 같은 PR에서 IaC 추가) |
| 비공개 라우트가 prerender되어 보안 데이터 노출 | PUBLIC_ROUTES만 prerender. 명시적 화이트리스트. |
| dev/prod URL 분기 | 3A의 `BASE_URL` 환경변수로 분리 |
| **`s3 sync --delete`로 prerender 디렉토리 의도치 않게 삭제** | prerender 단계가 vite build 후 실패하면 dist에 하위 디렉토리 부재 상태로 sync → 기존 prerender 파일 wipe. 완화: build 스크립트에서 prerender 실패 시 exit 1로 잡 fail시켜 deploy 단계 진입 차단 (CI의 `needs:` 의존성). |
| **하이드레이션 mismatch로 prerender 콘텐츠 깜빡임** | 결정 A 채택: createRoot 유지, prerender는 크롤러 전용으로 활용. 사용자 체감 영향 미미. 필요 시 후속 PR에서 hydrateRoot 도입. |
| **stale cached HTML on update** | 위 Sub-phase 3A.8 캐시 헤더 수정으로 해결. PR에서 반드시 검증. |

## 진행 권장 순서

1. **본 spec PR 머지** (인프라 결정 합의 도장)
2. 사용자가 옵션 1(CloudFront Function) 동의 시 → AWS Console에서 함수 배포 + behavior attach (사용자 직접 또는 인프라 담당)
3. **Sub-phase 3A PR 작성** (puppeteer 스크립트 + build 통합)
4. dev 배포 → curl로 본문 HTML 확인
5. prod 배포 → GSC/Naver 색인 모니터링

## Acceptance Criteria

- [ ] 사용자가 CloudFront 라우팅 옵션(1/2/3 중 1개) 선택
- [ ] Sub-phase 3A PR — `npm run build` 후 `dist/guide/ai-mock-interview/index.html` 등 7개 정적 HTML 생성
- [ ] 각 정적 HTML에 페이지별 title/description/JSON-LD 인라인 확인
- [ ] CI green
- [ ] dev 배포 후 `curl -A "Yeti" https://dev.rehearse.co.kr/guide/ai-mock-interview` 응답 본문에 "AI 모의면접" 텍스트 존재
- [ ] prod 배포 후 동일 검증 + Lighthouse SEO ≥ 95

## Notes

- Phase 4 (Core Web Vitals 개선) 와 병행 가능하나 충돌 없으므로 3 → 4 순서 권장.
- 본 spec은 기획·옵션 비교만 — 실제 코드/인프라 변경은 별도 PR.
