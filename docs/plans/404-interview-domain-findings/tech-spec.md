# Tech Spec — interview 도메인 발견 이슈 통합 (보안/안정성/cleanup 7건)

> Issue: #404
> product-spec: ./product-spec.md
> 작성일: 2026-05-06
> 승인 게이트: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄)

interview 도메인 7건 (보안 #1/#2/#3 + 안정성 #4/#5 + 운영 #7 + cleanup #8) 단일 PR 머지. #6 (`@Version`) 보류.

## Evidence

- `Interview.java:160` `validateOwner` — `if (this.userId != null && !this.userId.equals(userId))`. NULL row 통과
- `Interview.java:59-62` `@CollectionTable interview_cs_sub_topics`, `cs_sub_topic VARCHAR(50)`, `Set<String>`. enum 부재
- `QuestionGenerationPromptBuilder` line 53/88 `String.join(", ", csSubTopics)` 직접 prompt 주입
- `InterviewController.java:94` `@RequestPart("audio") MultipartFile audioFile` (followUp endpoint). mime / 길이 / 매직바이트 검증 부재
- `AudioTurnAnalyzer.MAX_AUDIO_BYTES = 10MB` 만 존재
- `InterviewService.retryQuestionGeneration` — counter / cooldown 부재. 사전 조건 = `questionGenerationStatus == FAILED` 만
- `ResumeSkeletonEntity` (resume_skeleton 테이블) `interview_id` 1:1. `findByInterviewId` 존재. **하지만 첫 추출 실패 시 영속 X → retry 시 부재**
- `Interview` 엔티티에 `resume_text` / `resume_file_hash` 컬럼 부재 → retry 시 client 인자 / 영속 양쪽 X
- `ResumePlanPreparationService.resolvePlan` — `findByInterviewId` 영속본 그대로 반환. update 경로 부재
- `InterviewErrorCode.CANNOT_DELETE_COMPLETED` — 호출처 0건 (grep)
- 사용자 결정:
  - #6 `@Version` 보류 (별도 결정 후 진행)
  - 동시성 추가 보장 X (#4 counter race / #7 replan 동시성 = last-write-wins 허용)
  - #4 = DB 컬럼 (counter + cooldown), 한도 5회 / cooldown 30초 (외부화)
  - #5 = retry 분리 (skeleton 실패 = 처음부터 안내)
  - AI 한도 도달 시 명시 에러
  - #1 응답 = `INTERVIEW_NOT_FOUND` (FORBIDDEN X)

## Trade-offs

### Option A (채택) — 7건 일괄 PR + 동시성 보장 X
- 장점: 리뷰 한 번. 마이그레이션 1회. 최소 변경
- 단점: #6 보류 → race 시나리오 미해결
- 사유: 사용자 결정 (한 PR + #6 보류)

#### #6 보류 위험 시나리오 (인수)
1. **counter race** (사용자 더블클릭): SELECT 둘 다 `retry_count=4` → 둘 다 5 update → 한도 1회 초과 통과. 손해 = AI 호출 1회 추가. UI 디바운스 권장
2. **status 전이 race** (`updateStatus(COMPLETED)` + retry 동시): retry 가 COMPLETED 덮어씀. 빈도 매우 낮음 (완료 인터뷰 retry 안 함)
3. **replan 동시** (운영자 더블 호출): plan row last-write-wins. 운영자 단일 호출 가정 = 빈도 0

→ 별도 결정 후 #6 (`@Version`) 도입. 보류 기간 위험 인수.

### Option B (폐기) — Phase 분리
- 단점: 사용자 거부 (한 PR)

### Option C (폐기) — 메모리 캐싱 retry counter
- 단점: 다중 인스턴스 / 재시작 시 우회

### Option D (폐기, #5) — Interview 에 resume_text / file_hash 컬럼 추가
- 단점: 마이그레이션 +1, resume 원본 중복 저장. 사용자 X 선택

## Architecture

```
[Controller]                          [Service]                       [Repo / Entity]
InterviewController
  ├ POST /retry-questions          → InterviewService                 interviews
  │                                    ├ AudioValidator (X)              (retry_count, last_retried_at)
  │                                    ├ checkLimitAndCooldown           ├ counter 검증
  │                                    ├ incrementRetry @Transactional   └ counter++ + last_retried_at
  │                                    └ AI 호출 (tx 밖, ResilientAiClient)
  │                                       ├ skeleton 부재 시 거부 (#5)
  │                                       └ RETRY_LIMIT_EXCEEDED / RETRY_COOLDOWN
  │
  ├ POST /interviews/{id}/replan    → ResumePlanPreparationService    interview_plans
  │   (#7 신규)                          └ existing plan update
  │
  ├ POST /follow-up                  → AudioValidator (controller 단)
  │   @RequestPart("audio")              ├ mime whitelist
  │                                       ├ 길이 cap (5분 / 10MB)
  │                                       └ 매직바이트 1차 검증
  │                                    → FollowUpService.generateFollowUp
  │
  └ GET / 조회 모든 경로              → Interview.validateOwner
                                         └ userId NULL 거부 → INTERVIEW_NOT_FOUND (#1)

QuestionGenerationPromptBuilder
  └ csSubTopics : Set<CsSubTopic>  (enum 신규, #2)
```

### `@Transactional` 경계 (#4)
- `incrementRetry(interviewId)` = `@Transactional`. SELECT + counter++ + last_retried_at update. 짧은 tx (DB only).
- `retryQuestionGeneration` = `incrementRetry` 호출 → tx commit → AI 호출 = tx 외부 (긴 lock 방지).
- last-write-wins 허용 (사용자 결정). 락 X.

## Data Model

### Flyway 마이그레이션 (1건)
```sql
-- V{NN}__add_question_gen_retry_to_interviews.sql
ALTER TABLE interviews
  ADD COLUMN question_gen_retry_count INT NOT NULL DEFAULT 0,
  ADD COLUMN question_gen_last_retried_at DATETIME(6) NULL;
```

### Java enum 신규 (#2)
- `CsSubTopic` enum — 운영 DB `interview_cs_sub_topics.cs_sub_topic` 조회 후 사용 중 모든 값 포함하여 정의 (구현 단계 확정).
- `@Enumerated(EnumType.STRING)`, 컬럼 VARCHAR(50) 그대로.
- enum 외 입력 시 400 (`CS_SUB_TOPIC_INVALID`).
- 운영 데이터에 미예상 값 발견 시 backfill SQL 별도 (`UPDATE interview_cs_sub_topics SET cs_sub_topic = 'OTHER' WHERE cs_sub_topic NOT IN (...)`).

### dead code 제거 (#8)
- `InterviewErrorCode.CANNOT_DELETE_COMPLETED` 항목 삭제.

## API Contract

### POST /api/v1/interviews/{interviewId}/replan (신규, #7)
**Request body**: 없음 (path interviewId 만)

**Response (200)**
```json
{ "interviewId": 1, "planId": 42, "regenerated": true }
```

**Error**
- 400 `INTERVIEW_NOT_RESUME_BASED` — RESUME_BASED 외 호출
- 403 / 404 `INTERVIEW_NOT_FOUND` — 본인 인터뷰 아님 / 부재 (구분 X, 보안)
- 409 `RESUME_PLAN_NOT_READY` — skeleton 부재 (replan 불가)

### POST /api/v1/interviews/{id}/retry-questions (기존, #4 / #5 강화)
**Error 추가**
- 429 `RETRY_LIMIT_EXCEEDED` — `question_gen_retry_count >= 5`
- 429 `RETRY_COOLDOWN` — `now - last_retried_at < 30s`
- 409 `INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED` — skeleton 영속본 부재 (#5, 처음부터 다시 안내)

### POST /api/v1/interviews/{id}/follow-up (기존, #3)
**Error 추가**
- 400 `AUDIO_MIME_NOT_ALLOWED` — whitelist (`audio/webm`, `audio/mp4`, `audio/mpeg`, `audio/wav`) 외
- 400 `AUDIO_DURATION_EXCEEDED` — 5분 초과
- 400 `AUDIO_MAGIC_BYTE_MISMATCH` — 헤더 불일치

### 권한 검증 (#1, 모든 경로)
- `validateOwner`: `userId == null || !userId.equals(this.userId)` → `INTERVIEW_NOT_FOUND` (FORBIDDEN X — 존재 사실 노출 차단)

### 외부화 설정 (`application-*.yml`)
```yaml
rehearse:
  interview:
    retry:
      max-attempts: 5
      cooldown-seconds: 30
    audio:
      max-duration-seconds: 300
      max-bytes: 10485760
      mime-whitelist:
        - audio/webm
        - audio/mp4
        - audio/mpeg
        - audio/wav
```

## NF 결정

- 영향 범위: BE only
- 정합성: **동시성 보장 X. counter / status / plan = last-write-wins 허용. #6 보류로 별도 결정 후 추가**
- 실시간성: P95 < 1s
- 부하: 중간. 추가 부하 미미. audio 매직바이트 = stream 첫 N byte (전체 read 금지)
- 동시성: 보장 X. 락 / 큐 도입 X
- 마이그레이션: 스키마 변경 1건 (#4). 백필 = NOT NULL DEFAULT 0 자동. zero-downtime
- 외부 의존: AI (OpenAI/Claude). 한도 도달 시 `RETRY_LIMIT_EXCEEDED` 명시 에러
- 보안: A01 (#1) / A03 (#2 LLM injection) / A03 input (#3) / A04 (#4 rate limit). 회귀 테스트 포함
- 관찰성: 구조적 로그만. 우회 / 한도 / replan = warn/info. 메트릭 / 알람 비스코프
- 롤백: git revert. 컬럼 unused 잔존 허용

## Verification

- [ ] 단위 테스트
  - `Interview.validateOwner` userId NULL 거부
  - `CsSubTopic` enum 변환 + 미정의 값 거부
  - `AudioValidator` mime / 길이 / 매직바이트 각각
  - `InterviewService.retryQuestionGeneration` counter / cooldown 로직
  - `ResumePlanPreparationService.replan` 정상 update
- [ ] 통합 테스트 (Testcontainers + MockMvc)
  - #1 NULL row 다른 user 조회 → 404 `INTERVIEW_NOT_FOUND`
  - #2 enum 외 csSubTopics → 400 `CS_SUB_TOPIC_INVALID`
  - #3 mime 위조 / 5분 초과 / 매직바이트 불일치 → 400
  - #4 한도 초과 → 429 `RETRY_LIMIT_EXCEEDED`. cooldown 미경과 → 429 `RETRY_COOLDOWN`
  - #5 skeleton 부재 + retry → 409 `INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED`
  - #7 replan 호출 → plan row 갱신, 200
- [ ] Flyway 마이그레이션 적용 후 기존 row default=0 확인
- [ ] `./gradlew build` + `./gradlew test` 통과
- [ ] BE CI 그린

## Pre / Post

### Pre
- userId NULL row 권한 우회 가능 (#1)
- csSubTopics 자유 문자열 prompt 주입 (#2)
- audio mime / 길이 / 매직바이트 검증 부재 (#3)
- retryQuestionGeneration 무한 재시도 (#4)
- RESUME_BASED skeleton 추출 실패 후 retry → RESUME_PLAN_NOT_READY 무한 (#5)
- InterviewPlan replan 경로 부재 (#7)
- `CANNOT_DELETE_COMPLETED` dead code (#8)

### Post
- `validateOwner` userId NULL 거부, `INTERVIEW_NOT_FOUND` 응답
- `CsSubTopic` enum, prompt 안전
- `AudioValidator` controller 단 mime / 길이 / 매직바이트 검증
- `interviews.question_gen_retry_count` + `question_gen_last_retried_at` 컬럼. 한도 5회 / cooldown 30초 (외부화)
- skeleton 부재 retry = `INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED` 명시 거부 (사용자 처음부터)
- `POST /api/v1/interviews/{id}/replan` 신규
- `CANNOT_DELETE_COMPLETED` 제거

## 위험 / 마이그레이션 / 롤백

- 위험: #6 보류 = counter / status / replan race 미해결. UI 디바운스 권장. 보류 기간 lost update / lost-status 가능성 인수
- 마이그레이션 전략: zero-downtime. 컬럼 추가 (NOT NULL DEFAULT 0) → 신규 코드 deploy → 기존 코드와 호환 (DEFAULT 처리)
- 롤백: git revert. 컬럼 unused 잔존 허용. DB 데이터 손상 X

## 분기 결정

- [x] 단일 영역 (BE only) → `implement.md` 1개
- [ ] BE+FE 동시
- [ ] BE 선행 강제
