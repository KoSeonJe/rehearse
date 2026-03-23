# OAuth2 소셜 로그인 (GitHub + Google)

**Status**: In Progress
**Date**: 2026-03-24

---

## Why

현재 리허설 서비스는 인증 없이 모든 페이지에 접근 가능하다. 면접 준비 플로우는 사용자별 이력서 업로드, 질문 생성, 피드백 이력 관리가 필요하므로 인증이 필수다. GitHub/Google OAuth를 통해 회원가입 마찰을 최소화하고, 개발자 타겟 서비스 특성상 GitHub 계정 연동이 자연스럽다.

**Goal**: 로그인 없이 랜딩 페이지 열람 가능, `/interview/setup` 이후 플로우는 인증 필요
**Measure**: 로그인 후 인터뷰 생성 → 피드백 조회까지 완전한 인증 플로우 동작

---

## 기술 결정

| 결정 | 선택 | 이유 |
|------|------|------|
| OAuth 제공자 | GitHub + Google | 개발자 타겟 → GitHub 친숙, 범용성 위해 Google 추가 |
| 토큰 방식 | JWT (Stateless) | Spring Boot REST API, 수평 확장 고려 |
| 토큰 저장 | HttpOnly Cookie | XSS 공격 방어, CORS credentials 이미 활성화 |
| OAuth Flow | Spring 표준 Redirect | Spring Security OAuth2 Client 기본 플로우 활용 |
| JWT 라이브러리 | jjwt 0.12.x | Spring 생태계 표준, 최신 API |

---

## BE 구현 범위 (`feat/auth-oauth2-jwt`)

### 의존성 (`build.gradle.kts`)
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-client`
- `io.jsonwebtoken:jjwt-api:0.12.3` + impl + jackson

### DB Migration V14
```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  profile_image VARCHAR(500),
  provider VARCHAR(20) NOT NULL,
  provider_id VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  created_at DATETIME(6),
  updated_at DATETIME(6),
  UNIQUE KEY uk_provider_provider_id (provider, provider_id)
);
```

### 도메인/패키지 구조
```
domain/user/entity/User.java
domain/user/entity/UserRole.java       (enum: USER, ADMIN)
domain/user/entity/OAuthProvider.java  (enum: GITHUB, GOOGLE)
domain/user/repository/UserRepository.java
domain/user/service/UserService.java

domain/auth/controller/AuthController.java   (GET /api/v1/auth/me, POST /api/v1/auth/logout)
domain/auth/dto/UserResponse.java
domain/auth/exception/AuthErrorCode.java

global/security/config/SecurityConfig.java
global/security/jwt/JwtTokenProvider.java
global/security/jwt/JwtAuthenticationFilter.java
global/security/oauth2/CustomOAuth2UserService.java
global/security/oauth2/CustomOAuth2User.java
global/security/oauth2/OAuth2SuccessHandler.java   → JWT cookie → FE redirect
global/security/oauth2/OAuth2FailureHandler.java   → /login?error=
```

### Security 규칙
- `/api/v1/auth/**` → permitAll
- `/api/internal/**` → InternalApiKeyFilter (기존 유지)
- `/api/v1/interviews/**` → JWT 인증 필요
- CSRF disable, SessionCreationPolicy.STATELESS

---

## FE 구현 범위 (`feat/auth-login-ui`)

```
src/stores/auth-store.ts
src/hooks/use-auth.ts
src/components/ui/protected-route.tsx
src/pages/login-page.tsx           (/login)
src/app.tsx                        (Protected Route 감싸기)
src/pages/home-page.tsx            (헤더 로그인 버튼)
src/components/home/hero-section.tsx (CTA 인증 분기)
src/lib/api-client.ts              (401 → /login redirect)
```

### 로그인 페이지 UI
- 중앙 카드: `bg-white rounded-[32px] shadow-toss-lg`
- GitHub 버튼 (검정), Google 버튼 (흰색 + 테두리)
- OAuth URL: `window.location.href = ${VITE_API_URL}/oauth2/authorization/{provider}`
- 처음 방문 시 자동 회원가입 안내

---

## Trade-offs

| 항목 | 선택 | 포기한 것 |
|------|------|----------|
| Stateless JWT | 수평 확장 용이 | 토큰 즉시 무효화 불가 |
| HttpOnly Cookie | XSS 방어 | 모바일 앱 연동 복잡 (MVP 외 범위) |
| 소셜 로그인만 | 마찰 최소화 | 이메일/비밀번호 로그인 없음 |
