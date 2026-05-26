# Product Spec — 정상 답변 turn 의 NOT_EVALUABLE 오판정 해소

> **작성자**: 사용자
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- **현재 상태**: 인터뷰 답변 turn 분석 직후 결과 화면에서 차원별 점수 또는 "평가 제외" 표기 노출. PR #527 머지로 차원별 NOT_EVALUABLE 상태 도입.
- **발생 증상**: 사용자가 수 백자 분량 음성 답변 입력 후 결과 화면에서 `기술 피드백` 영역 5개 차원 (후속 깊이 / 경험 구체성 / 사실 일관성 / 설명력 / 기술 깊이) 모두 "평가 제외 — 응답 길이 3자 이하" 표기. 사용자 직접 발견.
- **사용자·운영 인지 채널**: 사용자 본인 답변 후 결과 확인 시 발견. 운영 측 자동 알림 없음 (NOT_EVALUABLE 사유별 빈도 통계 미수집).

## 왜 해야 하는가 (Why)

- **사용자 임팩트**: 차원별 피드백 = 본 서비스 핵심 가치 제공 단위. 정상 답변에 "평가 제외" 표기 = 사용자 신뢰도 직접 훼손 + 핵심 가치 미전달.
- **운영 / 시스템 임팩트**: PR #527 (NOT_EVALUABLE 차원 상태 도입) 직후 발견된 regression 성격. 빈도 추적 부재로 노출 범위 미상.
- **외부 압력**: 신뢰도 회복 압력 + 인접 NOT_EVALUABLE 트리거 경로 (LLM 응답 기반 "관련 발언 없음", validator 위배) 도 동일 위험 보유 가능성 — 통합 점검 필요.

## 해결 방향 (Approach)

PM 수준 high-level 방향. 구현 디테일은 tech-spec.

- **핵심 접근**: 정상 답변 turn 이 잘못 차단되지 않도록 NOT_EVALUABLE 판정 경로 전체 (3개 트리거) 검토 + 강화. 판정 input 보강 또는 판정 기준 재설계 중 어느 쪽이 적절한지는 tech-spec 단계 결정.
- **대안 비교**:
  - (a) FE STT 정확도 개선 — Web Speech 한계 + 비용 큼. 본질 해결 안 됨.
  - (b) BE 판정 기준 보강 — 작고 즉시 가능. 채택.
- **단계 분리 불필요** — 3개 트리거 모두 동일 도메인 (RubricScorer 경로) 안. 한 번에 점검. 구현 디테일 (데이터 흐름 / 모델 확장 여부 포함) = tech-spec 위임.

## Evidence

- **트리거 1 (blank length)**: `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorer.java:47-53,69` — FE 송신 `userAnswer` strip 후 길이 ≤ 3 시 전 차원 NOT_EVALUABLE.
- **트리거 2 (LLM "관련 발언 없음")**: `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScoringAdapter.java:187-200` — LLM observation 이 "관련 발언 없음" 으로 시작 시 해당 차원 NOT_EVALUABLE.
- **트리거 3 (validator 위배)**: `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScorerResponseValidator.java:14-42` — score 범위 / observation 누락 / evidence 미일치 시 retry 또는 차단 경로.
- **FE 송신 origin**: `frontend/src/hooks/use-answer-flow.ts:106-115` — `getCurrentAnswerText()` = Web Speech `isFinal=true` transcript join. STT 실패 / 조각 누락 시 빈 문자열 가능.
- **동일 turn audio 분석**: `backend/src/main/java/com/rehearse/api/domain/interview/service/AudioTurnAnalyzer.java:41-56` — audio 본체 별도 분석 (`AnswerAnalysis` 산출). 현재 확인 범위에서 `AnswerAnalysis` 필드 = claims / dimensionGaps / weakestDimension / unstatedAssumptions / recommendedNextAction (transcript 텍스트 보존 여부 = tech-spec 단계 정밀 확인 필요, 추정 = 미보존).
- **사용자 직접 발화**: "현재 답변 내용이 채워져있음에도 불구하고 기술 피드백이 응답이 3자 이하라는 잘못된 분석을 하고 있어".
- **관련 PR**: #527 (4시간 전 머지) — NOT_EVALUABLE 차원 상태 도입.

## Goal

측정 가능한 결과.

- [ ] 정상 답변 turn (음성 또는 텍스트 충분) 입력 시 NOT_EVALUABLE 판정 0건 — e2e live test 통과로 확인.
- [ ] 정말 무응답 turn (오디오 무음 + 텍스트 임계 미만) 만 NOT_EVALUABLE 정상 적용 — e2e live test 통과로 확인.
- [ ] e2e live test (실 LLM 호출 포함) 신규 시나리오 통과 — 본 issue 해소 판정 단일 기준.

## Non-Goals

- **STT 자체 정확도 개선 아님**. Web Speech 한계 / 외부 의존. 본 작업은 BE 판정 측 보강 한정.
- **차원별 점수 알고리즘 / 점수 분포 변경 아님**. 정상 답변이 NOT_EVALUABLE 로 빠지는 경로만 해소. 점수 산출식 자체는 손대지 않음.
- **사용자 측 알림 / UX 변경 아님**. "평가 제외" 표기 자체 변경 X — 잘못 적용되는 조건만 수정.

## 수용 기준 (Acceptance Criteria)

- [ ] 30자 이상 음성 답변 + FE 송신 텍스트 빈 문자열 케이스 — 차원별 점수 정상 산출 (NOT_EVALUABLE 아님).
- [ ] 4자 이상 텍스트 입력 답변 — NOT_EVALUABLE blank 사유로 차단되지 않음.
- [ ] 정말 무응답 (오디오 무음 + 텍스트 임계 미만) — NOT_EVALUABLE blank 사유 정상 적용.
- [ ] LLM "관련 발언 없음" 트리거 경로 — 정상 답변 turn 에 잘못 적용 0건.
- [ ] validator 위배 트리거 경로 — 정상 답변 turn 에 잘못 적용 0건.
- [ ] e2e live test 신규 시나리오 5개 모두 통과:
  - (1) 긴 음성 답변 + FE 송신 텍스트 빈 문자열 — 차원별 점수 정상 산출
  - (2) 긴 음성 답변 + 정상 LLM 응답 — 차원별 점수 정상 산출
  - (3) 무음 + 텍스트 임계 미만 — NOT_EVALUABLE blank 사유 정상 적용
  - (4) 긴 답변 + LLM observation 정상 — "관련 발언 없음" 트리거 미작동
  - (5) 긴 답변 + validator 통과 LLM 응답 — validator 트리거 미작동

## 비스코프 (Don't)

- **BLANK_ANSWER_LENGTH_THRESHOLD 값 자체 튜닝 (3 → N)** — 본 문제 (잘못된 input 으로 판정) 와 별개 결정. 임계값 자체는 합리적.
- **차원별 점수 산출식 / Rubric 재정의** — 별도 Sprint (Interview Quality) scope.
- **NOT_EVALUABLE UI 표기 / 사용자 알림 메시지 개선** — 표기 자체 정상. 잘못 적용되는 조건만 수정.

## 참고

- 관련 Issue: #530
- 관련 PR: #527 (NOT_EVALUABLE 차원 상태 도입)
- 외부 자료 / 디자인: 없음
