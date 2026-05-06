# Backend Agent Instructions

`backend/` 하위 작업 진입점. 루트 `AGENTS.md` 도 함께 적용.

## Rules

@.claude/rules/conventions.md
@.claude/rules/testing.md

## 기술 스택

- Java 21
- Spring Boot 3.x
- Gradle (Kotlin DSL)
- Spring Data JPA
- MySQL 8.0 (local / dev / prod 동일)
- Flyway
- Spring Security + JWT
- OpenAI SDK + Anthropic SDK (`ResilientAiClient` 진입점)
- JUnit 5 + Mockito + AssertJ + ArchUnit + REST Assured + **Testcontainers (mysql)**

## 작업 진입 순서

1. 본 `backend/AGENTS.md`
2. `backend/.claude/rules/conventions.md` — 패키지 구조 / 네이밍 / 계층 / DTO / 에러 / 트랜잭션 / Lombok / 로깅 (`@import` 자동 로드)
3. `backend/.claude/rules/testing.md` — 테스트 정책 (`@import` 자동 로드)
4. 루트 `AGENTS.md` — `@import` 룰 (commit / comments / security / plan-mode)

## 빠른 명령

```bash
./gradlew bootRun --args='--spring.profiles.active=local'      # MySQL local
./gradlew test                                                 # 전체
./gradlew test --tests "InterviewServiceTest"                  # 단일 클래스
./gradlew test --tests "com.rehearse.api.domain.interview.*"   # 도메인
./gradlew compileJava                                          # 컴파일만
```

## 핵심 룰

- **AI 호출 = `ResilientAiClient` 단일 진입점** — OpenAI / Claude SDK 도메인 / 애플리케이션 서비스 직접 호출 금지.
- **AI 모델 ID = `application-*.yml`** — Java 코드 하드코딩 금지.
- **Spec 없는 수정 금지** — `backend/src/` 변경 전 `docs/plans/{N}-{slug}/tech-spec.md` 존재 확인. 부재 시 설계부터 (구현 거부). `product-spec.md` 부재 시 사용자에게 기획 요청. 자세한 워크플로우는 루트 `AGENTS.md` "Spec-Driven Work" + `docs/plans/AGENTS.md` 참조.

> 패키지 / 네이밍 / 계층 / DTO / 트랜잭션 / Flyway / Lombok / 로깅 등 코드 컨벤션 = `backend/.claude/rules/conventions.md` (`@import`).

## 도메인 맵 (`com.rehearse.api`)

각 도메인 표준 구조: `controller / service / repository / entity / dto / exception` (도메인별 일부 생략).

### `domain/`

| 패키지 | 역할 | 핵심 |
|--------|------|------|
| `auth/` | 인증 / OAuth 콜백 | controller + dto only (JWT 발급은 `global/security/`) |
| `user/` | 사용자 프로필 | entity / repository / service (controller 없음, 다른 도메인이 사용) |
| `resume/` | 이력서 도메인 + 트랙 | service 중심 (Resume Project / Playground Context Engineering) |
| `interview/` | 인터뷰 세션 (핵심 도메인) | `Interview`, `InterviewRuntimeState`, `IntentType`, FollowUp 서비스군. `intent/` 하위 분기 분류기 |
| `question/` | 질문 본체 | service 만 (controller 없음 — questionset 경유) |
| `questionset/` | 질문 세트 / 프리셋 | 표준 구조 |
| `feedback/` | AI 피드백 + 루브릭/점수 | `rubric/`, `score/` 하위 도메인 분리 (event 발행 포함) |
| `reviewbookmark/` | 피드백 북마크 | 표준 구조 |
| `servicefeedback/` | 서비스 자체 피드백 (사용자 → 운영진) | 표준 구조 |
| `file/` | 업로드 파일 메타 / S3 키 관리 | 표준 구조 |
| `admin/` | 관리자 엔드포인트 | controller + dto + exception only |

### `infra/`

| 패키지 | 역할 |
|--------|------|
| `ai/` | **`ResilientAiClient` 단일 진입점** (`OpenAiClient` primary + `ClaudeApiClient` fallback). `WhisperService` (STT). `prompt/`, `context/`, `persona/`, `metrics/` 보조. `MockAiClient` 테스트용 |
| `aws/` | S3, EventBridge 연동 |
| `google/` | Google OAuth |
| `tts/` | Text-to-Speech |

### `global/`

| 패키지 | 역할 |
|--------|------|
| `config/` | Spring 빈 / 프로필별 설정 |
| `security/` | JWT 발급 / 검증, Spring Security 필터 |
| `exception/` | `@RestControllerAdvice` 전역 예외 핸들러 |
| `common/` | 공용 enum / 베이스 엔티티 |
| `util/` | 순수 유틸 |

## 테스트 정책

`backend/.claude/rules/testing.md` (위 `@import` 자동 로드) 단일 소스. 6 카테고리 + Smoke + ArchUnit, Support 추상 클래스, 피라미드, Mock 정책, Truncate cleanup 등.

## Lambda 와의 관계

EventBridge 디커플링. Backend 는 Lambda 직접 invoke 안 함. S3 이벤트 → EventBridge → Lambda. Lambda 영역 룰은 `lambda/` 별도.

## 자주 재확인할 컨벤션

- 패키지 배치 / 계층 의존성 → `backend/.claude/rules/conventions.md`
- DTO 네이밍 / 매핑 → `backend/.claude/rules/conventions.md`
- Service / Transaction 패턴 → `backend/.claude/rules/conventions.md`
- Repository 쿼리 패턴 → `backend/.claude/rules/conventions.md`
- 테스트 정책 → `backend/.claude/rules/testing.md`
