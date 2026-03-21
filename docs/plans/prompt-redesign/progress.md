# 프롬프트 재설계 — 진행 상태

> **최종 업데이트**: 2026-03-21
> **전체 상태**: 계획 완료 (검토 반영 v2), 구현 대기

---

## Phase 1: [BE] 기반 구조 `TODO`

- [ ] Task 1-1: TechStack enum 생성
- [ ] Task 1-2: DB 마이그레이션 V9 (tech_stack 컬럼)
- [ ] Task 1-3: Interview 엔티티 techStack 필드 + getEffectiveTechStack()
- [ ] Task 1-4: YAML 프로필 구조 생성 (base 5개 + overlay 5개)
- [ ] Task 1-5: PersonaResolver + BaseProfile/StackOverlay/ResolvedProfile (fromBaseOnly, base null 방어 포함)

**PR**: `feat/prompt-persona-foundation` → develop
**병렬**: Task 1-1~1-3 ↔ Task 1-4 독립 진행 가능 (Task 1-5는 둘 다 완료 후)

---

## Phase 2: [BE] 프롬프트 빌더 리팩토링 `TODO`

- [ ] Task 2-1: LevelGuideProvider 생성
- [ ] Task 2-2: 프롬프트 템플릿 파일 작성 (question-generation.txt, follow-up.txt)
- [ ] Task 2-3: AiClient Parameter Object (QuestionGenerationRequest, FollowUpGenerationRequest) + 프롬프트 빌더
- [ ] Task 2-4: **[C1]** QuestionGenerationRequestedEvent에 techStack 추가 + 이벤트 체인 수정
- [ ] Task 2-5: ClaudeApiClient + MockAiClient 구현 변경 + InterviewService 호출 수정
- [ ] Task 2-6: **[H4]** InterviewResponse에 techStack 추가
- [ ] Task 2-7: **[H5]** CreateInterviewRequest techStack + position-techStack 검증 로직

**PR**: `feat/prompt-builder-refactor` → develop
**의존**: Phase 1

---

## Phase 3: [BE] Internal API 확장 `TODO`

- [ ] Task 3-1: AnswersResponse에 position/techStack/level 추가 (InterviewFinder 사용, 교차 도메인 의존 방지)

**PR**: `feat/internal-api-interview-context` → develop
**의존**: Phase 1
**병렬**: Phase 2와 동시 진행 가능

---

## Phase 4: [Lambda] 프롬프트 최적화 `TODO`

- [ ] Task 4-1: VerbalPromptFactory 생성 (keyword_usage 별도 필드 없음, comment에 자연어 반영)
- [ ] Task 4-2: verbal_analyzer.py 수정 (factory 사용, position=None 폴백)
- [ ] Task 4-3: **[H6]** handler.py 수정 — `_build_timestamp_feedbacks` + `_safe_verbal` + `analyze_verbal` 3함수 시그니처 체인 변경
- [ ] Task 4-4: 비언어 분석 프롬프트 최적화 (vision_analyzer.py — JSON 스키마 압축만)

**PR**: `feat/lambda-verbal-prompt` → develop
**의존**: Phase 3

---

## Phase 5: [FE] 기술스택 선택 UI `TODO`

- [ ] Task 5-1: TechStack 타입 + 상수 정의
- [ ] Task 5-2: StepTechStack 컴포넌트 생성
- [ ] Task 5-3: 위저드 5단계 확장 (use-interview-setup, interview-setup-page)

**PR**: `feat/fe-tech-stack-selection` → develop
**의존**: Phase 2 (BE API에 techStack 필드 + 서버 검증)
**병렬**: Phase 4와 동시 진행 가능

---

## Phase 6: 통합 검증 + A/B 테스트 `TODO`

- [ ] Task 6-1: BE 통합 테스트
- [ ] Task 6-2: 하위 호환성 E2E 검증
- [ ] Task 6-3: A/B 테스트 (prompt-test-guide.md 기반)

**PR**: `feat/prompt-redesign-tests` → develop
**의존**: Phase 2 + Phase 4

---

## 의존성 그래프

```
Phase 1 ──┬──> Phase 2 ──────> Phase 5 (FE)
           │                       │
           ├──> Phase 3 [parallel] ──> Phase 4 (Lambda)
           │                       │
           └───────────────────────┴──> Phase 6 (검증)
```

---

## 검토 반영 이력

| 날짜 | 이슈 | 반영 내용 | 문서 |
|------|------|----------|------|
| 2026-03-21 | C1 | Event techStack 전파 — Task 2-4 추가 | plan-02 |
| 2026-03-21 | C2 | keyword_usage 저장하지 않음 결정 (D8) — 프롬프트에서 제거, comment에 반영 | plan-04, requirements |
| 2026-03-21 | H1 | AiClient Parameter Object 도입 — Task 2-3에 Request DTO | plan-02 |
| 2026-03-21 | H2 | PromptBuilder가 Interview 엔티티 대신 Request DTO 수신 | plan-02 |
| 2026-03-21 | H3 | InterviewFinder 사용 — questionset→interview 교차 의존 방지 | plan-03 |
| 2026-03-21 | H4 | InterviewResponse에 techStack 추가 — Task 2-6 | plan-02 |
| 2026-03-21 | H5 | position-techStack 서버 검증 — Task 2-7 + ErrorCode | plan-02 |
| 2026-03-21 | H6 | Lambda 시그니처 체인 명시 — 3함수 변경 | plan-04 |
| 2026-03-21 | M1 | ResolvedProfile.fromBaseOnly() 정적 팩토리 메서드 추가 | plan-01 |
| 2026-03-21 | M2 | follow-up.txt 플레이스홀더 통일 ({FOLLOWUP_DEPTH} 1개로) | plan-02 |
| 2026-03-21 | M3 | PersonaResolver base null 방어 + DEFAULT_BASE_PROFILE | plan-01 |
| 2026-03-21 | M5 | "코드 변경 없이 스택 추가" 목표 문구 수정 | requirements |
| 2026-03-21 | D7 | AiClient Parameter Object 설계 판단 추가 | requirements |
| 2026-03-21 | D8 | keyword_usage MVP 미저장 설계 판단 추가 | requirements |

---

## 완료 기록

| 날짜 | Phase | Task | PR | 비고 |
|------|-------|------|----|------|
| - | - | - | - | 구현 시작 전 |
