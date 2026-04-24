# Feedback Domain — Session Feedback 관계 결정

> 상태: Completed
> 작성일: 2026-04-24 (plan-00e S3b 실행)
> 범위: plan-09 `SessionFeedback` 과 기존 `TimestampFeedback` / `QuestionSetFeedback` 의 관계·동시성·API 경계 확정
> 소비자: plan-09 (Feedback Synthesizer)
> 코드 변경: **없음** (본 문서는 결정 문서)

## 배경

plan-09의 `SessionFeedback`(5섹션 세션 종합)은 기존 Feedback 도메인과 **완전히 다른 스키마**다. critic M6 지적:

- 대체인가, 병존인가?
- FE 가 새 구조를 인식 안 하면 "생성됐는데 안 보이는" 기간 발생
- Verbal/Vision(Lambda 비동기)이 세션 종료 시점에 미완료면?

plan-09 범위를 넘는 **도메인 설계 결정**이므로 Phase 0 에서 먼저 합의한다. plan-09 는 이 결정을 소비만.

## 현행 Feedback 도메인 상태 (2026-04-24 실측)

`backend/src/main/java/com/rehearse/api/domain/feedback/` 구조:
- `controller/FeedbackController.java` — 사용자 피드백 조회 API
- `service/FeedbackService.java` — 기존 피드백 생성/조회 서비스
- `service/TimestampFeedbackMapper.java`
- `entity/TimestampFeedback.java` — 타임스탬프 싱크 영상 피드백 (서비스 차별점)
- `entity/QuestionSetFeedback.java` — 질문셋 단위 피드백
- `repository/{TimestampFeedbackRepository, QuestionSetFeedbackRepository}.java`
- `dto/{QuestionSetFeedbackResponse, TimestampFeedbackResponse, SaveFeedbackRequest, ...}`
- `exception/`

→ **`SessionFeedback` 은 이 도메인의 새 서브패키지(`session/`)로 추가**. 기존 entity/service/repository 는 건드리지 않음.

## 결정

### 결정 1 — 병존 (대체 아님)

**선택**: `SessionFeedback`(신규)과 `TimestampFeedback` / `QuestionSetFeedback`(기존)은 **별도 aggregate 로 병존**.

**이유**:
- `TimestampFeedback` 은 **타임스탬프 싱크 영상 피드백** — 서비스 차별점. 제거/대체 시 핵심 가치 훼손.
- `SessionFeedback` 은 **세션 전체에 대한 구조화 리포트** — 기존에 없던 층위. 보완재.
- FE 는 두 피드백을 다른 탭/섹션에서 보여줌 (FE 별도 PR 에서 결정).

**관계**:
```
Interview (1) ─┬─ (N) QuestionSetFeedback   [기존, 유지]
               │        └─ (N) TimestampFeedback  [기존, 유지]
               └─ (1) SessionFeedback        [신규, plan-09 / Flyway V27]
```

### 결정 2 — 동시성 / 비동기 처리 (partial-first)

**문제**: 세션 종료 시점에 Lambda(Delivery/Vision) 분석이 아직 미완료일 수 있음.

**선택**: `SessionFeedback` 은 **partial-first** 로 생성.

1. 세션 종료 이벤트 발생 → `SessionFeedbackService.synthesizePreliminary()` 즉시 실행 (Delivery/Vision 없이 Content 피드백만)
2. 사용자에게 "Delivery 섹션은 분석 완료 후 업데이트됩니다" 표시 (FE 영역)
3. Delivery/Vision 이벤트 도달 시 `SessionFeedbackService.enrichDelivery()` 호출 → Delivery 섹션 업데이트
4. `SessionFeedback.status` enum: `PRELIMINARY | COMPLETE`

**폴백 타임아웃**: Delivery/Vision 이 **10 분** 내 미도착 시 Delivery 섹션 `null` 유지, status 는 `COMPLETE` 로 전환 (무한 대기 방지).

**재시도 정책** (2026-04-21 VERIFICATION_REPORT 반영):
- Lambda 실패는 **모두 재시도 가능** (`deliveryRetryable = true` 기본, 영구 실패 플래그 없음)
- Admin 수동 재처리 엔드포인트로 `enrichDelivery()` 재호출
- 사용자에게는 "일시 오류" 만 노출 (영구 실패 표시 금지)

### 결정 3 — FE 연동 전 Admin API 제공

**문제**: "FE 연동은 별도 PR" 기간 동안 새 피드백이 생성돼도 확인 불가.

**선택**: plan-09 에 `GET /api/admin/interviews/{id}/session-feedback` Admin API 포함. 운영자 / QA 가 raw JSON 확인 가능.

엔드포인트 패키지: `domain/feedback/session/controller/AdminSessionFeedbackController.java`.

### 결정 4 — `InterviewCompletedEvent` 신규 도입 (2026-04-24 교정)

**실측 결과** (`grep -rn "InterviewCompletedEvent" backend/src/main/java/` → 0건):
- 기존 코드에 `InterviewCompletedEvent` 자체가 **없음**
- 기존 `FeedbackService.onInterviewCompleted()` 리스너도 **없음**
- 현행 Feedback 생성은 Lambda 이벤트 핸들러 경로로 직접 호출 (이벤트 디스패치 없음)

**결정**:
- plan-09 범위에서 **신규 `InterviewCompletedEvent` 도입** (Spring `ApplicationEventPublisher` 경유)
- 발행 위치: `domain/interview/service/InterviewCompletionService` 의 완료 처리 메서드
- 리스너: **단일 `SessionFeedbackService.onInterviewCompleted()` 신규 도입** (기존 Feedback 생성 경로와 독립)
- 기존 Lambda 이벤트 핸들러 경로는 **건드리지 않음** → `TimestampFeedback` / `QuestionSetFeedback` 생성은 현행 유지

```
InterviewCompletionService.complete(interview)
   ├─ 기존 로직 (DB 커밋)
   └─ applicationEventPublisher.publishEvent(new InterviewCompletedEvent(interview.getId()))
            └─ (신규) SessionFeedbackService.onInterviewCompleted() — 5섹션 리포트 생성 (partial-first)
```

**격리**: `@TransactionalEventListener(phase = AFTER_COMMIT)` 로 커밋 후 리스너 실행 → 리스너 실패가 원 트랜잭션에 영향 없음.

### 결정 5 — 패키지 경로 (aa88a96 리팩터 반영)

```
backend/src/main/java/com/rehearse/api/domain/feedback/
├── (기존) controller/      — FeedbackController 유지
├── (기존) service/         — FeedbackService 유지
├── (기존) entity/          — Timestamp/QuestionSet Feedback 유지
├── (기존) repository/
├── (기존) dto/
├── (기존) exception/
└── (신규) session/         ← plan-09 에서 생성
    ├── SessionFeedbackService.java
    ├── entity/SessionFeedback.java
    ├── repository/SessionFeedbackRepository.java
    ├── dto/SessionFeedbackResponse.java
    └── controller/AdminSessionFeedbackController.java
```

**불침습 원칙**: plan-09 는 기존 `feedback/{controller,service,entity,repository,dto,exception}` 파일을 **수정하지 않음**. 신규 `session/` 서브패키지만 추가.

### 결정 6 — 점진 전환 방식 (2026-04-23 Runtime Flag 철거 반영)

- flag 기반 점진 전환 **없음**. 신규 스키마는 배포 시 **완전 전환**
- 기존 `FeedbackService` 경로는 그대로 유지되므로 신규 `SessionFeedbackService` 리스너 추가는 병존이 아닌 **추가 계층 도입**
- Runtime feature flag 메커니즘은 **2026-04-23 결정으로 전면 폐기** (ECR 이미지 롤백으로 대체)
- 롤백 전략: ECR 이전 태그 재배포 + Caffeine 세션 스토어 캐시 퍼지

→ plan-09 구현 시 `rehearse.features.*` 신규 flag **도입 금지**. `rehearse.feedback-synthesizer.model` 같은 **설정 값** 은 허용(기본값 고정, runtime toggle 없음).

## plan-09 연계 체크리스트

plan-09 구현자는 아래를 **이 문서 참조** 로 처리:

- [x] `SessionFeedback` 은 별도 aggregate (결정 1)
- [x] `status: PRELIMINARY | COMPLETE` + 10 분 타임아웃 (결정 2)
- [x] `deliveryRetryable = true` 기본, 영구 실패 금지 (결정 2)
- [x] `AdminSessionFeedbackController` 포함 (결정 3)
- [x] `InterviewCompletedEvent` **신규 생성** + `InterviewCompletionService` 발행 (결정 4)
- [x] `@TransactionalEventListener(AFTER_COMMIT)` 사용 (결정 4)
- [x] `domain/feedback/session/` 서브패키지 경로 준수 (결정 5)
- [x] 기존 Feedback 도메인 public API 불변 (결정 5)
- [x] Runtime flag 금지, 설정 값만 허용 (결정 6)

## 검증

1. 본 문서에 결정 1~6 명시 ✅
2. plan-09 Context 섹션에 본 문서 앵커 존재 (`plan-00e FEEDBACK_DOMAIN.md 결정 소비`, plan-09-feedback-synthesizer.md:15)
3. 기존 Feedback 도메인 public API 변경 0 — plan-09 PR 리뷰 시 `architect-reviewer` 가 최종 확인
4. `InterviewCompletedEvent` 실측 부재 → plan-09 에서 신규 도입으로 교정됨 (결정 4)
5. `progress.md` 00e → `Draft → Completed`

## Out of Scope

- FE 세션 피드백 UI 설계 (별도 FE PR)
- `QuestionSetFeedback` 구조 개편 (기존 유지)
- `TimestampFeedback` → `SessionFeedback` 마이그레이션 (병존 결정에 따라 영구적으로 실행 안 함)
- Admin 대시보드 UI (plan-09 는 JSON API 만)
