# Security Rule

## AI 행동 원칙 (Blocking)

코드 작성/리뷰/리팩토링 중 OWASP Top 10 항목 위반 또는 위반 가능성을 발견하면 **즉시 작업 중단**하고 사용자에게 보고 + 진행 여부 되묻는다. 자율 판단으로 우회·완화·무시 금지.

보고 형식:
```
[보안 경고] OWASP A0X — <항목명>
위치: <파일:라인>
사유: <위반 내용>
권장: <수정안>
계속 진행할까요?
```

## OWASP Top 10 (2021)

작업 시 항상 아래 10개 항목 위반 여부 self-check.

### A01: Broken Access Control
- 권한 우회 / 다른 사용자 리소스 접근.
- 본인 소유 리소스만 접근하는지 controller/service 단에서 `userId` 검증 강제.
- 관리자 전용 엔드포인트는 role 체크 명시.

### A02: Cryptographic Failures
- 비밀번호 평문 저장 금지. BCrypt/Argon2 등 단방향 해시.
- HTTPS 강제. 민감 데이터 평문 전송 금지.
- 약한 알고리즘 (MD5, SHA1, DES) 금지.

### A03: Injection
- SQL: JPA `@Query` 네이티브 작성 시 파라미터 바인딩 (`?1`, `:name`) 강제. 문자열 concat 금지.
- Command: `Runtime.exec`, `ProcessBuilder` 사용자 입력 직접 전달 금지.
- XSS: FE `dangerouslySetInnerHTML` 금지 (sanitize 필수).
- LDAP/XPath/NoSQL 인젝션 동일 원칙.

### A04: Insecure Design
- 설계 단계에서 위협 모델링 (인증/인가/데이터 흐름).
- Rate limiting / CAPTCHA 누락된 인증 엔드포인트 금지.

### A05: Security Misconfiguration
- 기본 비밀번호 / 기본 키 사용 금지.
- Production 에러 응답에 stack trace 노출 금지.
- CORS `*` 금지 — allowlist 명시.
- 불필요 HTTP 헤더 (`Server`, `X-Powered-By`) 제거.

### A06: Vulnerable and Outdated Components
- `npm audit`, `gradle dependencyCheck` 정기 확인.
- CVE 발견 시 즉시 업데이트 또는 mitigation.
- 사용 안 하는 라이브러리 제거.

### A07: Identification and Authentication Failures
- JWT 만료 / refresh 토큰 분리.
- 세션 고정 (session fixation) 방지 — 로그인 시 세션 ID 재발급.
- 비밀번호 정책 (길이/복잡도) 명시.
- 무차별 대입 방어 (lockout / rate limit).

### A08: Software and Data Integrity Failures
- CI/CD 파이프라인 변조 방지 (signed commits, branch protection).
- 외부 의존성 무결성 검증 (`integrity` lockfile, checksum).
- 직렬화 역직렬화 시 신뢰 가능 소스만.

### A09: Security Logging and Monitoring Failures
- 인증 실패 / 권한 거부 / 입력 검증 실패 로그 기록.
- 로그에 민감 정보 (비밀번호, 토큰, 카드번호) 기록 금지.
- 의심 활동 탐지 / 알림 메커니즘 마련.

### A10: Server-Side Request Forgery (SSRF)
- 사용자 입력 URL 을 서버가 호출 시 도메인 allowlist 강제.
- 내부 IP (10.x, 172.16-31.x, 192.168.x, 127.x, 169.254.x) 차단.
- Lambda / Backend 외부 fetch 시 동일 원칙.

## Secrets

- API 키 / 시크릿을 코드 / 로그 / 문서 / FE 번들에 노출 금지.
- `.env`, `.claude.local.md`, `application-local.yml` 등 민감 파일 gitignored 유지. tracked 파일에 복사 금지.
- secret 누출 발견 시 즉시 revoke + rotate.
- AWS 인프라 상태는 `aws` CLI 직접 조회. stale 문서 추측 금지.

## 입력 검증 위치

- **Backend**: controller 경계 (`@Valid`, Bean Validation) + service 도메인 규칙.
- **Frontend**: 사용자 입력은 UX 용 1차 검증만. 신뢰 기준은 backend.
- **Lambda**: 이벤트 페이로드 schema 검증 (S3 key, EventBridge detail).
