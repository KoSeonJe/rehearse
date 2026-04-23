# Plan 00e: Feedback Migration Strategy (Phase 0) `[parallel:00d]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W2 후반 (1-2일)
> 해결 RC: RC2 중 M6(기존 `TimestampFeedback`/`QuestionSetFeedback`과 신규 `SessionFeedback` 관계)

## Why

plan-09의 `SessionFeedback`(5섹션 구조화)은 기존 Feedback 도메인과 **완전히 다른 스키마**다. critic M6 지적:
- 대체인가, 병존인가?
- FE가 새 구조를 인식 안 하면 "생성됐는데 안 보이는" 기간 발생
- Verbal/Vision(Lambda 비동기)이 세션 종료 시점에 미완료면?

이 결정은 plan-09 범위를 넘는 **도메인 설계 결정**이므로 Phase 0에서 먼저 합의한다. plan-09는 이 결정을 소비만.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/plans/interview-quality-2026-04-20/FEEDBACK_DOMAIN.md` | 신규. 결정 문서 |

(이 플랜은 **코드 변경 없음**. 문서 결정만. plan-09에서 실제 구현.)

## 상세 — 결정 내용

### 결정 1: 병존 (대체 아님)

**선택**: `SessionFeedback`(신규)과 `TimestampFeedback`/`QuestionSetFeedback`(기존)은 **별도 aggregate로 병존**.

**이유**:
- `TimestampFeedback`은 **타임스탬프 싱크 영상 피드백** — 서비스 차별점. 제거/대체 시 핵심 가치 훼손.
- `SessionFeedback`은 **세션 전체에 대한 구조화 리포트** — 기존에 없던 층위. 보완재.
- FE는 두 피드백을 다른 탭/섹션에서 보여줌 (추후 FE plan에서 결정).

**관계**:
```
Interview (1) ─┬─ (N) TimestampFeedback    [기존, 유지]
               ├─ (N) QuestionSetFeedback  [기존, 유지]
               └─ (1) SessionFeedback      [신규, plan-09/V27]
```

### 결정 2: 동시성/비동기 처리

**문제**: 세션 종료 시점에 Lambda(Verbal/Vision) 분석이 아직 미완료일 수 있음.

**선택**: `SessionFeedback`은 **partial-first**로 생성.
1. 세션 종료 이벤트 발생 → `SessionFeedbackService.synthesizePreliminary()` 즉시 실행 (Verbal/Vision 없이 기술 피드백만)
2. 사용자에게 "Delivery 섹션은 분석 완료 후 업데이트됩니다" 표시(FE 영역)
3. Verbal/Vision 이벤트 도달 시 `SessionFeedbackService.enrichDelivery()` 호출 → Delivery 섹션 업데이트
4. `SessionFeedback.status` enum: `PRELIMINARY | COMPLETE`

**폴백 타임아웃**: Verbal/Vision이 10분 내 미도착 시 Delivery 섹션 `null` 유지, status는 `COMPLETE`로 전환(무한 대기 방지).

### 결정 3: FE 연동 전 Admin API 제공

**문제**: "FE 연동은 별도 PR" 기간 동안 새 피드백이 생성돼도 확인 불가.

**선택**: plan-09에 `GET /api/admin/interviews/{id}/session-feedback` Admin API 포함. 운영자/QA가 raw JSON 확인 가능.

### 결정 4: 기존 피드백 생성 로직은 건드리지 않음

plan-09는 **기존 `FeedbackService.java`의 Lambda 이벤트 핸들러 경로를 수정하지 않는다**. 대신 별도 `SessionFeedbackService`를 신설하고, 세션 종료 이벤트(`InterviewCompletedEvent`)의 새 리스너로 붙는다.

```
InterviewCompletedEvent
   ↓ (기존) FeedbackService.onInterviewCompleted() — TimestampFeedback 생성 트리거
   ↓ (신규) SessionFeedbackService.onInterviewCompleted() — 5섹션 리포트 생성
```

두 리스너는 독립. 하나 실패해도 다른 하나 영향 없음.

### 결정 5: 스키마 경로

- `backend/src/main/java/com/rehearse/api/domain/feedback/` 하위에 `session/` 서브패키지 신설
  - `session/SessionFeedback.java` (Entity)
  - `session/SessionFeedbackService.java`
  - `session/dto/SessionFeedbackResponse.java`
  - `session/repository/SessionFeedbackRepository.java`
- 기존 Feedback 도메인 파일은 건드리지 않음 → 소유권 분리

### 결정 6: 점진 전환 방식 (2026-04-23 갱신)

flag 기반 점진 전환 없이 **신규 스키마는 배포 시 완전 전환**한다. 기존 `FeedbackService` 리스너 경로는 그대로 유지되므로 신규 `SessionFeedbackService` 리스너 추가는 병존이 아닌 추가 계층 도입이다. Runtime feature flag 메커니즘은 2026-04-23 결정으로 전면 폐기 (ECR 이미지 롤백으로 대체).

## 담당 에이전트

- Implement: `backend-architect` — 도메인 경계 결정 작성
- Review: `architect-reviewer` — 기존 Feedback aggregate 불침습 보장

## 검증

1. FEEDBACK_DOMAIN.md에 결정 1~5 모두 명시
2. plan-09 문서에서 본 문서를 참조하도록 업데이트됨 (REMEDIATION.md의 "기존 plan 수정 지시"에 포함)
3. 기존 Feedback 도메인 public API의 변경이 전혀 없음을 코드 리뷰로 확인
4. `InterviewCompletedEvent`가 실제로 현재 코드에 존재하는지 plan-00a 인벤토리에서 확인(없다면 plan-00e에서 도입 결정 추가)
5. `progress.md` 00e → Completed
