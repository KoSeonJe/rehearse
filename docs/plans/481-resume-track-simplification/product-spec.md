# Product Spec — 이력서 면접 트랙 단순화 (Chain FSM / Mode / InterviewPlan / 다층 컨텍스트 / L1FN 가드 폐기 + AnswerAnalysis 재설계)

> **작성자**: 사용자
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태:
  - 이력서 면접 트랙 = 2단계 모드 FSM (`ResumeMode.PLAYGROUND` ↔ `ResumeMode.INTERROGATION`) + 4-level Chain FSM (`ChainStateTracker`) + `InterviewPlan` 사전 계획 (primary chains + backup chains) + 모드별 prompt builder
  - 표준 면접 트랙 = `QuestionSet` 사전 생성 + 순서 진행 + 시간 잔여 시 LLM 꼬리질문
- 발생 증상:
  - 증상 1 — 동일 주제 질문 3~4회 반복. evidence = `interview_id=29` Q162~165 (`RESUME_INTERROGATION`, "이벤트 기반 아키텍처" 4회)
  - 증상 2 — "왜 이 질문이 나왔는가" 추적 난해. FSM 상태 + Plan + 모드 핸들러 다층 분기 거쳐야 답 가능
  - 증상 3 — 동일 본질 (사전 질문 + 꼬리질문) 을 두 트랙이 다른 코드 경로로 처리 → 유지보수 비용 ↑
  - 증상 4 — 분석 모델 (`AnswerAnalysis`) 의 부족점 표현 = `missingPerspectives` 7종 enum + `answerQuality` int. Rubric 평가 dimension 과 별도 축 → "부족점" 이 두 개념으로 분리 + 꼬리질문 타겟팅이 enum 매핑 의존 (LLM 자율 신호 약화)
  - 증상 5 — `L1FalseNegativeGuard` 코드 분기 (`claims.isEmpty() && answerQuality<=1` → 강제 `CLARIFICATION`) 가 동일 주제 재질문 트리거 후보. 코드 룰 vs LLM 자율 신호 충돌
- 사용자·운영 인지 채널:
  - 사용자 체감 (반복 답변 요구)
  - 운영 로그 분석 (interview 29 trace = LLM 매 턴 `LEVEL_STAY` 반환 → tracker 강제 `LEVEL_UP` 만 작동, `CHAIN_SWITCH` 강제 조건이 `level=MAX` 한정이라 동일 chain 갇힘)

## 왜 해야 하는가 (Why)

- 사용자 임팩트:
  - 동일 답변 반복 요구 → 면접 품질 체감 저하 / 신뢰 손실
- 운영 / 시스템 임팩트:
  - Chain FSM 실효 미검증. 매직넘버 (`LEVEL_STAY_MAX_TURNS=2`, `MAX_LEVEL=4`) 영구 튜닝 부담
  - LLM 자율 신호 ↔ FSM / 가드 강제 룰 결정 주체 충돌 → 디버깅 난도 ↑
  - 이력서 도메인 45 파일 / 2352줄. 트랙 분기로 표준 트랙 개선 시 이중 작업 부담
  - `InterviewRuntimeState` 누적 필드 (`coveredClaims` 등) + `DialogueCompactor` 비동기 요약 + 다층 (L1~L4) 컨텍스트 — 사전 질문 일괄 생성 + 1-step 꼬리질문 흐름엔 불필요. 실제 production write 호출자 부재 필드 존재 (dead code)
- 외부 압력:
  - 인접 plan (404 / 410 / 421 / 434) = 이력서 트랙 패치 누적. 근본 구조 단순화 시점 도래

## 해결 방향 (Approach)

PM 수준 high-level. 구현 디테일은 tech-spec.

- 핵심 접근:
  - 이력서 트랙을 표준 트랙과 동일 패턴으로 통합
  - 흐름 = opener 2~3개 (프로젝트 설명, 꼬리질문 없음) → main 질문 N개 (각 main 마다 LLM 꼬리질문 1회 따라붙음) → 시간 만료 시 종료
  - main 개수 N = duration 기반 충분히 책정해 "시간 안에 main 소진" 발생 불가 설계
  - 모드 / Chain / Level 개념 폐기. LLM 자율 + prompt 가이드로 꼬리질문 깊이 조절
- 생성 시점 / 이력서 정합성:
  - opener + main 일괄 생성 시점 = **면접 시작 시 1회**. ResumeSkeleton (사전 파싱된 이력서 구조) 기반 LLM 1회 호출 → `QuestionSet` 적재. 진행 중 추가 LLM 호출은 꼬리질문 한정
  - 이력서 정합성 보장 = opener / main / 꼬리질문 모든 prompt 에 ResumeSkeleton 동봉. "스켈레톤 내 `projects` / 기술 topic 만 활용" prompt 가이드. 사후 코드 단언 X (LLM 자율)
  - ResumeSkeleton 의 `interrogationPriorityMap` 필드 (Chain FSM 의존 잔재) = P3 단계에서 정리
- 분석 모델 (`AnswerAnalysis`) 재설계:
  - 부족점 표현 = **Rubric dimension 단일 축으로 통일**. 기존 `missingPerspectives` 7종 enum + `answerQuality` int → `dimensionGaps: Map<RubricDimension, Integer>` + `weakestDimension`. 채점 / 꼬리질문 타겟팅 같은 축 공유 → 매핑 코드 제거 + LLM 자율 신호 강화
  - 식별자 = `turnId` → `mainQuestionId` 로 명확화 (1 main = 1 follow-up, 별도 turn 테이블 없음)
  - `claims` 필드 **유지** — 표준 트랙 follow-up 템플릿이 `target_claim_idx` 로 파고들 claim 선택. 폐기 시 템플릿 재작성 부담
  - **`L1FalseNegativeGuard` 코드 분기 제거** — `RecommendedNextAction` 결정 = LLM 자율. prompt 가이드로 "claim 부재 + 품질 저하 시 CLARIFICATION" 의도 전달
- 부수 폐기 범위 확장:
  - `InterviewRuntimeState` 누적 필드 (`coveredClaims`, `activeChain` 등 production write 부재 dead code) 제거 → RuntimeState 본질 (현재 main idx / 진행 상태) 만 유지
  - `DialogueCompactor` + 대화 요약 파이프라인 폐기 — 사전 질문 일괄 생성 흐름에선 누적 요약 불필요. 꼬리질문 컨텍스트 = 직전 main + 사용자 답변 + ResumeSkeleton 만
  - 다층 (L1~L4) 컨텍스트 분기 제거 → 단일 prompt (main + 답변 + skeleton)
  - 꼬리질문 입력 = main 질문 텍스트 + 사용자 답변 (audio analysis 결과 + 텍스트) + ResumeSkeleton + AnswerAnalysis (dimensionGaps + claims)
- 대안 비교:
  - (대안 A) Chain FSM `CHAIN_SWITCH` 강제 조건 보강 hotfix — 폐기 예정 코드 패치. 근본 미해결. 기각
  - (대안 B) 표준 트랙 자체를 이력서 트랙처럼 FSM 화 — 복잡도 확산. 기각
  - (채택) 이력서 트랙을 표준 패턴으로 흡수 + 분석 모델 단일 dimension 축으로 통일 — 코드 삭제 + 디버깅 가시성 + LLM 결정 영역 명확화
- 단계 분리 (3 Phase, dev only 라 병행 단계 생략):
  - P1 — Rubric 디커플링 (점수 산출에서 `resumeMode` / `currentChainLevel` 의존성 제거. 회귀 검증 선행)
  - P2 — 구 흐름 제거 + 신규 흐름 도입 (단일 PR: Mode / Chain / Plan / 핸들러 / `DialogueCompactor` / 다층 컨텍스트 / L1FN 가드 폐기 + AnswerAnalysis 재설계 + 신규 흐름 동시)
  - P3 — RuntimeState + ResumeSkeleton 슬림화 (구 코드 제거 후 미사용 필드 정리. `coveredClaims` / `activeChain` / `ResumeSkeleton.interrogationPriorityMap` 포함)

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/resume/` — 45 파일 / 2352줄
  - 폐기 대상 (이력서 트랙 핸들러) = `ResumeInterviewOrchestrator.java` + `PlaygroundModeHandler.java` + `InterrogationModeHandler.java` + `ChainStateTracker.java`
  - `backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStateTracker.java:75-89` — `LEVEL_STAY` 한계 초과 시 강제 `LEVEL_UP` 만, `CHAIN_SWITCH` 강제 조건이 `level=MAX(4)` 한정
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:101-123` — `applyDecision` 분기
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java:10` — `RESUME_OPENER` 이미 `RubricCategory.EXPERIENCE` 매핑 (재사용 기반 존재)
  - `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/event/TurnCompletedEvent.java:19,42` — Rubric 결합점 (`resumeMode` + `currentChainLevel`)
  - `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java` — `missingPerspectives` 7종 enum + `answerQuality` int + `applyL1FalseNegativeGuard()` 코드 분기
  - `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java` — `coveredClaims` / `activeChain` production write 호출자 부재 (dead code)
  - `DialogueCompactor` — 비동기 대화 요약 LLM 호출 (`compaction_summarizer`, temp 0.3, maxTokens 800)
- 운영 로그:
  - `interview_id=29` Q162~165 RESUME_INTERROGATION 4회 반복. tracker trace = LEVEL_STAY count 누적 → 강제 LEVEL_UP → 같은 chain 유지
- 인접 plan:
  - 404 (interview-domain-findings), 410 (resume-context-defects), 421 (resume-playground-opener-tone), 434 (resume-playground-hard-cap)
- 메모리:
  - Interview Quality Sprint S14 후보로 본 epic 트랙됨

## Goal

- [ ] 이력서 면접 본질 유지 — opener 2~3 질문 (프로젝트 설명, 꼬리질문 없음) → main 기술 질문 (각 main 마다 꼬리질문 1회). 시간 만료까지 main + 꼬리 반복
- [ ] 동일 주제 3회 이상 반복 출제 차단 (e2e 회귀 테스트)
- [ ] 운영자가 `question` 테이블 row 1회 조회로 "이 질문이 사전 질문인지 / 꼬리질문인지 / 어떤 topic 인지" 식별 가능
- [ ] Rubric 점수 snapshot diff 회귀 0
- [ ] 신규 이력서 면접 = 표준 트랙 꼬리질문 서비스 코드 경로 재사용. 트랙별 분기 코드 0
- [ ] 답변 부족점 표현 = Rubric dimension 단일 축. 별도 enum (`missingPerspectives`) / int (`answerQuality`) 축 폐기. 채점 + 꼬리질문 타겟팅이 같은 축 공유
- [ ] 꼬리질문 결정 코드 분기 (L1FN 가드 등) 0. `RecommendedNextAction` = LLM 자율

## Non-Goals

- Level-aware 깊이 강제 (1=배경 / 2=구현 / 3=원인 / 4=의사결정) — 사유: LLM 자율 + prompt 가이드로 대체. 깊이 강제 ≠ 본 작업 목표
- backup chains fallback — 사유: 표준 트랙엔 없는 개념. 단순화 우선
- 구 흐름 호환 유지 — 사유: dev 전용 단계. 기존 면접 데이터 보존 불필요
- `AnswerFeedbackPerspective` 7종 enum 보존 — 사유: Rubric dimension 으로 통일. enum 자체 폐기 대상
- 대화 요약 / 다층 컨텍스트 보존 — 사유: 사전 질문 일괄 생성 흐름에 불필요. 누적 요약 없이 직전 main + 답변 + skeleton 만 사용
- 표준 트랙 follow-up 템플릿 (`target_claim_idx`) 변경 — 사유: claims 유지로 템플릿 재작성 불필요

## 수용 기준 (Acceptance Criteria)

- [ ] 신규 이력서 면접 진입 시 opener 2~3개 (프로젝트 설명, 꼬리질문 미출제) → 각 main 질문 직후 꼬리질문 1회 → 시간 만료 시 종료 (e2e 테스트). main 소진 시나리오 발생 안 함 (N duration 기반 충분 책정)
- [ ] interview 29 와 동일한 면접 조건 / 답변 패턴 e2e 시나리오에서 동일 주제 3회 이상 반복 출제 발생 시 fail (회귀 disabled 처리)
- [ ] `question` 테이블 row 1개로 "사전 질문 / 꼬리질문" 구분 가능 + topic 식별 가능
- [ ] Rubric 점수 snapshot diff 회귀 0 (기존 fixture 기반)
- [ ] dev 환경 구 데이터 제거 후 신규 흐름으로 면접 정상 동작 (수동 검증 + e2e 테스트)
- [ ] 생성된 opener / main / 꼬리질문 = 모두 해당 이력서 내 `projects` / 기술 topic 범위 (LLM prompt 에 ResumeSkeleton 동봉 검증)
- [ ] 답변 분석 산출물에서 Rubric dimension 별 부족 정도 + 최약 dimension 이 운영자에게 노출 (기존 perspective enum / quality int 축 부재)
- [ ] 꼬리질문 LLM prompt 가 직전 main 의 최약 dimension 을 포함 (운영 LLM 로그로 검증)
- [ ] 답변 분석 / 꼬리질문 결정 경로에서 코드 강제 분기 부재 — 결정 = LLM 자율 (회귀 테스트 + grep 검증)

## 비스코프 (Don't)

- main 질문 N 개수 산출 공식 (`durationMinutes` 기반, "소진 불가" 충분 책정 기준) — tech-spec 단계
- opener 2개 prompt 문구 정밀화 — tech-spec 단계
- AB 테스트 인프라 구축 — 별도 plan
- prod 환경 마이그레이션 전략 — 현 단계 N/A (dev only)
- 표준 트랙 자체 패턴 개선 — 별도 Issue

## 참고

- 관련 Issue: #481
- 인접 plan: docs/plans/404-interview-domain-findings/, docs/plans/410-resume-context-defects/, docs/plans/421-resume-playground-opener-tone/, docs/plans/434-resume-playground-hard-cap/
- evidence trace: interview_id=29 Q162~165 (EC2 dev DB)
