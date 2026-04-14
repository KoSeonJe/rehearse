# Plan 09: Google / GitHub OAuth prod client 등록

> 상태: Draft
> 작성일: 2026-04-12

## Why

dev OAuth client를 prod에서 재사용하면 다음 문제가 발생한다:

- 로그/쿼터 혼선 (dev 개발 로그인이 prod 감사에 섞임)
- 보안 경계 모호 (dev secret이 유출되면 prod 영향)
- JavaScript origins / redirect URIs에 dev + prod 도메인이 혼재 → 실수 시 교차 리다이렉트
- Google OAuth의 브랜딩 정책: 공개 상태 전환 시 dev 테스트 데이터 축적이 심사에 영향

prod 전용 신규 OAuth App을 발급해 자격을 완전 격리한다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| Google Cloud Console OAuth 2.0 Client (Rehearse Prod) | 신규 생성 |
| GitHub OAuth App (Rehearse Prod) | 신규 생성 |
| EC2-prod `~/rehearse/backend/.env` | 신규 client_id/secret 주입 |
| `docs/architecture/infrastructure-status.md` | OAuth prod client 메타데이터 기록 (secret 제외) |

## 상세

### Google OAuth 2.0 Client (prod)

**Google Cloud Console** → 기존 `Rehearse` 프로젝트 또는 신규 `Rehearse-Prod` 프로젝트(권장: **기존 프로젝트 내 신규 Client** — 브랜드 일관성 + 심사 이력 유지).

1. APIs & Services → Credentials → Create Credentials → OAuth 2.0 Client ID
2. Application type: `Web application`
3. Name: `Rehearse Production`
4. **Authorized JavaScript origins**:
   - `https://rehearse.co.kr`
   - `https://www.rehearse.co.kr`
5. **Authorized redirect URIs**:
   - `https://api.rehearse.co.kr/login/oauth2/code/google`
6. 저장 → `Client ID`, `Client Secret` 발급
7. 두 값을 EC2 `.env`에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`로 저장

**OAuth consent screen**:
- 기존 dev consent screen이 `External` + `Testing` 상태라면, prod 전환 전 `In production` publishing 검토 (Google 심사 필요 — 별건 또는 선행 작업)
- MVP 초기에는 `Testing` 유지 + 테스트 사용자 명시 추가도 가능
- **결정 필요**: prod OAuth consent screen 공개 여부 (심사 소요 1~6주). 본 플랜에서는 `Testing` 유지 전제로 작성

**dev Google client와의 격리**:
- dev client의 JavaScript origins에 `rehearse.co.kr` 포함되지 않도록 확인 — dev-domain-restore에서 이미 제거됨 (plan-06)
- dev client의 redirect URIs에 `api.rehearse.co.kr` 포함되지 않도록 확인

### GitHub OAuth App (prod)

**GitHub** → Developer settings → OAuth Apps → New OAuth App

| 필드 | 값 |
|---|---|
| Application name | `Rehearse Production` |
| Homepage URL | `https://rehearse.co.kr` |
| Application description | "AI 모의면접 플랫폼 Rehearse 프로덕션 환경" |
| Authorization callback URL | `https://api.rehearse.co.kr/login/oauth2/code/github` |
| Enable Device Flow | 비활성 |

생성 후 `Client ID`, `Client Secret`(`Generate a new client secret`) 발급 → EC2 `.env`의 `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`

**GitHub App 소유권**: 팀 공유 시 Organization 계정 또는 공용 관리자 계정 사용. 개인 계정 발급 시 퇴사/이직 리스크.

### scope 정책

`application-dev.yml:10` 기준 (prod도 동일):
- GitHub: `read:user,user:email`
- Google: `email,profile`

**원칙**: MVP 단계에서 최소 권한. 향후 기능 추가 시 scope 추가는 별건 + 사용자 재동의.

### 보안 저장

- Client Secret은 **EC2 `.env`에만 보관**, Git 저장 금지
- GitHub Environments `production` secrets에는 별도 백업 저장도 고려 (plan-10) — 단, EC2 `.env`와 이중 관리 시 불일치 리스크. 원칙: **EC2 `.env`가 단일 소스**, GitHub Environment에는 배포 관련 secret만 (EC2 SSH 키 등)
- 1Password / 팀 비밀 관리 도구에 1회 복사해 backup

### dev OAuth app 재확인

- Google dev client: redirect URI에 `api-dev.rehearse.co.kr`만 포함 (apex / api 없음)
- GitHub dev OAuth App (`Rehearse Dev`로 rename 권장): callback `https://api-dev.rehearse.co.kr/login/oauth2/code/github`

## 담당 에이전트

- Implement: `devops-engineer` — OAuth 콘솔 설정
- Review: `code-reviewer` — 보안 경계, secret 관리 규칙

## 검증

- Google Cloud Console에서 신규 `Rehearse Production` client 존재 + origins/redirect URIs 확인
- GitHub OAuth Apps 목록에 `Rehearse Production` 존재 + callback URL 확인
- EC2 `.env`에 prod client_id/secret 주입 완료
- 컷오버 후 (plan-13):
  - `https://rehearse.co.kr`에서 `Sign in with GitHub` → GitHub 승인 화면 → 리다이렉트 성공 → JWT 쿠키 세팅
  - `Sign in with Google` 동일 시나리오 성공
- dev 로그인 회귀 없음 (`https://dev.rehearse.co.kr`에서 GitHub/Google 로그인 성공)
- `progress.md` Task 9 → Completed
