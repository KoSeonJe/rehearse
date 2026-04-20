# Plan 06: Resume Interview Planner (Phase 2) `[parallel:05]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W3
> 원본: `docs/todo/2026-04-20/06-resume-track.md` (Phase 2)

## Why

plan-05가 뽑은 Resume Skeleton 전체를 세션 내내 떠먹이면 3가지 문제: (a) 10개 프로젝트 × 수십 chain이 주의력 분산시킴, (b) 시간 배분 없이 두 번째 프로젝트 못 들어감, (c) 난이도 곡선 없이 기계적 진행.

세션 시작 시 **Planner가 1회** 돌면서 "오늘 다룰 2-3개 프로젝트 + 8-12개 chain + 시간 배분 + 난이도 곡선"을 결정해 Plan JSON으로 고정하면, 매 턴 Orchestrator(plan-07)는 "이 plan 따라 진행 중인가"만 판단하면 됨 → 일관성 + 시간 통제.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeInterviewPlannerPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ResumeInterviewPlanner.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/InterviewPlan.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ProjectPlan.java` | 신규 (playground_phase + interrogation_phase) |
| `backend/src/main/java/com/rehearse/api/domain/interview/InterviewSession.java` | 수정. `InterviewPlan` 참조 추가 |

## 상세

### 입력
- `RESUME_SKELETON` (plan-05 결과)
- `SESSION_DURATION_MIN` (기본 60)
- `USER_LEVEL` (Skeleton에서 추정)

### 출력 스키마
```json
{
  "session_plan_id": "plan_...",
  "total_projects": 2,
  "estimated_duration_min": 55,
  "project_plans": [{
    "project_id": "p1",
    "project_name": "...",
    "allocated_time_min": 25,
    "playground_phase": {
      "opener_question": "...",
      "expected_claims_coverage": ["p1_c1", "p1_c2", "p1_c3"],
      "max_turns": 3
    },
    "interrogation_phase": {
      "primary_chains": [{"chain_id": "p1_chain_...", "topic": "...", "priority": 1, "levels_to_cover": [1,2,3,4]}],
      "backup_chains": [{"chain_id": "...", "priority": 3}]
    }
  }]
}
```

### 선정 원칙 (프롬프트 내 명시)
1. **프로젝트 2-3개**: 최근 + high priority claims 밀도 + 기술 스택 다양성
2. **Chain 선정 per 프로젝트**: opener 1 + primary 2-3 + backup 1-2
3. **난이도 분배**: 초반 = USER_LEVEL → 중반 = +0.5 → 후반 = USER_LEVEL (마무리 편안)
4. **시간 배분 예시 (60분)**: 놀이터 10-15분 / 심문 30-45분 / 여유 10분

### 모델 파라미터
- Primary: GPT-4o-mini / Fallback: Claude Haiku (고정된 Skeleton에서 추출만 하므로 경량 모델 충분)
- temperature: 0.3
- max_tokens: 2048

### Plan의 역할
세션 전체의 "북극성". plan-07 Orchestrator가 매 턴 진행 상황을 plan에 기록하면서 진행.

## 담당 에이전트

- Implement: `backend` — Planner 서비스 + 도메인 모델
- Review: `architect-reviewer` — Plan/Session/Skeleton 세 도메인 객체 간 의존 방향(Skeleton ← Plan ← Session)

## 검증

1. Skeleton 10개에서 Plan 생성 성공률 100%
2. 각 Plan의 `estimated_duration_min` ≤ 요청 `SESSION_DURATION_MIN`
3. `primary_chains`의 `chain_id`가 Skeleton의 실제 chain과 매칭(고아 참조 0건)
4. `expected_claims_coverage` ⊆ Skeleton의 실제 claims
5. 생성된 Plan이 `InterviewSession`에 정상 직렬화 (JSONB 저장 or 캐시)
6. plan-07과 통합 테스트: Orchestrator가 Plan을 읽어 Playground opener 출력
7. `progress.md` 06 → Completed
