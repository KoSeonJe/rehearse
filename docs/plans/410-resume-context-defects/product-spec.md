# Product Spec — Resume 4-layer 컨텍스트 결함 (P0 + P1)

> **작성자**: 사용자 (PM 페르소나, Claude 초안)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성
> **관련 Issue**: #410

---

## 문제 상황 (Problem)

이력서 면접 트랙 (Resume) 의 컨텍스트 엔지니어링 4-layer 아키텍처 + 조립 로직 + Resume 전용 핸들러에서 코드 audit 결과 결함 5건이 확인됨. 결함은 ① 사용자에게 5xx 응답으로 직접 노출되거나 ② LLM 응답 품질을 저하시키거나 ③ 운영 비용을 증가시킨다.

### 현재 상태

- L1 (FixedContextLayer), L2 (SessionStateLayer): 정상 작동
- L3 (DialogueHistoryLayer): 압축 비동기 / runtimeState 부재 시 옛 turn 컨텍스트가 prompt 에서 누락될 수 있음
- L4 (FocusLayer): fragment 가 callType별 cap 초과 시 `IllegalStateException` throw → 사용자 5xx
- 조립 (InterviewContextBuilder): 합산 토큰이 `maxContextTokens` 초과해도 WARN 로그만 남기고 그대로 LLM 호출
- 핸들러 (InterrogationModeHandler): chain state lock 안에서 LLM 호출 + DB persist 까지 수행 → 같은 인터뷰의 동시 turn 요청 시 lock contention

### 발생 증상

> 빈도 = 코드 audit 기반 추정. 운영 로그 정량 수치는 본 plan 시점 미수집.

| 결함 | 재현 시나리오 | 빈도 (추정) |
|------|--------------|------|
| **P0-1** L4 cap 초과 throw | Resume Interrogation 등 fragment 입력이 길어질 때 (긴 user answer / 누적 chain 정보) | 입력 길이 의존, 운영 시 발생 가능 |
| **P0-2** L4 미등록 callType throw | `FocusHints.EmptyHints` + `compaction_summarizer` 외 callType 진입 시 | 회귀 시 신규 callType 추가 누락 시 |
| **P1-1** L3 압축 race + null runtimeState | 압축 트리거 직후 다음 1-2턴, 또는 runtimeState cache miss | 긴 인터뷰에서 매번 |
| **P1-2** 전체 cap 초과 후 그대로 호출 | L1+L2+L3+L4 합산이 `maxContextTokens` 초과 | LLM 자동 truncation 시 응답 품질 저하 |
| **P1-3** Interrogation lock 점유 과다 | 같은 인터뷰의 동시 turn 요청 (예: 클라이언트 재시도) | 동시성 시나리오 발생 시 |

### 인지 채널

- 코드 audit (`/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/`)
- Issue #410 본문에서 일부 의심 (L3 async / L4 cap)
- audit 과정에서 추가 발견 (P1-2 전체 cap, P1-3 lock 범위)

## 왜 해야 하는가 (Why)

### 사용자 임팩트

- **P0**: L4 cap 초과 시 인터뷰 진행 도중 5xx → 화면 멈춤 → 세션 강제 중단. 이미 진행한 turn 손실 가능
- **P1-1**: 압축 직후 1-2턴 동안 LLM 이 옛 대화 맥락을 모르는 상태 → 같은 질문 재반복 / 일관성 없는 답변 평가

### 운영 / 시스템 임팩트

- **P1-2**: 토큰 cap 초과 호출이 비용 / 응답 품질에 그대로 반영되지만 운영자는 WARN 로그 외 원인 추적 불가
- **P1-3**: 같은 인터뷰 동시 turn 요청 시 lock 점유 시간이 LLM latency (수 초) + DB I/O 만큼 누적 → 후속 turn 응답 지연

### 외부 압력

- Issue #410 본문에 P0 라벨 부착됨
- Resume 트랙 = MVP 핵심 차별점. 인터뷰 중단 = 핵심 사용자 가치 손상

## 해결 방향 (Approach)

PM 수준 high-level 방향 (HOW = tech-spec 영역):

### 핵심 접근

1. **L4 cap 초과 graceful 처리** — fragment 가 cap 초과해도 사용자 응답 200 보장 (truncation 또는 우선순위 절단). 운영 로그로 cap 초과 식별 가능
2. **L4 미등록 callType graceful 처리** — 신규 callType 진입 시에도 5xx 대신 식별 가능한 운영 로그 + 안전 폴백
3. **L3 컨텍스트 누락 차단** — 압축 미완료 또는 runtimeState 부재 시에도 옛 turn 또는 그 대체 컨텍스트가 prompt 에 항상 포함
4. **전체 cap 초과 시 조치 정의** — 단순 WARN 이상의 운영자 액션 가능 상태 (메트릭 / 일부 layer 절단 / 차단 중 정책 결정 필요)
5. **Interrogation 동시성 보호 범위 축소** — chain state 변경 구간만 직렬화 보장, LLM 호출 / DB I/O 는 동시 진행 허용

### 단계 분리

P0 (L4 5xx 차단 2건) + P1 (3건) 5건을 단일 plan 으로 묶음. 모두 Resume 컨텍스트 도메인 1개 영역. PR 1-2 개 분할은 tech-spec 단계에서 결정.

### 비스코프된 PG handler 운영 로그 (P2-1)

별도 Issue 로 분리. 본 plan 에 미포함.

## Evidence

- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java:86-94` — cap 초과 시 `IllegalStateException` throw
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java:49-53` — 미등록 callType `IllegalStateException` throw
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java:51-66` — runtimeState null 분기에서 압축 / raw fallback 둘 다 미수행, 압축 in-flight 분기에서도 raw fallback 없음 (코드 audit 기반 추정. 운영 로그 상 분기 진입 빈도는 미검증)
- `backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java:47-50` — total > maxContextTokens 시 WARN 로그만, 그대로 진행
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:38-79` — `tracker.withLock(...)` 안에서 `promptBuilder.build()` (LLM 호출) + `questionPersister.persist()` (DB I/O) 모두 수행
- `backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java:65` — `// TODO when sync fallback added (deferred from Task 3)` 부채 흔적
- 메모리: `project_interview_quality.md` — Resume 트랙은 인터뷰 품질 sprint 핵심 영역

## Goal

- [ ] L4 cap 초과로 인한 Resume 트랙 5xx 발생 0건 (운영 메트릭 기준 1주 누적)
- [ ] L4 미등록 callType 진입 시 5xx 발생 0건 (운영 메트릭 기준)
- [ ] L3 압축 트리거 직후 다음 턴 prompt 에 옛 turn 또는 그 대체 컨텍스트 포함률 100%
- [ ] 전체 컨텍스트 토큰 cap 초과 호출이 운영자가 식별 가능한 메트릭 신호 (1개 이상) 로 노출되고, 명문화된 처리 정책에 따라 일관 처리됨 (정책 명문화 = tech-spec 결정)
- [ ] 같은 인터뷰의 동시 turn 요청 시, Interrogation handler 의 chain state lock 점유 시간이 LLM latency / DB I/O 와 무관 (lock 점유 = chain state 변경 구간만)

## Non-Goals

- LLM 응답 품질 자체 개선 — 본 plan 은 컨텍스트 정합성 / 안정성 보장에만 집중. 응답 품질 향상은 Goal 의 부산물
- 토큰 cap 값 자체 재조정 — cap 값 결정은 별도 튜닝 영역. 본 plan 은 cap 초과 시 동작 정의에만 집중
- 모드 전환 정책 자체 변경 — 4-condition 평가 / chain level 정책 그대로 유지
- 압축 알고리즘 (LLM summarizer) 변경 — 트리거 / 대기 / 폴백 메커니즘만 다룸

## 수용 기준 (Acceptance Criteria)

- [ ] L4 fragment 가 callType 별 cap 초과해도 사용자 응답 200 반환되며, 운영자가 운영 로그로 cap 초과 + 어떤 callType + 적용된 처리 (truncation / 우선순위 절단 등) 식별 가능
- [ ] L4 미등록 callType 진입 시에도 5xx 대신 운영 로그 + 안전 응답 반환되어, 운영자가 미등록 callType 식별 가능
- [ ] L3 압축 트리거 직후 다음 turn 의 prompt 에 옛 turn 또는 그 대체 컨텍스트 (요약 / raw / fallback 중 어느 형태든) 누락 없이 포함됨 — 회귀 테스트로 검증
- [ ] 전체 컨텍스트 토큰 cap 초과 호출이 운영자가 식별 가능한 메트릭 신호로 노출되며, 명문화된 처리 정책 (정책 명문화 = tech-spec 단계 결정) 에 따라 일관되게 처리됨
- [ ] 같은 인터뷰의 동시 turn 시나리오에서 Interrogation handler 가 LLM 호출 / DB persist 동안 다른 동시 turn 을 차단하지 않음 — 회귀 테스트로 검증
- [ ] 회귀: 기존 정상 케이스 (L1/L2 / 압축 미트리거 / cap 미초과 / 단일 turn) 동작 변경 없음

## 비스코프 (Don't)

- **PG 전환 결정 운영 로그 (P2-1)** — 별도 Issue 분리 (회고 가시성. 사용자 직접 영향 없음)
- **ChainStateTracker.levelStay 정책 가시성** — 한계 초과 강제 전환 로그 이미 존재. 추가 가시성 = 별도 Issue
- **L1 / L2 코드 변경** — audit 결과 결함 미발견. 회귀 범위에만 포함
- **WrapUpModeHandler 검증** — Issue 미언급, audit 결과 결함 미발견
- **ContextEngineeringProperties 설정값 재조정** — cap 값 자체 튜닝 별도

## 참고

- 관련 Issue: #410
- 관련 plan: `docs/plans/404-interview-domain-findings/` (interview 도메인 발견 통합)
- TODO 부채: `DialogueCompactor.java:65` — sync fallback 미구현 인지된 상태
- 메모리: `project_interview_quality.md`, `project_ai_stack.md`
