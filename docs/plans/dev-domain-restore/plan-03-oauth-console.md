# Plan 03: Google/GitHub OAuth 콘솔 정리 (JS origins + http→https callback 마이그레이션)

> 상태: Draft (v3, 2026-04-08 개정)
> 작성일: 2026-04-08
> 의존: plan-01 [parallel with plan-02, plan-04]

## Why

OAuth 로그인 플로우는 다음과 같다:
1. 프론트가 `${VITE_API_URL}/oauth2/authorization/{provider}`로 이동 (`login-modal.tsx:25-30`)
2. 백엔드가 provider 인증 페이지로 리다이렉트 (`api-dev.rehearse.co.kr/login/oauth2/code/{provider}`가 callback)
3. 백엔드 `OAuth2SuccessHandler`가 `${app.frontend-url}`로 사용자를 되돌림

본 태스크는 두 가지를 다룬다:

### (1) 프론트 도메인(`dev.rehearse.co.kr`) JS origins 등록 확인
Google OAuth는 Authorized JavaScript origins도 체크하므로, 새 프론트 도메인이 누락되면 `origin_mismatch` 에러 발생 가능. 2026-04-08 실측상 이미 등록돼 있을 가능성이 높지만 plan-01 스냅샷으로 확정.

### (2) **OAuth callback URL을 `http://` → `https://`로 마이그레이션** (별건이지만 같이 처리)
실측 결과 Spring이 Google에 보내는 redirect_uri가 **`http://api-dev.rehearse.co.kr/login/oauth2/code/google`** (HTTP)이고, Google OAuth Client에도 같은 `http://` 버전이 등록돼 있어 현재는 작동 중. 사용자 확인:
- "지금 http 적어놨는데" — Google OAuth Client에 http 버전 등록 확인됨

문제:
- OAuth 인증 코드(`?code=...`)가 평문 HTTP로 콜백 → MITM 탈취 위험
- Google이 단계적으로 실도메인 `http://` callback을 폐기 중 (localhost 외)
- 보안 표준 위배 (RFC 6749는 redirect_uri HTTPS 권장)

→ plan-04에서 `server.forward-headers-strategy: framework` 추가하여 Spring이 `https://`로 redirect_uri를 생성하도록 픽스. 이때 Google/GitHub 콘솔에 `https://` 버전이 등록돼 있어야 매칭 성공. 본 태스크에서 그 사전 작업을 한다.

## 생성/수정 파일

| 리소스 | 작업 |
|------|------|
| Google OAuth 2.0 Client | (a) Authorized JS origins에 dev 추가/확인, (b) Authorized redirect URIs에 `https://` callback 추가 (http는 유지) |
| GitHub OAuth App | callback URL `http://...` → `https://...` 교체 (plan-05 배포 직후 타이밍, 본 태스크에서는 사전 메모만) |
| `docs/plans/dev-domain-restore/survey-snapshot.md` | 변경 후 상태 업데이트 |

## 상세

### Step 1. Google OAuth Client — `https://` callback **추가** (Save 가능)
**위치**: GCP Console → APIs & Services → Credentials → OAuth 2.0 Client IDs → 해당 클라이언트

**Authorized redirect URIs**
```
기존 (유지):
  http://api-dev.rehearse.co.kr/login/oauth2/code/google

추가:
  https://api-dev.rehearse.co.kr/login/oauth2/code/google
```
- **반드시 "추가"만**. 기존 `http://` 버전을 지우지 말 것 — plan-04 배포 전이라 Spring은 여전히 http로 보내고 있음
- Save → Google 측 전파에 최대 5분
- plan-06에서 http 버전 제거

**Authorized JavaScript origins**
- plan-01 스냅샷 기준 `https://dev.rehearse.co.kr` 존재 여부 확인
- 누락이면 추가, 기존 `https://rehearse.co.kr`은 그대로 (plan-06에서 제거)

### Step 2. GitHub OAuth App — 사전 메모만, 실제 교체는 plan-05 배포 직후
**위치**: GitHub Settings → Developer settings → OAuth Apps → 해당 앱

GitHub OAuth App은 **Authorization callback URL을 1개만 등록 가능**하다. Google처럼 "둘 다 추가 후 정리" 방식이 안 된다.
- 현재: `http://api-dev.rehearse.co.kr/login/oauth2/code/github`
- 목표: `https://api-dev.rehearse.co.kr/login/oauth2/code/github`

**교체 시점**: plan-05 Step 2-bis (Spring 배포 + 백엔드 재기동 직후, OAuth E2E 검증 직전). 본 태스크에서는 GitHub 콘솔 URL과 절차만 메모해두고 실제 변경은 하지 않는다.

**예상 다운타임**: ~10초 ~ 1분 (콘솔 저장 + 새 callback 사용 시작 사이의 race)

### Step 3. 로그인 프리체크 (Step 1 직후, plan-04 배포 전)
콘솔 저장 직후, Google이 새 redirect URI를 받아들이는지 간단 확인:
```bash
# 브라우저 시크릿 창
# 1. https://dev.rehearse.co.kr 접속 → 로그인 버튼 → Google
# 2. Google 동의 화면까지 도달하면 origin은 정상
# 3. Google 로그인 완주 시도 → "이 시점엔 Spring이 여전히 http://를 보내므로
#    Google 콘솔에 등록된 http:// 버전과 매칭되어 정상 로그인 성공해야 함"
#    → http://가 등록 안 돼 있으면 redirect_uri_mismatch (이 경우 즉시 에러)
```

만약 이 시점에 redirect_uri_mismatch가 나면, plan-04 배포 후에는 https로 매칭되므로 자연 해소된다. 다만 사용자 영향이 있을 수 있으니 plan-04 배포를 가능한 빨리 이어서 진행.

### Step 4. 스냅샷 업데이트
`survey-snapshot.md`에 다음 기록:
- Google OAuth Client redirect URIs (변경 후 전체 목록)
- Google OAuth Client JS origins (변경 후)
- GitHub OAuth callback URL (현재값, 교체 예정 시각)

## 담당 에이전트

- Implement: `devops-engineer` — Google/GitHub OAuth 콘솔 조작
- Review: `code-reviewer` — 화이트리스트 확장이 최소 권한 원칙 준수하는지 검토
- Review: `qa` — 프리체크 시나리오 실행

## 검증

- [ ] Google OAuth Client Authorized redirect URIs에 `http://api-dev.../login/oauth2/code/google` + `https://api-dev.../login/oauth2/code/google` **둘 다** 존재
- [ ] Google OAuth Client Authorized JavaScript origins에 `https://dev.rehearse.co.kr` 존재 (apex는 plan-06까지 유지)
- [ ] GitHub OAuth App callback URL 현재값이 스냅샷에 기록됨 (교체는 plan-05 Step 2-bis에서)
- [ ] Google 프리체크 시나리오에서 동의 화면까지 도달
- [ ] `survey-snapshot.md` 업데이트
- [ ] `progress.md` Task 3 → Completed
