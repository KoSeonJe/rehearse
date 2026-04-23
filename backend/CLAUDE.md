# Backend — Rehearse API

> 이 파일은 `backend/` 하위 파일 작업 시 자동 로드된다. 루트 `CLAUDE.md`(프로젝트 전체 맥락)와 함께 적용.

## Stack

- Java 21 + Spring Boot 3.x / Gradle (Kotlin DSL) / Spring Data JPA
- DB: MySQL 8.0 (prod) / H2 (dev)
- AI 이중화: OpenAI **GPT-4o-mini** primary + **Claude Sonnet/Haiku** fallback — `ResilientAiClient` 단일 진입점, 프롬프트 빌더 공용. 모델 ID는 `application-*.yml` 관리 (코드 하드코딩 금지).

## 작업 전 필독

코드 수정 전 아래 문서를 Read로 확인한다 (이 파일은 요약 + 엔트리 포인트 역할).

- `backend/CONVENTIONS.md` — 패키지 구조, 계층 규칙, DTO, 에러 처리
- `backend/CODING_GUIDE.md` — Entity/Service/Repository 패턴, 클린코드
- `backend/TEST_STRATEGY.md` — 테스트 대상 판단, Mock 정책
- `.claude/rules/testing_rule.md` (루트) — 횡단 테스트 원칙 (Classist TDD, 경계에서만 Mock)

## 핵심 규칙

- **Entity 직접 반환 금지** — 모든 응답은 Response DTO로 변환
- **`@Transactional(readOnly = true)` 기본**, 쓰기 메서드에서만 `@Transactional`
- **DB 스키마 변경은 Flyway 마이그레이션** — 수동 DDL 금지
- **주석 ZERO 기본** — WHY가 비자명할 때만 1–2줄
- **AI 호출은 `ResilientAiClient` 경유** — OpenAI/Claude SDK 직접 호출 금지 (이중화·재시도·모니터링 공통화)
- **Spec 없는 수정 금지** — `backend/src/` 변경 전 `.omc/plans/` 또는 `docs/plans/`에 spec 존재 여부 확인

## 테스트

```bash
./gradlew test                                          # 전체
./gradlew test --tests "InterviewServiceTest"           # 단일 클래스
./gradlew test --tests "com.rehearse.api.domain.interview.*"  # 도메인별
```

**BE 테스트 정책 우선순위**: `backend/TEST_STRATEGY.md` > `.claude/rules/testing_rule.md`. 두 문서가 충돌할 경우 **TEST_STRATEGY.md 준수**.

현재 정착된 실전 패턴 (2026-04 기준, 테스트 파일 84개 실측):
- **Mockito 기반 Unit 테스트가 주력** — `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks` 조합 (157 occurrences)
- **Repository / Finder는 Mock 허용** — JPA interface를 Mockito로 mock하는 전통적 Mockist 스타일로 프로젝트 정착
- **Controller slice: `@WebMvcTest`** (10개 파일), **Repository slice: `@DataJpaTest`** (드물게, 커스텀 쿼리 검증 시)
- **Testcontainers 미사용** — real DB 테스트는 H2(dev)로 충분, 전환 계획 없음
- **Entity/VO/DTO는 Mock 금지** — 실제 객체로 생성 (`TestFixtures` 팩토리 활용)

공통 원칙 (testing_rule.md에서 유효한 부분):
- 행위(behavior) 테스트 — `verify(...)` / `toHaveBeenCalledWith` 위주의 구현 추적 금지, 반환값/관찰 가능한 상태 검증
- 도메인 로직은 Entity 메서드로 추출해 Mock 없는 순수 단위 테스트로 검증
- Flaky 테스트 24시간 내 quarantine, 루트 원인 수정 (sleep/retry loop 금지)
- LLM/timestamp 등 비결정적 출력 snapshot 금지

## Lambda와의 관계

`lambda/` (Python)와는 **EventBridge로 완전 디커플링**. Backend는 Lambda를 직접 invoke하지 않으며, S3 이벤트가 EventBridge를 거쳐 Lambda를 트리거한다. Lambda 영역 규칙은 `lambda/CLAUDE.md` 참조.

## 에이전트 호출 시

- BE 구현: `backend` / `backend-architect`
- 디버깅: `debugger`
- 리뷰: `architect-reviewer` (레이어링/SOLID), `code-reviewer` (보안/성능/기술부채)
- 테스트: `test-engineer`, `qa`

위 에이전트들은 `backend/` 파일을 건드리면 이 `CLAUDE.md`를 자동 로드한다. 호출 프롬프트에서 "Read로 컨벤션 확인" 강제 문구는 이제 불필요.
