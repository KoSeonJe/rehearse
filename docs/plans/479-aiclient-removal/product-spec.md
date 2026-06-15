# Product Spec — AiClient God Interface 제거 / 도메인 port 분리

> **작성자**: 사용자 (트리아지 + 초안 = Senior PM)
> **답하는 질문**: 왜 / 무엇 / 수용기준
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

> **Issue #479 정합성**: 본 spec 은 Issue #479 body 의 Option A 안 (`AiClient` 에 type-specific 메서드 추가) 을 **폐기**하고 도메인별 port 분리로 전환한다. 사용자 승인 직후 Issue body 를 본 spec 방향으로 갱신한다 (DoD 재정의 = `docs-manager` 위임).

- 현재 상태:
  - `infra/ai/AiClient` 가 5개 메서드 (`chat`, `chatWithAudio`, `generateQuestions`, `generateFollowUpQuestion`, `generateFollowUpWithAudio`) 를 가진 거대 단일 인터페이스. 7개 도메인 호출부가 의존.
  - 호출부 4곳 (`SessionFeedbackSynthesizer:23`, `AnswerAnalyzer:63`, `FollowUpQuestionWriter:63`, `ResumeTrackInitiator:87`) 이 도메인 service 안에서 `ChatRequest` 를 직접 빌드 → infra DTO 가 도메인 노출.
  - 어댑터 3종 (`adapter/QuestionGenerationAdapter`, `adapter/FollowUpGenerationAdapter`, `adapter/RubricScoringAdapter`) 이 `AiClient` 를 인자로 받아 `chat()` 호출 → 이중 추상화.
  - `RubricScorer:54` 는 `adapter.adapt(aiClient, ...)` 형태로 `AiClient` 를 외부에서 주입 받음.
- 발생 증상 (정적 결함):
  - 컨벤션 (`backend/.claude/rules/conventions.md` Port 룰) 가 명시한 "거대 단일 인터페이스 (`AiClient` 류) X. 책임 단위 분리." 룰 자기 위배.
  - 신규 LLM 호출 추가 시 `AiClient` 인터페이스 + provider 구현체 2종 (`OpenAiClient`, `ClaudeApiClient`) + `ResilientAiClient` + `MockAiClient` 동시 수정 (시그니처 4곳 전파). 변경 비용 큼.
  - 도메인 테스트에서 `AiClient` 를 mock 할 때 도메인 외 책임 (audio chat / question generation) 까지 stub 필요 → 도메인 단위 격리 깨짐.
- 인지 채널: PR #515 (`OpenAiResumeSkeletonExtractor` 분리) 작업 중 발견. PR #515 리뷰 포인트 2 "Resume skeleton 추출기가 공통 AI 클라이언트를 우회하는 구조" 가 사실 정답 패턴이었음 — 나머지 호출부에도 적용 필요.

## 왜 해야 하는가 (Why)

- 사용자 임팩트: 직접 없음 (정적 리팩토링). 단, 향후 도메인별 LLM 모델 변경 / 프롬프트 튜닝 / fallback 정책 차등 적용 시 안전성 ↑ → 품질 개선 작업 속도 ↑.
- 운영 / 시스템 임팩트:
  - 도메인 단위 mock / 통합 테스트 격리도 ↑ → 회귀 식별 빠름.
  - LLM 호출 단위로 metric / log / retry / fallback 정책 차등 적용 가능 (현재는 일률).
  - PR #515 패턴이 일관적으로 적용되어 신규 LLM 호출 추가 비용 감소.
- 외부 압력: 컨벤션 anti-pattern 자기 명시 + PR #515 리뷰 포인트 표면화. 누적 부채.

## 해결 방향 (Approach)

핵심 접근: PR #515 `ResumeSkeletonExtractor` 패턴 (도메인 port + infra adapter + 단일 책임 HTTP client) 을 LLM 호출 7곳에 일관 적용. `AiClient` 인터페이스 + 어댑터 3종 + Resilient/Abstract/Mock 구현체 일괄 제거. 호출별 fallback 정책 (OpenAI primary → Claude fallback) 은 도메인 분리 후에도 유지 — 구조는 tech-spec 결정.

### Fallback 보존 대상 호출 목록

도메인 port 분리 후에도 OpenAI → Claude fallback 동작이 유지되어야 하는 호출 6개:

1. 표준 질문 생성 (`StandardQuestionProvider` 경로)
2. 이력서 질문 생성 (`ResumeTrackInitiator` 경로)
3. 답변 분석 (`AnswerAnalyzer` 경로)
4. 팔로업 질문 생성 (`FollowUpQuestionWriter` 경로)
5. 루브릭 채점 (`RubricScorer` 경로)
6. 세션 피드백 합성 (`SessionFeedbackSynthesizer` 경로)

별도 처리:
- 오디오 turn 분석 (`AudioTurnAnalyzer` 경로) — OpenAI audio chat 실패 시 STT + text-only fallback (기존 흐름). Claude fallback 비대상.

대안 비교:
- (기각) Option A — Issue #479 원안. `AiClient` 에 type-specific 메서드 추가 → 인터페이스 비대화. 본질 해결 X.
- (기각) Phase 다단계 분리 — 일관성 깨진 중간 상태 길어짐. 사용자 결정: 한 PR 통합.
- (기각) Fallback 폐기 — OpenAI 단독 시 운영 위험. 도메인 분리 후에도 유지.

단계 분리: 본 작업은 한 PR. commit 단위 분리 (호출부별 port 분리 → 인터페이스 제거 마지막) 는 구현 agent 가 tech-spec 단계에서 자율 결정.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/infra/ai/AiClient.java:14-25` — 5개 메서드 God Interface.
  - `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java:67-151` — `chat()` / `chatWithAudio()` fallback 일률 적용.
  - `backend/src/main/java/com/rehearse/api/infra/ai/adapter/QuestionGenerationAdapter.java:22-42` — `AiClient` 받아 `chat()` 호출 → 위장 추상화.
  - `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScoringAdapter.java:40-77` — 동일 패턴.
  - `backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/SessionFeedbackSynthesizer.java:23,30` — 도메인 service 가 `aiClient.chat()` 직접 호출.
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java:55-65` — `ChatRequest.builder()` 도메인 노출.
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpQuestionWriter.java:55-66` — 동일.
  - `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java:80-88` — 동일.
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/AudioTurnAnalyzer.java:65-77` — `chatWithAudio` 강결합.
  - `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorer.java:54-55` — `adapter.adapt(aiClient, ...)` 이중 추상화.
- 컨벤션 근거:
  - `backend/.claude/rules/conventions.md` "Port 인터페이스 — 책임 단위 명사. 거대 단일 인터페이스 (`AiClient` 류) X. 책임 단위 분리." (anti-pattern 자기 명시).
- 모범 사례:
  - PR #515 (`OpenAiResumeSkeletonExtractor`, `OpenAiResumeExtractorClient`, `MockResumeSkeletonExtractor`) — port / adapter / client / config / mock 분리 패턴 검증 완료.
- 운영 단서: 본 작업은 동작 변경 없는 리팩토링. 검증은 기존 단위 / 통합 / E2E 테스트 통과로 흡수.
- 정량 근거: 호출부 7개 / 어댑터 3종 수치는 `grep -rln "aiClient\." backend/src/main/java/com/rehearse/api/domain` 및 `find backend/src/main/java/com/rehearse/api/infra/ai/adapter -name "*Adapter.java"` 기준. tech-spec 단계에서 구현 agent 가 재확인.

## Goal

- [ ] `backend/src/main/java/com/rehearse/api` 하위에 `AiClient` 인터페이스 및 본 인터페이스를 구현하는 모든 클래스가 존재하지 않는다 (`grep -rn "AiClient" backend/src/main/java/com/rehearse/api` 결과 0).
- [ ] 도메인 패키지 (`backend/src/main/java/com/rehearse/api/domain`) 에서 `ChatRequest` / `ChatResponse` / `ResponseFormat` import 0건.
- [ ] `backend/src/main/java/com/rehearse/api/infra/ai/adapter/` 아래 `*GenerationAdapter`, `*ScoringAdapter` 파일이 존재하지 않는다 (PR #515 의 `OpenAiResumeSkeletonExtractor` 패턴 adapter 만 잔존).
- [ ] OpenAI primary → Claude fallback 동작이 "해결 방향 §Fallback 보존 대상 호출 목록" 의 6개 호출에서 보존된다.
- [ ] 기존 단위 / 통합 / E2E 테스트 100% 통과 (`./gradlew test`).

## Non-Goals

- `ChatMessage`, `ResponseFormat`, `JsonSchemaSpec`, `CachePolicy`, `AiResponseParser`, `AiCallMetrics`, `SchemaExampleRegistry`, `InterviewContextBuilder` 자체 제거 — 사유: 공통 인프라 자산. 본 작업은 호출부 추상화 정리만 목표.
- LLM 모델 ID / 프롬프트 / 응답 schema 변경 — 사유: 동작 변경 0 가 본 작업 안전성 기준.
- audio chat fallback 흐름 (OpenAI audio chat 실패 → STT + text-only fallback) 변경 — 사유: 호출 1곳 전용 도메인 특화. 분리 후에도 의미 동일 유지.
- 공통 fallback helper 추출 — 사유: 도메인별 wrapper 패턴 우선. 단일 helper 도입은 또 다른 추상화 — 본 작업 본질 (추상화 제거) 과 충돌. 패턴 안정화 후 별도 검토.

## 수용 기준 (Acceptance Criteria)

- [ ] **인터페이스 제거 가시화**: 운영자가 `grep -rn "AiClient" backend/src/main/java` 실행 시 결과 0 (인터페이스 / 구현체 / 의존 주입 모두).
- [ ] **도메인 격리 가시화**: `grep -rn "ChatRequest\|ChatResponse" backend/src/main/java/com/rehearse/api/domain` 결과 0.
- [ ] **호출별 fallback 동작 유지**: OpenAI 인증 키만 활성화한 환경에서 표준 질문 생성 / 이력서 질문 생성 / 답변 분석 / 팔로업 생성 / 루브릭 채점 / 세션 피드백 합성 6개 흐름이 정상 동작 (응답 반환). Claude 키만 활성화한 환경에서도 동일 6개 흐름 정상 동작 (audio chat 제외).
- [ ] **장애 시 자동 전환**: E2E 시나리오에서 OpenAI 5xx 시뮬레이션 → Claude 응답 폴백이 fallback 보존 대상 6개 호출별로 검증 (호출 단위 분리 → 단일 호출 회귀 식별 가능).
- [ ] **Mock 환경 정상**: 두 provider API key 모두 부재 시 (mock 시나리오) 인터뷰 생성 / 질문 생성 / 면접 진행 / 피드백 생성 흐름이 mock 응답으로 정상 종료.
- [ ] **테스트 전수 통과**: `./gradlew test` 통과. 아키텍처 테스트 (`ResumeArchitectureTest` 등 동등) 가 도메인 → infra 단방향 의존 룰 위배 0.

## 비스코프 (Don't)

- **LLM provider 일원화 (Claude 또는 OpenAI 단일)** — 사유: fallback 안전성 유지가 본 작업 전제. 별도 의사결정 필요 시 신규 plan.
- **`infra/ai/dto` 패키지 전수 재배치** — 사유: DTO 위치 정리는 별도 plan (#462 인접 작업 참조).
- **운영 메트릭 라벨 재설계** — 사유: 현 라벨 (callType 기반) 그대로 유지. 분리 후 라벨이 도메인명으로 자연스럽게 정리됨은 부산물.
- **테스트 카테고리 재분류** — 사유: testing.md 카테고리는 그대로. 단, mock 대상이 `AiClient` 에서 도메인 port 로 자연 이동.

## 참고

- 관련 Issue: #479 (본 spec 이 Issue body 의 Option A 안을 대체. Issue body 갱신 필요)
- 관련 PR: #515 (분리 패턴 모범)
- 관련 plan: `docs/plans/462-ai-response-dto-cleanup/` (DTO 통일 선행 작업)
- 컨벤션 근거: `backend/.claude/rules/conventions.md` Port 룰
