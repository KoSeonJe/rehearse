# Product Spec — LLM 응답 DTO 위치/네이밍/검증 통일

> **작성자**: 사용자 (PM 페르소나 초안)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → backend agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태:
  - LLM 응답을 받아 비즈 로직에 전달하는 매핑 클래스 10종 존재.
  - 위치 4가지 혼재 — `domain/interview/entity/`, `domain/feedback/rubric/entity/`, `domain/feedback/session/dto/`, `infra/ai/context/compaction/`, `infra/ai/dto/`.
  - 네이밍 4종 혼재 — `*Result`, `*Payload`, `Extracted*`, `Generated*`.
  - 매핑 시점 검증 적용 = 1/10 (`AnswerAnalysis` 만).
- 발생 증상:
  - LLM 이 필수 필드 누락 / 범위 위반 응답 반환 시 매핑은 통과. 비즈 로직 깊숙한 곳 (채점 / 인터뷰 분기 / 피드백 합성) 에서 NPE 또는 잘못된 분기로 표면화.
  - 운영 로그 stack trace 가 도메인 내부 가리킴 → root cause 가 LLM 응답 결함이라는 사실 추적 비용 큼.
  - 신규 LLM 응답 타입 추가 시 위치 / 네이밍 결정 자유도 = 부채 가속.
- 사용자·운영 인지 채널:
  - 매핑 검증 부재로 인한 도메인 내부 결함은 사용자 화면 잘못된 결과 / 5xx 로 노출됨 (단, 빈도 / 영향 정량 측정값 현재 없음).
  - 컨벤션 차원 부채는 신규 도메인 작업 시 PR 리뷰에서 반복 지적.

## 왜 해야 하는가 (Why)

- 사용자 임팩트:
  - 잘못된 LLM 응답이 채점 / 인터뷰 진행 / 피드백 결과에 silent 영향 줄 위험. 매핑 시점 거절 + 재시도가 정상 경로.
- 운영 / 시스템 임팩트:
  - 결함 root cause 추적 시간 단축. "LLM 응답 결함 vs 도메인 버그" 구분이 stack trace 첫 줄에서 가능.
  - 신규 LLM 응답 타입 추가 시 위치 / 네이밍 / 검증 룰 결정 자유도 0 → 부채 누적 멈춤.
- 외부 압력:
  - 도메인 패키지에 LLM 응답 매핑 클래스 동거 = 도메인-인프라 경계 흐림. 컨벤션 (`backend/.claude/rules/conventions.md` 계층 책임) 위반.

## 해결 방향 (Approach)

- 핵심 접근:
  - LLM 응답 매핑은 인프라 계층 단일 위치로 이동.
  - 매핑 시점에 필수 필드 / 범위 위반을 즉시 거절. 거절 응답은 기존 LLM 재시도 정책에서 흡수 (정책 자체 변경 X).
  - 도메인 로직 필요 시 매핑 DTO 에서 도메인 객체로 변환한 뒤 비즈 로직 진입. 도메인 entity 가 LLM 응답으로 위장하는 패턴 종결.
- 대안 비교:
  - "위치만 통일 / 네이밍·검증 미적용" — 검증 격차 잔존, 부채 미해결. 채택 X.
  - "검증만 적용 / 위치·네이밍 유지" — root cause 추적 쉬워지나, 도메인-인프라 경계 흐림 잔존. 채택 X.
  - "위치 + 네이밍 + 검증 동시" — 1회 정리로 향후 신규 응답 타입 자유도 0. 채택.
- 단계 분리:
  - 작업 분량 (10 클래스 + 4 도메인 호출부) → PR 분할 권장. 방향 가이드 = infra 위치 5종 (검증 도입 + 리네이밍) 선행 → 도메인 위치 5종 (이동 + 리네이밍 + 검증 + 변환 메서드 도입) 후행. 최종 분할 단위는 tech-spec 결정.
  - 사유: 호출부 4도메인 동시 변경 시 리뷰 부담 / 회귀 위험 큼. 인프라 단독 변경은 호출부 import 만 영향 → 격리 용이.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java:18-39` — 매핑 시점 검증 모범 (필수 필드 / 범위 즉시 거절).
  - `backend/src/main/java/com/rehearse/api/infra/ai/dto/ExtractedResumeSkeleton.java` — DTO → 도메인 변환 메서드 (`toDomain`) 사례.
  - 호출부 다도메인: interview / resume / feedback/rubric / feedback/session — Issue 본문 매핑표 참조.
- 운영 로그 / 메트릭: 매핑 결함 정량 측정값 부재 — 본 작업이 가시성 자체를 도입하므로 사후 측정 가능해짐. 정량 근거 부재 = 추정. 본 작업은 컨벤션 부채 차원 정리로 진행 (refactor + P2 우선순위 부합).
- 사용자 발화 / 인접 plan:
  - Issue #462 본문 (이미 매핑 / 정책 / 비스코프 정의됨).
  - 인접 영역 plan: `docs/plans/460-domain-naming-cleanup/` (도메인 enum / 필드 정리 — 별도 작업, 의존 없음).

## Goal

- [ ] LLM 응답 매핑 위치 = `infra/ai/dto/` 단일 디렉토리 10/10 (신규 이동 5종 + 기존 위치 5종)
- [ ] LLM 응답 매핑 네이밍 = `Generated` 접두사 10/10 (신규 리네이밍 6종 + 기존 4종)
- [ ] LLM 응답 매핑 = 매핑 시점 필수 필드 / 범위 검증 적용 10/10 (현재 1/10, 신규 9종)
- [ ] 도메인 비즈 로직 코드 = LLM raw 매핑 직접 사용 0건 — 측정 범위 = `backend/src/main/java/com/rehearse/api/domain/**` import grep, `Generated*` 직접 import = 변환 호출부만 허용

## Non-Goals

이 작업이 **목표로 삼지 않는** 것 (가치 차원). 비스코프 (작업 차원) 와 구분.

- LLM 응답 품질 향상 — 사유: 본 작업은 매핑 안정성 / 가시성 정리. 응답 내용 자체 품질은 prompt / 모델 영역. (대응 비스코프: AI prompt 본문 변경)
- 응답 latency 개선 — 사유: 구조 정리 우선. 매핑 검증 비용 미세 증가 가능성 수용.
- 도메인 비즈 로직 리팩토링 — 사유: 호출부는 import / 타입만 변경. 내부 분기 / 책임 재배치는 별도 plan.

## 수용 기준 (Acceptance Criteria)

- [ ] 신규 LLM 응답이 필수 필드 누락 / 범위 위반 상태로 도착하면 매핑 단계에서 거절. 기존 LLM 재시도 정책으로 자동 보완 — 사용자 화면 / 응답 회귀 0건.
- [ ] LLM 응답 매핑 클래스 위치 = 단일 디렉토리. 신규 응답 타입 추가 시 위치 결정 자유도 0.
- [ ] LLM 응답 매핑 클래스 명 = `Generated` 접두사 일관. 새 타입 추가 시 네이밍 결정 자유도 0.
- [ ] 도메인 비즈 로직 (인터뷰 진행 / 채점 / 피드백 합성) 회귀 0건 — 기존 BE 테스트 스위트 (`./gradlew test`) 그대로 통과.
- [ ] 9개 신규 검증 적용 클래스 각각에 대해 필수 필드 누락 / 범위 위반 / 정상 케이스 단위 테스트 존재.
- [ ] 도메인-인프라 경계 ArchUnit 룰 위배 0건 — 도메인 패키지의 `Generated*` 의존이 변환 호출부 한정으로 유지.

## 비스코프 (Don't)

- AI prompt 본문 변경 — 사유: 본 작업은 응답 매핑 계층 한정. prompt 영역은 별도 plan.
- LLM 재시도 정책 (`parseOrRetry`) 변경 — 사유: 매핑 거절을 흡수하는 기존 동작 활용만. 정책 변경은 별도 Issue.
- AI Client (`OpenAiClient` / `ClaudeApiClient` / `ResilientAiClient`) 시그니처 변경 — 사유: 호출 경로 그대로. 시그니처 변경 시 영향 범위 본 작업 초과.
- LLM 모델 / 토큰 정책 변경 — 사유: 응답 매핑과 무관.
- 신규 LLM 응답 타입 추가 — 사유: 본 작업은 기존 10종 정리만. 신규 타입은 정리된 기준 위에서 별도 plan.

## 참고

- 관련 Issue: #462
- 관련 plan: `docs/plans/460-domain-naming-cleanup/` (인접 — 도메인 enum / 필드 네이밍 정리, 의존 없음)
- 외부 자료 / 디자인: 해당 없음
