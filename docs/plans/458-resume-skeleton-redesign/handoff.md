# Handoff — 458-resume-skeleton-redesign

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 2026-05-09
> **다음 세션**: plan 폴더 진입 시 이 파일 먼저 읽음

---

## 현재 상태

- **Phase 1 완료 / PR #463 머지됨**
  - PR: https://github.com/KoSeonJe/rehearse/pull/463
  - 타이틀: `[BE] feat: 이력서 분석 메타 4종 추출 + LLM 응답 누락 시 인터뷰 차단 없이 graceful drop`
  - 변경 내용: skeleton 메타 4종 (`techStack` / `role` / `architecture` / `decisions`) 추출 + claim text 매칭 + graceful drop + Live LLM E2E 3 fixture
  - closes #458 Phase 1
- **Phase 2 대기 중** — 1주 관찰 기간 (D4 게이트) 충족 후 진입 가능 (Phase 1 머지 기준 ~2026-05-16 이후)
- 브랜치: `feat/435-resume-model-answer-quality` (Phase 1 작업 브랜치, 이미 머지됨)
- 빌드: 통과 (PR #463 머지 기준)

---

## 다음 세션 시작점

### Phase 2 즉시 진입 금지 — Issue #466 먼저

PR #463 머지로 Phase 1 인프라는 배포됐으나, 사용자가 관찰한 면접 품질 결함이 미해결 상태임.
해당 결함은 Issue #466으로 등록됨. **Issue #466 해결 후 Phase 2 진입**.

**다음 세션 첫 작업 순서**:
```
1. Issue #466 product-spec 작성 (/create-product-spec 또는 수동)
   폴더: docs/plans/{NNN}-resume-interview-context-continuity/
   (NNN = 현재 최신 plan 번호 + 1. 458 계열 폴더 목록 확인 후 결정)
2. product-spec → tech-spec 작성 (backend agent 위임)
3. tech-spec 사용자 승인 → implement-plan 작성
4. 구현 → code-reviewer-backend 리뷰 → PR → 머지
5. 머지 후 #458 Phase 2 진입
```

---

## Issue #466 상세

- URL: https://github.com/KoSeonJe/rehearse/issues/466
- 타이틀: `[Resume] 면접 진행 맥락 연속성 결함 — OPENER 진부 + 모드 전환 거짓 phrasing`
- 라벨: `type:bug`

### 관찰 (interview_id=27 / questionset_id=129)

| 순서 | qid | type | 질문 |
|------|-----|------|------|
| 1 | 153 | RESUME_OPENER | 리허설 프로젝트에서 맡으신 역할과 그 과정에서의 인상 깊었던 경험에 대해 자세히 말씀해 주실 수 있나요? |
| 2 | 154 | RESUME_PLAYGROUND | 리허설 프로젝트에 대해 좀 더 구체적으로 말씀해 주실 수 있나요? |
| 3 | 155 | RESUME_PLAYGROUND | 리허설 프로젝트에서 가장 기억에 남는 순간은 무엇이었나요? |
| 4 | 156 | RESUME_INTERROGATION | 방금 답변하신 이벤트 기반 아키텍처에 대해 구체적으로 어떤 기술을 사용하셨는지 말씀해 주실 수 있나요? |

### 두 가지 결함

**결함 1 — OPENER 진부**
응시자 이력서 / skeleton 메타 무관하게 "역할 + 인상 깊은 경험" 단일 패턴 고착.
이력서에 따라 OPENER가 달라져야 하나 현재 planner 사전 생성 시 다양성 없음.

**결함 2 — 모드 전환 거짓 phrasing**
qid 155 (PLAYGROUND 마지막): "리허설 프로젝트에서 가장 기억에 남는 순간" 질문
qid 156 (INTERROGATION 첫 질문): "방금 답변하신 이벤트 기반 아키텍처에 대해..."
→ 응시자가 "기억에 남는 순간"을 답했는데 "방금 이벤트 기반 아키텍처를 말씀하셨다"는 거짓 phrasing.

### 원인 분석 (코드 출처)

| # | 원인 | 코드 위치 |
|---|------|----------|
| 1 | 사전 생성 plan 락인 — `resume-interview-planner` LLM 1회 호출로 `opener_question` + `primary_chains` 풀 고정. 진행 중 응시자 실제 답변 흐름 무관하게 플랜 유지 | `resume-interview-planner.txt` (planner prompt 전반) |
| 2 | PLAYGROUND → INTERROGATION 모드 전환 직후 null answer 전달 — 모드 전환 시 `null answer`, `null analysis`로 chain interrogator 호출. 직전 PLAYGROUND turn 답변을 인용 불가 | `ResumeInterviewOrchestrator.java:177` |
| 3 | chain interrogator few-shot "방금 답변하신" 패턴 — few-shot 예시가 `"방금 답변하신 [topic] 의 ~"` 형식이라 LLM이 그대로 mimic | `resume-chain-interrogator.txt:67` |
| 4 | (잠재 결함) `getCurrentChainId()` 합성키 노출 — `"p1::토픽"` 형태 합성키가 `<<<CURRENT_CHAIN>>>` 블록에 그대로 노출될 가능성 | `InterrogationModeHandler.java:54` |

### 우선순위 4개 작업

| 우선 | 작업 | 핵심 파일 |
|------|------|----------|
| 1 | Orchestrator:177 — PLAYGROUND 마지막 답변 + analysis 를 INTERROGATION 첫 호출에 전달 | `ResumeInterviewOrchestrator.java` |
| 2 | chain-interrogator.txt:67 — few-shot "방금 답변하신" 패턴 제거 + 자연스러운 다리 phrasing 가이드로 교체 | `resume-chain-interrogator.txt` |
| 3 | planner few-shot 패턴 다양화 + skeleton 메타 4종을 OPENER prompt에 입력 (OPENER 진부 해결) | `resume-interview-planner.txt` |
| 4 | `getCurrentChainId()` 합성키 노출 fix | `InterrogationModeHandler.java` |

---

## #458 Phase 2 (Issue #466 해결 후 진입)

- 위치: `docs/plans/458-resume-skeleton-redesign/tech-spec.md` Phase 2 섹션
- 내용: standard `AnswerAnalyzer` 재활용 + chain interrogator에 `ANSWER_ANALYSIS` / `RESUME_SKELETON_CONTEXT` 블록 inject
- 진입 조건 (D4 게이트 — 1주 관찰 후):
  - 운영 신규 row validator graceful drop = 0건
  - 운영 5xx 회귀 0건
  - 추출 결과 4종 메타 누락 row 0건
  - 미달 시 Phase 1 prompt instruction 보강 후 관찰 연장

---

## 컨텍스트 메모

- **Phase 1 운영 관찰 (D4 게이트)**: drop > 0 → Jaccard 한글 N-gram 보강 fix PR 필요. 5xx 발생 → `mapClaim` guard fix PR 필요. 기간 충족 후 Phase 2 진입.
- **함정 — ORPHAN_CLAIM enum**: Phase 1에서 `ORPHAN_CLAIM` 을 drop 카운터 로그 키로 전환. enum 자체 존재 여부는 `ResumeErrorCode.java` grep 후 외부 사용처 확인 후 결정 (surgical 룰).
- **함정 — 운영 Plan row 호환**: 기존 `expectedClaimsCoverage` 에 LLM 합성 claimId 문자열 보유한 row들은 text 매칭 실패 → graceful drop → playground prompt 가이드 일부 소실. 면접 자체는 진행됨 (fatal 아님). 신규 면접부터 신규 plan 적용.
- **함정 — chain interrogator prompt token**: ANSWER_ANALYSIS + RESUME_SKELETON_CONTEXT inject로 Phase 2에서 input 토큰 증가. MVP 사용자 부재라 사전 측정 생략 결정. 5xx 발생 시 사후 prompt 재설계 대응.
- **결정 — Rubric scoring 회귀 생략**: `[claimId]` → `[1]/[2]` sequential idx 변경. 점수 회귀 fixture 비교는 사용자 확정으로 생략. 운영 점수 분포 이상 발견 시 별도 plan.
- **결정 — 단일 영역 BE 작업**: FE 시그니처 변경 없음. Skeleton / Plan = 서버 내부 도메인.

---

## 핵심 파일 (다음 세션 첫 Read 추천)

```
# Issue #466 원인 분석용
backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java  (line 177 집중)
backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java     (line 54 집중)
backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt
backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt               (line 67 집중)

# #458 Phase 2 진입 시
docs/plans/458-resume-skeleton-redesign/tech-spec.md  (Phase 2 섹션)
```

---

## 참고 명령

```bash
# BE 테스트 실행 (Issue #466 구현 검증)
cd backend
./gradlew test --tests "com.rehearse.api.domain.resume.*"

# Phase 1 D4 게이트 모니터링 — 운영 로그에서 graceful drop 확인
grep "droppedClaimCount" <prod-log>

# Live LLM E2E 회귀 (Phase 1 관찰)
RUN_LIVE_API=true ./gradlew test --tests "Resume*LiveLlm*"
```

---

업데이트: 2026-05-09
