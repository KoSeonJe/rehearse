# Plan 04: `application-dev.yml` 기본값 변경 + OAuth https 픽스 (코드 PR)

> 상태: Draft (v3, 2026-04-08 개정)
> 작성일: 2026-04-08
> 의존: plan-01, plan-03(Step 1: Google https 추가) 완료 후 머지 가능

## Why

EC2에서 dev 프로파일(`SPRING_PROFILES_ACTIVE=dev`)로 백엔드가 동작하므로, `application-dev.yml`이 다음 두 가지를 결정한다:

1. **`frontend-url` / `cors.allowed-origins` 기본값** — env 주입 실패 시 fallback. 현재 apex로 떨어져 있어 dev 재이전 후에도 실수 여지가 있다 (실측: EC2 `.env`에 `FRONTEND_URL`이 아예 없어서 fallback이 적용 중)
2. **`server.forward-headers-strategy`** — Spring Boot가 Nginx의 `X-Forwarded-Proto: https`를 신뢰할지 여부. 현재 미설정이라 Spring이 자기 자신을 HTTP 서버로 인식 → OAuth `redirect_uri`가 `http://api-dev.rehearse.co.kr/login/oauth2/code/google`로 생성됨 (실측 확인됨, 보안 경계 깨짐)

본 PR은 두 가지 변경을 한 파일(`application-dev.yml`)에서 같이 처리한다. 별건이지만 같은 파일이고 plan-05의 OAuth E2E 검증과 직결되어 있어 묶는 것이 효율적.

prod 프로파일(`application-prod.yml`)은 **건드리지 않는다** — 추후 신규 prod 서버용으로 보존.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/application-dev.yml` | (a) `frontend-url` fallback → dev, (b) `cors.allowed-origins` fallback 확장, (c) `server.forward-headers-strategy: framework` 추가 |
| `backend/.env.example` | `FRONTEND_URL` 항목 추가, `CORS_ALLOWED_ORIGINS` 문구 재확인 |

## 상세

### 1. `application-dev.yml` 변경
**Before**
```yaml
app:
  frontend-url: ${FRONTEND_URL:https://rehearse.co.kr}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}

# (server: port: 8080 만 있고 forward-headers-strategy 없음)
server:
  port: 8080
```

**After**
```yaml
app:
  frontend-url: ${FRONTEND_URL:https://dev.rehearse.co.kr}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,https://dev.rehearse.co.kr}

server:
  port: 8080
  forward-headers-strategy: framework  # Nginx X-Forwarded-* 헤더 신뢰
```

#### 변경 (a)(b) — fallback 정리
EC2 런타임은 `.env`가 덮어쓰지만(plan-05에서 추가), 신규 부트스트랩/로컬 재현 시 올바른 도메인으로 폴백되도록 기본값 정정. CORS 기본값에 `localhost:5173`을 유지하는 이유: 로컬 개발자가 `SPRING_PROFILES_ACTIVE=dev`로 띄울 때 대비.

#### 변경 (c) — `forward-headers-strategy: framework`
- **목적**: Spring Boot가 `X-Forwarded-Proto: https` 헤더를 신뢰해 자기 자신을 HTTPS 컨텍스트로 인식 → OAuth redirect_uri를 `https://`로 생성
- **선택 근거**: `framework`는 Spring의 표준 `ForwardedHeaderFilter` 사용. `native`(Tomcat RemoteIpValve)보다 설정 유연성 높음. Spring Boot 공식 권장
- **부수효과**: `request.getScheme()`, `getRequestURL()`, `ServletUriComponentsBuilder.fromCurrentRequest()` 등 모든 scheme 관련 API가 `https://` 반환. 영향받는 코드:
  - `OAuth2SuccessHandler` redirect URL — 변화 없음 (`${app.frontend-url}` 사용)
  - Spring Security CSRF/세션 쿠키의 `Secure` 플래그 — `true`로 설정될 수 있음 → 정상
  - presigned URL/이메일 링크 등 절대 URL을 생성하는 코드가 있다면 전부 `https://`로 변환됨 → 정상 방향

### 2. `backend/.env.example` 정리
```
# Frontend/CORS
FRONTEND_URL=https://dev.rehearse.co.kr
CORS_ALLOWED_ORIGINS=https://dev.rehearse.co.kr
```
기존에 `CORS_ALLOWED_ORIGINS`만 있었으면 `FRONTEND_URL`도 추가.

### 3. 로컬 검증
```bash
cd backend
./gradlew test
```
yaml 파싱/바인딩 에러 없는지 확인.

### 4. 머지 전 사전조건 (Gate — plan-03 Step 1과 직결)
PR을 만들 수는 있지만 **머지 전에 반드시** Google OAuth Client에 `https://api-dev.rehearse.co.kr/login/oauth2/code/google` redirect URI가 **추가**되어 있어야 한다 (plan-03 Step 1). 이 사전 조건이 없으면 배포 직후 Google 로그인이 즉시 `redirect_uri_mismatch`로 실패한다.

GitHub OAuth App는 callback URL을 1개만 등록할 수 있으므로, **배포 직후 즉시 교체하는 절차**가 필요하다 (plan-05 Step 2-bis 참조).

### 5. PR 생성
- **브랜치**: `refactor/dev-domain-restore`
- **PR 제목**: `[BE] refactor: dev 프론트 도메인 dev.rehearse.co.kr 복구 + OAuth https 픽스`
- **Base**: `develop`
- **본문**
  - Why
    - 곧 신규 prod 서버를 띄울 예정이라 apex를 prod용으로 회수
    - 별건이지만 같은 파일이라 OAuth `redirect_uri`가 `http://`로 나가는 문제도 함께 픽스 (`X-Forwarded-Proto` 무시 이슈)
  - Scope: dev 프로파일 3줄 변경 + `.env.example` 1줄. prod 프로파일 비영향
  - 사전조건: Google OAuth Client에 `https://` callback이 등록돼 있어야 머지 가능
  - GitHub OAuth는 배포 직후 콘솔에서 즉시 https로 교체 필요 (plan-05 Step 2-bis)
  - 검증: `./gradlew test`, plan-05의 OAuth E2E

### 6. 커밋 메시지
```
refactor: dev 프로파일 도메인 fallback 정리 + forward-headers-strategy 추가

application-dev.yml 변경 사항:
1. frontend-url / cors.allowed-origins fallback을 dev.rehearse.co.kr로 정정
2. server.forward-headers-strategy=framework 추가
   → Spring이 Nginx의 X-Forwarded-Proto를 신뢰
   → OAuth redirect_uri가 http:// 대신 https://로 생성됨
   → Google/GitHub OAuth 콘솔의 callback URL도 https로 동기화 필요

EC2 런타임은 .env가 덮어쓰지만, 신규 부트스트랩/로컬 재현 시 올바른
도메인으로 폴백되도록 기본값 정정. prod 프로파일은 손대지 않음.

Constraint: prod 프로파일(application-prod.yml)은 건드리지 않음
Constraint: Google OAuth Client에 https:// callback이 사전 등록돼 있어야 함
Rejected: prod 프로파일도 같이 바꾸기 | 추후 신규 prod 서버가 apex를 쓸 예정이라 의도적 보존
Rejected: forward-headers-strategy를 별도 PR로 분리 | 같은 파일 + 같은 E2E 검증 경로라 묶음 처리가 효율적
Confidence: high
Scope-risk: moderate (forward-headers-strategy는 scheme 인식이 바뀌어 부수효과 가능)
Directive: OAuth callback 도메인(api-dev.rehearse.co.kr)은 변경 금지
Directive: GitHub OAuth App callback은 배포 직후 콘솔에서 즉시 https로 교체 필요
Not-tested: forward-headers-strategy 변경이 presigned URL/이메일 절대 링크에 미치는 부수효과
```

## 담당 에이전트

- Implement: `backend` — yaml 변경, `.env.example` 정리
- Review: `code-reviewer` — 스코프 최소성 유지, prod 프로파일 비영향 확인, forward-headers-strategy 부수효과 점검
- Review: `architect-reviewer` — Nginx 헤더 신뢰 모델이 합리적인지 (Nginx만 직접 트래픽 받으므로 안전)

## 검증

- [ ] `./gradlew test` 통과
- [ ] `application-dev.yml` diff 3줄 (frontend-url, cors.allowed-origins, server.forward-headers-strategy)
- [ ] `application-prod.yml` 변경 없음
- [ ] `application-local.yml` 변경 없음
- [ ] `.env.example`에 `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` 둘 다 존재
- [ ] PR이 develop 대상으로 생성되고 Backend CI 통과
- [ ] **Gate**: plan-03 Step 1(Google https callback 추가) 완료 확인 후 머지
- [ ] `progress.md` Task 4 → Completed (머지 시점)
