# Product Spec — Resume 면접 LLM prompt 맥락 정합성 회복 (#466 + #457 통합)

> **작성자**: PM (create-product-spec 스킬)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

### 현재 상태 (정상 동작)

Resume 트랙 면접:
1. 면접 시작 시 `resume-interview-planner` LLM 1회 호출 → `interview_plan` 에 OPENER 텍스트 + chain 풀 사전 박힘
2. PLAYGROUND 모드 진행 (워밍업 질문 누적)
3. 임계 도달 시 INTERROGATION 모드 자동 전환 → chain 단위 깊이 파고들기

### 발생 증상

**증상 1 — OPENER 응시자·프로젝트 무관 진부** (관찰: `interview_id=27`)
- 프로젝트 이름 명시 없이 "역할과 인상 깊었던 경험" 일반 문구 반복
- 응시자 이력서에 명시된 프로젝트가 무엇인지 OPENER 단계에서 식별 불가

**증상 2 — PLAYGROUND→INTERROGATION 전환 시 거짓 다리 phrasing** (관찰: 동일 면접)
- 직전 답변: "리허설 프로젝트에서 가장 기억에 남는 순간..."
- 첫 INTERROGATION 질문: "**방금 답변하신** 이벤트 기반 아키텍처에 대해 ..."
- 응시자가 그 주제로 답한 적 없음 → LLM 이 거짓 다리 phrasing 생성

**증상 3 — LLM 이 받는 직전 답변 품질 정보가 실제와 다름** (정합성 결함)
- 모드 전환 후 첫 chain 질문 생성 시점에 LLM 이 "이전 답변 품질 = 평균 이하 정상 답변" 으로 인지
- 그러나 같은 코드의 다른 경로 (분석 결과 비어 있는 경우) 정책 = "분석 불가 → 명료화 요청"
- 단일 결함 시점에서 두 경로 정합성 부재 (관찰: `InterrogationModeHandler:83` vs `AnswerAnalysis.empty()`)

**증상 4 — 다른 도메인에 동일 패턴 잠복 의심** (점검 필요)
- 세션 피드백 / 비언어 점수 입력 조립 단계에서 입력값이 비었을 때 임의 기본값 (예: position="UNKNOWN", level="MID", duration=0, difficulty="easy") 으로 메우는 코드 6건 식별
- 각 케이스가 LLM prompt / 점수 가중치에 흘러가는지 영향도 점검 필요 (정책 변경 결정은 본 spec 범위)

### 사용자·운영 인지 채널

- 운영 DB row 직접 조회 (`interview_id=27` / `questionset_id=129` / qid 153~156) — (관찰)
- PR #455 (Issue #430) 리뷰 중 fallback 패턴 노출 → #457 등록 — (관찰)
- 사용자 체감 발생 빈도 정량화 부재 — (한계: 1건 직접 관찰 + 코드 결함 동시 일치)

---

## 왜 해야 하는가 (Why)

### 사용자 임팩트
- "방금 답변하신 X" 거짓 phrasing → 응시자 혼란 + 면접 신뢰도 손상 (제품 핵심 가치 = "맞춤형 꼬리질문" 정면 위배) — (관찰)
- OPENER 가 응시자 프로젝트를 명시하지 못함 → 첫 질문 단계에서 "내 이력서 안 본 것 같다" 인상 — (관찰)
- 한계: 빈도 미측정 — 1건 직접 관찰 + 패턴 코드 결함 동시 일치 (추정 영향 규모)

### 운영·시스템 임팩트
- LLM 이 받는 직전 답변 컨텍스트가 실제와 다른 가짜 시그널 → chain 진행 결정 (한 단계 더 깊이 가기 / 머무르기 / 다음 chain 으로) 왜곡 가능 — (추정)
- 정책 단일 소스 부재 → 향후 `AnswerAnalysis` 정책 변경 시 회귀 위험 — (관찰)
- 6건 fallback 미점검 → "비었으면 평균값 메우기" 패턴이 다른 도메인에 잠복 가능 — (의심, 영향도 분류 완료)

### 외부 압력
- 사용자 (관찰자) 가 직접 면접 row 까보고 결함 식별 → 운영 신뢰도 하락
- PR #463 (skeleton 메타 4종 추출) 머지 직전 → OPENER 에 프로젝트 이름 등 입력 신호 활용할 토대 마련됨

---

## 해결 방향 (Approach)

3 Phase 분리. 공통 root = "LLM 이 받는 입력에서 실제와 다른 가짜 컨텍스트 제거".

### Phase 1 (즉시 효과 — 공통 root)
- 모드 전환 직후 첫 INTERROGATION 질문 생성 시 LLM 이 직전 PLAYGROUND 답변 내용을 인지하도록 한다
- 직전 답변 분석 결과가 비어 있는 경우 가짜 평균값을 prompt 에 흘리지 않고 정책 단일 소스 (분석 불가 → 명료화) 와 정합 처리
- chain 첫 질문 phrasing 가이드 보정 — 직전 답변 내용을 실제로 참조하지 않은 경우 "방금 답변하신 ~" 류 다리 phrasing 사용 금지

### Phase 2 (OPENER 정합성)
- OPENER 가 응시자 이력서에서 식별된 프로젝트 이름을 명시한다
- 해당 프로젝트가 무엇인지 (간단한 설명) + 응시자가 어떤 역할을 맡았는지를 고정 패턴으로 묻는다
- 약간의 꼬리질문 추가 허용 (1~2개)
- 응시자별 OPENER 다양화 자체는 본질 아님 — 프로젝트 단위 anchoring 이 핵심

### Phase 3 (확장 점검 — Phase 1 정책 적용)
- 6건 fallback 케이스 각각: (a) 비어 있을 수 있는 정상 경로면 명시 분기 / (b) 비어 있으면 안 되는 경로면 거부 / (c) 정당한 기본값이면 사유 명문화 중 1로 정리
- 정책 일관성: "비었을 때 가짜로 메우기 금지" 원칙 동일 적용
- 구체 처리 방법 (구현 수단) 은 tech-spec 결정

대안 비교:
- 한 PR 일괄 처리: 정책 일관성 ↑ but PR 비대 / 리뷰 부담 / 승인 게이트 지연 → 거부
- Issue 별 분리 (#466 / #457 각자): 공통 root (모드 전환 직후 컨텍스트 단절) 중복 작업 + 정책 단일 소스 부재 → 거부
- 채택: 1 spec / 3 phase / phase 별 PR 분리 → 사용자 체감 즉시 + 정책 일관 + 리뷰 단위 작음

우선순위: Phase 1 (사용자 체감 즉시 + 공통 root) > Phase 2 (OPENER 가시 결함) > Phase 3 (확장 점검).

---

## Evidence

### 코드 추적
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:177` — 모드 전환 시 직전 답변 / 분석 결과 silent drop — (관찰)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:27,83` — 분석 결과 비었을 때 prompt 에 평균값 흘림 — (관찰)
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java:27,49,55` — `empty()` = "분석 불가 → 명료화 요청" 정책 정의 — (관찰)
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java:81` — chain 질문 prompt 에 답변 품질 정보 노출 — (관찰)
- `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt:67-68` — 다리 phrasing 가이드 — (관찰)
- `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt` — OPENER 패턴 정의 — (관찰)
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/SessionFeedbackInputAssembler.java:242,243,248` — position / level / duration fallback — (관찰, 영향도 분류 완료)
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/NonverbalScorePersister.java:55` — difficulty fallback (rubric 가중치) — (관찰, 영향도 분류 완료)

### 운영 단서
- `interview_id=27` / `questionset_id=129` / qid 153~156 — 4 turn 시퀀스 row 직접 관찰 (Issue #466 본문) — (관찰)
- PR #455 리뷰 회로 (Issue #457 본문) — (관찰)

### 인접 plan
- `docs/plans/458-resume-skeleton-redesign/` — skeleton 메타 4종 (techStack / role / architecture / decisions) 추출 구조. Phase 2 OPENER 입력 신호 (프로젝트 이름 / 역할) 토대
- `docs/plans/421-resume-playground-opener-tone/` — PLAYGROUND opener tone 선행 작업. 본 spec 은 OPENER **내용 정합성** 다룸 (어조 정합은 421 에서 처리됨)
- `docs/plans/430-warn-log-helper/` — 식별자 / 상수화 부채 plan. 본 spec 은 부채 정리가 아닌 LLM prompt 정합성 회복 — 직접 의존 없음 (참고만)

---

## Goal

### Phase 1
- [ ] 모드 전환 직후 첫 INTERROGATION 질문이 직전 PLAYGROUND 답변 내용을 반영한다 (LLM 출력 텍스트 기준)
- [ ] 분석 결과가 비어 있는 경우 LLM 이 받는 직전 답변 품질 정보가 정책 단일 소스 (분석 불가 → 명료화) 와 정합한다 (외부 관찰: 가짜 평균값으로 인한 chain 진행 결정 왜곡 사례 0)
- [ ] 직전 답변 내용을 실제로 참조하지 않은 chain 질문에 "방금 답변하신 ~" 류 다리 phrasing 등장하지 않는다

### Phase 2
- [ ] OPENER 질문이 응시자 이력서에서 식별된 프로젝트 이름을 명시적으로 포함한다
- [ ] OPENER 가 (1) 프로젝트 설명 (2) 응시자 역할 두 항목을 고정 패턴으로 묻는다 (약간의 꼬리질문 1~2개 추가 허용)
- [ ] 동일 응시자 / 동일 skeleton 입력 시 OPENER 가 skeleton 의 프로젝트 중 정확히 1개를 anchor 한다 (어느 거든 OK — 다양성 의도. 동일성 강제 X)

### Phase 3
- [ ] 6건 fallback 각 케이스가 (a) 명시 분기 / (b) 거부 / (c) 정당화 사유 명문화 중 1로 정리된다
- [ ] 운영자가 정책 결정 사유를 코드 또는 spec 문서에서 사후 확인할 수 있다

---

## Non-Goals

- 새 LLM 분석 엔진 / 점수 산식 변경 — 본 spec = 결함 제거. 신규 능력 도입 X (사유: 정합성 회복이 목적, 분석 능력 향상 X)
- 응시자별 OPENER 다양화 — 프로젝트 단위 anchoring 이 핵심 가치 (사유: 사용자 결정 — 프로젝트 이름 / 역할 / 설명 고정 패턴 OK)
- 전체 도메인 fallback 일괄 정리 — LLM prompt / 점수 영향 영역 한정 (사유: 영향 없는 fallback 까지 건드리면 PR 비대)

---

## 수용 기준 (Acceptance Criteria)

### Phase 1
- [ ] PLAYGROUND→INTERROGATION 전환 후 첫 질문이 직전 PLAYGROUND 답변 내용을 반영한다 (eval fixture 5종 — PLAYGROUND 답변 다른 5케이스, 첫 INTERROGATION 질문이 각 답변 내용을 anchor 함을 검토자 100% 매칭)
- [ ] 분석 결과 비어 있는 호출 경로에서 LLM 출력이 "이전 답변 평균 품질 가정" 으로 동작하지 않는다 (분석 불가 시 명료화 / 거부 둘 중 하나로 일관)
- [ ] 직전 답변 내용 참조 없는 chain 질문에 "방금 답변하신" 류 phrasing 등장 0건 (eval fixture 5종 — PLAYGROUND 답변 ↔ chain pool topic 의미 disjoint 케이스)
- [ ] 기존 면접 진행 정상 시나리오 회귀 없음 (정상 흐름 케이스 통과)

### Phase 2
- [ ] eval fixture 5종 (다른 프로젝트 이름) — 5종 OPENER 모두 해당 프로젝트 이름을 명시 (검토자 5/5 매칭)
- [ ] eval fixture 5종 — 5종 OPENER 모두 (1) 프로젝트 설명 요청 (2) 역할 요청 두 요소를 포함 (검토자 5/5 매칭)
- [ ] 기존 OPENER 정상 흐름 회귀 없음

### Phase 3
- [ ] 6건 fallback 각 처리 결정 (a/b/c 중 택1) tech-spec 에 명시
- [ ] 정리 후 기존 기능 회귀 없음
- [ ] 운영자가 fallback 발동 / 처리 정책 사유를 사후 확인할 수 있다 (수단은 tech-spec 결정)

---

## 비스코프 (Don't)

- Chain pool 사전 생성 vs 실시간 재계산 정책 변경 — 별도 Issue 권장 (사유: PLAYGROUND 답변 ↔ chain pool 의미 정합 검사 부재 = 본 spec phrasing / fallback 결함과 별개 축. 정책 변경 영향 큼)
- chain 식별자 합성키 (`p1::토픽`) prompt 노출 — 별도 Issue (사유: 잠재 결함, 본 spec 결함과 직교)
- FE 측 면접 진행 화면 변화 — BE prompt 정합성에 한정 (사유: 본 spec = LLM 입력 정합성. 화면 변경 시 별도 plan)
- LLM 분석 엔진 신규 시그널 추가 — 기존 시그널 정합성 회복에 집중 (사유: 신규 능력 도입은 별도 Issue)

---

## 참고

- 관련 Issue: #466 (primary trigger — phrasing bug), #457 (병합 — fallback 정합성)
- 의존 PR: #463 (closes #458 skeleton 메타 4종 추출) — Phase 2 머지 선행
- 인접 plan: `docs/plans/458-resume-skeleton-redesign/`, `docs/plans/421-resume-playground-opener-tone/`
- 발견 PR: #455 (Issue #430 식별자 enum / 상수화 — #457 발견 회로)
