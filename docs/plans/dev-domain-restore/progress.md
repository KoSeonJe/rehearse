# Dev 프론트 도메인 `dev.rehearse.co.kr` 재이전 + OAuth https 픽스 — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 의존 | 비고 |
|---|--------|------|------|------|
| 1 | 현재 인프라/OAuth 상태 조사 + 스냅샷 (CustomErrorResponses/Logging 포함) | Draft | — | `plan-01-infra-survey.md` |
| 2 | CloudFront + ACM + DNS가 이미 dev를 커버함을 공식 확인 + invalidation | Draft | 1 | `plan-02-cloudfront-verify.md` [parallel with 3,4] |
| 3 | Google OAuth https callback 사전 등록 + JS origins 확인 + GitHub callback 사전 메모 | Draft | 1 | `plan-03-oauth-console.md` [parallel with 2,4] |
| 4 | `application-dev.yml` fallback 정리 + `forward-headers-strategy` 추가 (PR) | Draft | 1, 3-Step1 | `plan-04-backend-config.md` [parallel with 2] |
| 5 | EC2 `.env`에 `FRONTEND_URL` 추가 + 재기동 + GitHub callback 즉시 교체 + E2E 검증 | Draft | 2,3,4 | `plan-05-ec2-runtime-switch.md` [blocking] |
| 6 | apex 분리 (CloudFront/DNS/CORS/Google JS origins) + Google http callback 제거 + 문서 완료 | Draft | 5 + 1h(하드) / 24h(권장) | `plan-06-apex-decommission.md` |

## 진행 로그

### 2026-04-08 (v3 개정)
- **OAuth https 마이그레이션을 본 플랜에 통합** — 같은 파일(`application-dev.yml`) 수정이고 plan-05의 OAuth E2E 검증과 직결되어 별건 분리 비효율
- 실측: EC2 SSH 결과 `~/rehearse/backend/.env`에 `FRONTEND_URL`이 없음 → `application-dev.yml:60` fallback이 사용 중 (apex)
- 실측: `curl /oauth2/authorization/google` → `redirect_uri=http://api-dev.rehearse.co.kr/...` → `forward-headers-strategy` 미설정 원인
- 사용자 확인: Google OAuth Client에 `http://` callback이 등록돼 있어 현재는 작동 중. https로 전환 결정
- 변경된 작업 흐름:
  1. plan-03 Step 1에서 Google에 `https://` callback **추가** (http는 유지) — 사전
  2. plan-04 PR에 `forward-headers-strategy: framework` 포함
  3. plan-05 배포 + 직후 GitHub OAuth callback 즉시 교체 (다운타임 ~10초)
  4. plan-06에서 Google `http://` callback 제거

### 2026-04-08 (v2 개정)
- 요구사항 정의 작성 (`requirements.md`) — critic 리뷰 반영해 v2 개정
- 태스크별 플랜 작성 (`plan-01` ~ `plan-06`) — v2 개정
- **실측 발견**: `dig dev.rehearse.co.kr` → 이미 `d2n8xljv54hfw0.cloudfront.net`으로 CNAME 존재. apex와 dev가 같은 CloudFront 배포를 공유하며 alternate domain에 둘 다 등록된 상태
  - → plan-02가 "신규 생성"에서 "현 상태 검증 + invalidation"으로 축소됨
  - → plan-03의 Google OAuth origins에도 dev가 이미 있을 가능성 높음 (확인 필요)
  - → 전체 작업은 본질적으로 `FRONTEND_URL` runtime 전환 + apex 분리 두 가지로 압축됨
- **critic 리뷰 반영 사항**
  - `frontend/src/components/interview/interviewer-video.tsx:8-11`에 `dev.rehearse.co.kr` mp4 URL 4개 하드코딩 — S3에 파일 부재로 404→SPA fallback (현재도 작동 안 하는 상태). 본 플랜 범위 밖, 별건 기록
  - JWT 쿠키 host-only 확인 (`OAuth2SuccessHandler.java:38-43`) → 전환 후 기존 apex 로그인 세션 무효화, 재로그인 필요. requirements에 side-effect로 문서화
  - CORS `setMaxAge(3600L)` 확인 → plan-06 안정화 게이트 최소 1시간 필수
  - CustomErrorResponses(403/404→index.html) 보존이 plan-06 위험점 → plan-01/02 스냅샷으로 기준점 확보, plan-06은 콘솔 편집 권장
  - CloudFront Logging 활성화 여부를 plan-01 점검 항목에 추가 (plan-06 게이트 전제)
  - plan-06 apex 분리 후 `dig` 기대값을 "NXDOMAIN"이 아닌 "A/CNAME 레코드 없음"으로 정정
- 별개 이슈 식별 (본 플랜 Out)
  - `application-local.yml:7-12` OAuth client secret 하드코딩 → secret 로테이션 + env 분리 필요
  - `interviewer-video.tsx` mp4 하드코딩 + S3 파일 부재 → 면접관 비디오 기능 복구 필요
