# Product Spec — IntentClassifier 전면 제거 (면접 진행 차단 P0)

> **작성자**: 사용자 (PM 페르소나 초안 — Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- **현재 정상 흐름**: 사용자가 면접 시작 → 답변 입력 → 면접관이 다음 질문 / 다른 모드 전환 → 결과가 Question / question_score / rubric 으로 적재 → 분석 / 피드백 파이프라인 활용.
- **발생 증상** (Resume 트랙 시작 직후 자기소개성 답변 케이스에서 재현):
  1. 면접관이 동일 질문을 다른 표현으로 재발화 (꼬리질문 X). 사용자 다시 답변 → 또 같은 패턴 반복.
  2. 다음 질문 / 다음 모드 전환 자체가 발생 안 함.
  3. Question / question_score / rubric 적재 누락 → 면접 후 피드백 / 리포트 생성 불가.
- **영향 트랙**: Resume (text 경로), Standard (audio STT fallback) 양쪽.
- **인지 채널**: 개발자 / 팀원 도그푸딩 도중 발견 — 면접 시도 시 진행 자체 차단 확인.

## 왜 해야 하는가 (Why)

- **사용자 임팩트**: 핵심 기능 (모의면접 진행) 자체 차단. 서비스 가치 0. 사용자가 면접 시작 → 답변 → 진행 흐름 어디에서도 회복 불가.
- **운영 / 시스템 임팩트**: Question / question_score / rubric 적재 누락으로 분석 / 피드백 / 리포트 / 재학습 데이터 모두 결손. 면접 후 가치 파이프라인 전체 무효화.
- **외부 압력**: P0 결함. 사용자에게 노출되면 즉시 신뢰도 손상. 출시 전 차단 필수.

## 해결 방향 (Approach)

답변 의도 사전 분류 단계가 over-engineering 으로 판단됨. **별도 분류 stage 자체를 제거**한다.

- **핵심 접근**: 모든 사용자 발화 = 답변으로 단일화 처리. "모르겠다" / 주제 무관 발화 같은 엣지 케이스는 **면접관 페르소나가 직전 답변 컨텍스트와 함께 자연스럽게 흡수** (다른 각도 제시 / 다음 항목 전환 / 자연스러운 redirect). 면접관의 흡수 가능성은 **가설** — 본 작업 수용 기준 단계의 수동 검수로 검증.
- **대안 비교**:
  - (A) 분류 정밀도 보강 — 채택 X. 회귀 위험 + 분류 단계 자체가 실 사용 가치 부족.
  - (B) 분류 단계 제거 + 면접관이 엣지 흡수 (채택) — 단일화 / 회귀 면적 최소.
- **단계 분리**: 단일 phase + 단일 머지 단위. 분류 단계 제거와 면접관의 엣지 흡수 능력 보강을 분할 머지 시 (분류만 먼저 제거) 엣지 케이스 무처리 회귀 발생 — 함께 적용되어야 함. (분할 PR 운영 필요 시 tech-spec 단계에서 안전장치 포함 위임.)

## Evidence

- 면접 진행 차단 결함 직접 재현: Resume 트랙 면접 시작 → 자기소개성 답변 → 면접관이 같은 질문 재표현, 다음 질문 / 모드 전환 없음, Question 적재 누락 (도그푸딩 확인).
- 코드 추적 (Issue #423 본문 + 메인 세션 검증):
  - `backend/.../resume/service/ResumeInterviewOrchestrator.java:83-85` — 의도가 답변 아닐 시 early return → 다음 질문 / 모드 전환 / 데이터 적재 분기 모두 미실행.
  - `ResumeInterviewOrchestrator.java:116` — 데이터 적재 이벤트가 답변 분기에서만 발행.
  - `backend/.../interview/service/TurnAnalysisPipeline.java:32` — Resume 트랙 호출 진입점 (분류 단계).
  - `backend/.../interview/service/TextFallbackTurnAnalyzer.java:24` — Standard 트랙 (audio STT fallback) 호출 진입점.
  - 제거 대상 자산 식별 = Issue #423 본문 영향 범위 표 / 제거 대상 목록 참조 (구체 정밀화는 tech-spec 단계).
- 인접 plan: `docs/plans/410-resume-context-defects/` — 동일 도메인 (Resume) 직전 컨텍스트 결함 정리. 본 작업과 독립.
- 중복 Issue: 없음 (`gh issue list "intent classifier"` 0건).

## Goal

- [ ] 면접 진행 차단 결함 재현 0건 (Resume + Standard 트랙, 도그푸딩 시나리오 5건 + 통합 테스트 시나리오에서 모두 정상 진행).
- [ ] Question / question_score / rubric 적재 누락 0건 (Resume + Standard 트랙 통합 테스트로 답변 → 적재 경로 검증).
- [ ] 의도 분류 stage 잔존 0건 — 기준: Issue #423 본문 "제거 대상" 목록이 코드베이스에서 모두 제거됨 (정적 검증). 구체 자산 분류 / 검증 절차는 tech-spec 단계 정밀화.

## Non-Goals

- **분류 정밀도 보강 / 분류 단계 유지** — 사유: 본 작업은 분류 단계 자체가 over-engineering 이라는 판단에서 출발. 정밀도 개선 방향과 양립 불가.
- **분류 메타 데이터 (intent type / confidence) 활용한 분석 / 학습** — 사유: 분류 산출물 자체를 폐기하는 것이 핵심. 데이터 유지 가치 추구 안 함.
- **모든 엣지 케이스 100% 자연 처리** — 사유: 면접관 흡수가 가설인 만큼 100% 보장 추구 안 함. 수동 검수 시나리오 수준 통과가 기준.

## 수용 기준 (Acceptance Criteria)

- [ ] Resume 트랙 면접 시작 → 답변 입력 시 다음 질문 또는 다른 모드 전환이 외부 관찰 가능하게 발생.
- [ ] Standard 트랙 (audio STT fallback) 동일 동작.
- [ ] 모든 답변 경로에서 Question / question_score / rubric row 정상 적재 (통합 테스트로 확인).
- [ ] 답변자가 "모르겠다" 류 발화 시 면접관이 다른 각도 / 다음 항목으로 진행 (수동 검수 시나리오 2건 통과).
- [ ] 답변자가 주제 무관 발화 시 면접관이 redirect 또는 다음 항목으로 진행 (수동 검수 시나리오 2건 통과).
- [ ] 일반 답변 정상 처리 (수동 검수 시나리오 1건 통과).
- [ ] 위 수동 검수 5건 (GIVE_UP 2 + OFF_TOPIC 2 + 일반 1) **모두 통과** = 합격 라인. 1건이라도 실패 시 재작업.
- [ ] 의도 분류 stage 잔존 0건 (Issue #423 "제거 대상" 목록 기준 정적 검증 + 회귀 테스트 통과).
- [ ] 면접 진행 차단 결함 재발 시 운영자가 로그로 식별 가능 — 식별 키: 면접 세션 ID + 트랙 (Resume/Standard) + 차단 시점 단계. 로그 레벨 WARN 이상.

## 비스코프 (Don't)

- **Resume 트랙 WRAP_UP 모드 제거** — 사유: 별도 Issue #424.
- **면접관의 엣지 흡수 정확도 자동 메트릭 / 대시보드** — 사유: 본 작업은 차단 결함 해소 우선. 자동 측정은 후속 plan (메트릭 후속 Issue placeholder).
- **면접관 페르소나 톤 / 난이도 / 캐릭터 / 질문 전략 재설계** — 사유: 본 작업 범위 외. 흡수 능력 보강 수준만.
- **답변자 발화 통계 / 분류 로그 영구 보존** — 사유: 분류 단계 폐기가 목적. 통계 가치 < 코드 단순화 가치.

## 참고

- 관련 Issue: #423
- 후속 Issue: #424 (Resume WRAP_UP 모드 제거), 메트릭화 후속 (placeholder — 추후 등록)
- 인접 plan: `docs/plans/410-resume-context-defects/`, `docs/plans/412-resume-project-name/`
