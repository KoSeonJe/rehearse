# Plan 06: Resume Interview Planner (Phase 2) `[parallel:05]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W3
> 원본: `docs/todo/2026-04-20/06-resume-track.md` (Phase 2)

## Why

plan-05가 뽑은 Resume Skeleton 전체를 세션 내내 떠먹이면 3가지 문제: (a) 10개 프로젝트 × 수십 chain이 주의력 분산시킴, (b) 항상 첫 프로젝트에 매몰되어 다른 프로젝트 진입 못 함, (c) 우선순위 없이 기계적 진행.

세션 시작 시 **Planner가 1회** 돌면서 "프로젝트 priority 랭킹 + 프로젝트별 primary/backup chain 랭킹"을 Plan JSON으로 고정하면, 매 턴 Orchestrator(plan-07)는 "현재 위치에서 다음 무엇을 소비할지"만 판단하면 됨 → 일관성 확보.

### Dynamic Pacing — 최대 준비 + 실시간 마무리 (2026-04-22)

**원칙**: Planner 는 **시간(duration) 기준으로 자르지 않는다**. 대신 모든 chain 을 priority 로 랭킹만 한다. 실제 어디까지 소화할지는 **Orchestrator(plan-07) 가 사용자 페이스에 맞춰 런타임에 결정**한다.

**이유**:
- 15/30/45/60 분 별 스케일링 테이블을 Planner 에 박으면 정책 4개 × chain 조합 매트릭스가 복잡해짐
- 더 근본적으로 **사용자 답변 페이스에 적응 못 함** — 예상보다 빠르면 시간 남고, 느리면 어색한 컷오프
- Skeleton 추출 비용은 duration 무관 (이력서당 1회) → 최대 준비해도 비용 증가 없음
- backup_chains 의 진짜 존재 이유가 이 경우. 사용자가 빨라서 primary 다 끝나면 backup 으로 자연 확장

**Planner 출력 변경**:
- `allocated_time_min` 필드 **제거** (Orchestrator 가 실시간 판단)
- `project_plans[].max_turns` 제거 (WRAP_UP 모드가 종료 책임)
- `duration_hint_min` 신규 — opener 톤 조정용 (15분: "짧게 핵심만", 60분: "편하게 소개부터")
- 모든 프로젝트·chain 포함 (자르지 않음). `priority` 로만 소비 순서 결정

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeInterviewPlannerPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ResumeInterviewPlanner.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/InterviewPlan.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ProjectPlan.java` | 신규 (playground_phase + interrogation_phase) |
| `backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeState.java` | 수정 (plan-00c 산출). `InterviewPlan` 참조 필드 추가. (`InterviewSession` 클래스는 실재하지 않음 — INVENTORY.md:107) |

## 상세

### 입력
- `RESUME_SKELETON` (plan-05 결과)
- `SESSION_DURATION_MIN` (기본 60)
- `USER_LEVEL` (Skeleton에서 추정)

### 출력 스키마
```json
{
  "session_plan_id": "plan_...",
  "duration_hint_min": 30,
  "total_projects": 3,
  "project_plans": [{
    "project_id": "p1",
    "project_name": "...",
    "priority": 1,
    "playground_phase": {
      "opener_question": "...",
      "expected_claims_coverage": ["p1_c1", "p1_c2", "p1_c3"]
    },
    "interrogation_phase": {
      "primary_chains": [{"chain_id": "p1_chain_...", "topic": "...", "priority": 1, "levels_to_cover": [1,2,3,4]}],
      "backup_chains": [{"chain_id": "...", "priority": 3}]
    }
  }]
}
```

> `allocated_time_min` / `max_turns` / `estimated_duration_min` 필드는 Dynamic Pacing 전환으로 제거 (2026-04-22). `priority` 만으로 소비 순서 결정.

### 선정 원칙 (프롬프트 내 명시)
1. **프로젝트 랭킹**: 최근 + high priority claims 밀도 + 기술 스택 다양성 순으로 priority 부여 (상한 없음)
2. **Chain 선정 per 프로젝트**: opener 1 + primary 2-3 + backup 1-2 (모두 포함, 자르지 않음)
3. **난이도 곡선**: 초반 chain = USER_LEVEL → 중반 = +0.5 → 후반 backup = USER_LEVEL (마무리 편안)
4. **duration_hint 활용**: opener 질문 톤·길이만 조정. chain 선택/차단에는 **사용하지 않음** (Orchestrator 책임)

### 모델 파라미터
- Primary: GPT-4o-mini / Fallback: Claude Haiku (고정된 Skeleton에서 추출만 하므로 경량 모델 충분)
- temperature: 0.3
- max_tokens: 2048

### Plan의 역할
세션 전체의 "북극성". plan-07 Orchestrator가 매 턴 priority 순으로 chain 을 소비하며 `ClockWatcher` 로 남은 시간을 추적. `remaining_time ≤ 2분` 시점에 WRAP_UP 모드 전이 (자세한 규약은 plan-07 참조).

## 담당 에이전트

- Implement: `backend` — Planner 서비스 + 도메인 모델
- Review: `architect-reviewer` — Plan/Session/Skeleton 세 도메인 객체 간 의존 방향(Skeleton ← Plan ← Session)

## 검증

1. Skeleton 10개에서 Plan 생성 성공률 100%
2. Plan 에 `allocated_time_min` / `max_turns` / `estimated_duration_min` 필드 **부재** (Dynamic Pacing 규약)
3. `primary_chains`의 `chain_id`가 Skeleton의 실제 chain과 매칭(고아 참조 0건)
4. `expected_claims_coverage` ⊆ Skeleton의 실제 claims
5. `project_plans` 가 `priority` 오름차순 정렬 + 모든 프로젝트 포함 (잘리지 않음)
6. `duration_hint_min` 값이 요청 SESSION_DURATION_MIN 과 일치
7. 생성된 Plan이 `InterviewRuntimeState` 또는 V25 `interview_plan` 테이블(plan-00c)에 정상 저장 (JSONB 또는 캐시)
8. plan-07과 통합 테스트: Orchestrator가 Plan을 읽어 Playground opener 출력
9. `progress.md` 06 → Completed
