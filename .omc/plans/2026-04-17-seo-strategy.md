# SEO 본격 최적화 전략 (Phase 1~5)

## Status: In Progress

## Why

`2026-04-16-seo.md`로 기본 SEO 인프라(meta, OG, robots, sitemap)는 갖췄지만, 다음 두 병목으로 검색 노출이 저조:

1. **콘텐츠 페이지가 home 1개뿐** — 검색 키워드("AI 모의면접", "개발자 모의면접" 등)에 매칭될 색인 풀 부재.
2. **CSR(Vite SPA)** — 네이버 Yeti 봇은 JS 실행이 매우 제한적이라 정적 HTML이 없으면 색인 자체가 어려움. 카카오/페북 OG 크롤러도 JS 미실행.

본 spec은 PR #319(이번 사고로 main 직접 머지된 SEO 베이스라인)을 develop으로 백포트한 뒤(완료, PR #320), 그 위에 진짜 검색 노출까지 도달하는 5단계 로드맵을 정의한다.

## Goal

- **단기(4주)**: 브랜드 키워드 "리허설" 검색 시 자사 사이트 1페이지 노출.
- **중기(8주)**: 일반 키워드 "AI 모의면접", "개발자 모의면접" Top 10 진입 시도.
- **측정**: GSC 노출 수, CTR, 평균 순위. Naver Search Advisor 색인 페이지 수.

## Evidence

- 검색엔진별 JS 렌더링 한계: Naver Yeti 가이드 — 정적 HTML 우선. Google는 렌더 큐 지연으로 신규 페이지 색인 지체 사례 다수.
- 키워드 "AI 모의면접" 월 검색량 추정 (네이버 키워드 도구) — 브랜드 신생사 진입 가능 구간.
- 경쟁사(잡다, 인터뷰미 등) SSR/SSG 채택 → CSR로는 동등 경쟁 어려움.

## Trade-offs

- **vs 현 SPA 유지**: 색인성 ↑, 빌드 시간/배포 복잡도 ↑.
- **vs Next.js 마이그레이션**: 효과는 동등하지만 스택 전환 비용이 큼. Vite 기반 prerender(react-snap 또는 vite-ssg)로 점진 도입.
- **콘텐츠 페이지 제작 비용**: 가이드/FAQ 글쓰기 인건비 발생. 외주 또는 자체 작성.

## Scope

### 1차 타겟 키워드
| 키워드 | 페이지 | 검색량(추정) |
|--------|--------|-------------|
| 리허설 (브랜드) | `/` | 신규 — 브랜드 진입 |
| AI 모의면접 | `/`, `/guide/ai-mock-interview` | 중 |
| 개발자 모의면접 | `/`, `/guide/developer-interview-prep` | 중 |
| AI 면접 연습 | `/guide/ai-mock-interview` | 중 |
| 이력서 기반 면접 | `/guide/resume-based-interview` | 저-중 |

### 2차 (롱테일)
- 백엔드 모의면접, 프론트엔드 모의면접
- 신입 개발자 면접 준비
- AI 면접관

### 페이지 ↔ 키워드 매핑
| 페이지 | 1차 키워드 | 보조 키워드 |
|--------|-----------|-------------|
| `/` (home) | AI 모의면접, 리허설 | 개발자 면접, 면접 연습 |
| `/guide/ai-mock-interview` | AI 모의면접 | AI 면접관, AI 면접 연습 |
| `/guide/developer-interview-prep` | 개발자 모의면접 | 개발자 면접 준비, 신입 면접 |
| `/guide/resume-based-interview` | 이력서 기반 면접 | 이력서 면접, 맞춤 질문 |
| `/faq` | 리허설 사용법 | AI 면접 정확도, 영상 면접 |
| `/about` | 리허설 (브랜드 신뢰) | — |

## Implementation

### Phase 1 — SEO 베이스라인 강화 (1차 PR: `feat/seo-baseline`)

**1.2 환경별 SEO 분기 인프라**
- `frontend/.env.development`, `.env.production` — `VITE_SITE_URL` 정의
- `vite.config.ts` `transformIndexHtml` plugin로 `%VITE_SITE_URL%` 토큰 치환
- dev 빌드: `<meta name="robots" content="noindex, nofollow">` 글로벌 강제
- dev 빌드: Google/Naver verification meta 제거 (prod-only)
- canonical, og:url 동적화

**1.3 index.html 메타 보강**
- `<title>`: `리허설 | AI 모의면접 · 개발자 면접 연습 플랫폼` (60자 이내)
- `description`: "리허설은 AI 면접관과 진행하는 개발자 모의면접 플랫폼입니다. 이력서 기반 맞춤 질문, 영상 녹화, 타임스탬프 피드백으로 실전처럼 면접을 연습하세요." (155자 이내)
- `keywords`: 네이버용 — `AI 모의면접, 개발자 모의면접, 리허설, AI 면접, 면접 연습, 백엔드 면접, 프론트엔드 면접, 신입 개발자 면접`
- `<meta name="author">`, `<meta name="application-name">` 추가
- og:image (별도 PR — 디자이너 작업 후): `/og-image.png` 1200x630, alt/width/height
- Twitter Card `summary_large_image`

**1.4 구조화 데이터 (JSON-LD)** — `index.html`에 정적 삽입
- `Organization`: 회사명, URL, 로고, sameAs
- `WebSite`: SearchAction (사이트링크 검색박스 후보)
- `SoftwareApplication`: name, applicationCategory=BusinessApplication, operatingSystem=Web, offers(베타 무료)

**1.5 sitemap & robots 자동화**
- 빌드 타임 sitemap 생성 — 라우트 목록에서 공개 페이지만 추려 sitemap.xml 출력
- `lastmod` 빌드 시간으로 자동 입력
- robots.txt도 빌드 시 `VITE_SITE_URL` 치환

### Phase 2 — 콘텐츠 SEO (2차 PR: `feat/seo-content`)
- `/guide/ai-mock-interview` (1500~3000자, H1~H3 구조, JSON-LD Article)
- `/guide/developer-interview-prep`
- `/guide/resume-based-interview`
- `/faq` (10~15 Q&A, JSON-LD FAQPage)
- `/about` (서비스 소개, 미션)
- home-page H1/시맨틱 보강

### Phase 3 — SSG/Prerender (3차 PR: `feat/seo-prerender`)
- `vite-plugin-prerender` 또는 `react-snap` 도입
- 프리렌더 대상: `/`, `/guide/*`, `/faq`, `/about`, `/privacy-policy`
- 비공개 라우트는 SPA 유지
- CloudFront 라우팅/캐시 정책 검토

### Phase 4 — Core Web Vitals (병행/후속)
- Lighthouse 측정 → LCP < 2.5s, CLS < 0.1, INP < 200ms (mobile)
- 이미지 lazy/srcset, font preload, code split

### Phase 5 — 등록 및 모니터링
- GSC sitemap 제출, Naver Search Advisor 등록
- (선택) GA4/Plausible 분석 도구
- 월 1회 키워드 순위/CTR 리뷰

## Agent Assignment

| Task | Implement | Review |
|------|-----------|--------|
| 1.2~1.5 | `frontend` | `code-reviewer` (환경 분기 누락 검증) |
| og:image 디자인 | `designer` | — |
| Phase 2 페이지 | `frontend` + `writer`(콘텐츠) | `code-reviewer` |
| Phase 3 SSG | `frontend` | `architect-reviewer` (빌드 파이프라인) |
| Phase 4 성능 | `performance-engineer` | — |

## Acceptance Criteria

**Phase 1 완료 조건**
- [ ] `view-source:https://rehearse.co.kr/` 에 새 메타/JSON-LD 모두 표시
- [ ] dev URL view-source에 `noindex` 확인 + verification meta 부재
- [ ] [Rich Results Test](https://search.google.com/test/rich-results) Organization/WebSite/SoftwareApplication 인식
- [ ] sitemap.xml 빌드 시 자동 생성, 공개 라우트 모두 포함
- [ ] CI green

**Phase 2 완료 조건**
- [ ] 새 라우트 모두 200 OK
- [ ] 각 가이드 페이지 본문 1500자 이상
- [ ] FAQPage 리치 결과 검증 통과
- [ ] 페이지별 Helmet title/description 차별화

**Phase 3 완료 조건**
- [ ] `curl https://rehearse.co.kr/guide/ai-mock-interview` 응답에 본문 텍스트 존재
- [ ] `curl -A "Yeti" https://rehearse.co.kr/` 응답에 키워드 존재
- [ ] CloudFront 캐시 invalidation 자동화

**Phase 4/5는 별도 측정 spec으로 관리**

## 변경/검토 대상 파일

**Phase 1**
- `frontend/.env.development`, `.env.production`
- `frontend/vite.config.ts`
- `frontend/index.html`
- `frontend/public/robots.txt`
- `frontend/scripts/generate-sitemap.ts` (또는 vite plugin 도입)
- `frontend/package.json`

**Phase 2**
- `frontend/src/pages/guide/*-page.tsx` (3개)
- `frontend/src/pages/faq-page.tsx`
- `frontend/src/pages/about-page.tsx`
- 라우터 정의 (`frontend/src/app.tsx` 등)
- `frontend/src/pages/home-page.tsx`

**Phase 3**
- `frontend/package.json`, `frontend/vite.config.ts`
- 빌드 스크립트, `.github/workflows/deploy-*.yml`

## Notes

- 이번 사고(SEO PR #319 main 직접 머지) 수습은 PR #320으로 완료. 본 spec은 그 다음 단계.
- 모든 PR은 반드시 `/create-pr` 스킬 경유 (`base develop`, BE/FE 분리, 한국어 컨벤션).
- 재발 방지: GitHub `main` branch protection 강화는 별도 운영 작업 (devops).
