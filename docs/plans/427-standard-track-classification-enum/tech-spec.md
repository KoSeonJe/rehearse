# Tech Spec — STANDARD 트랙 분류 메타 enum 단일 출처화 + 컬럼 정규화

> **작성자**: 구현 agent — Staff Engineer 페르소나 (`/create-tech-spec` 스킬)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement-*.md 진입 ★
> **product-spec**: `./product-spec.md`
> **Issue**: #427 (Epic, P2)

---

## Why → Goal (1줄 미러)

LLM 응답과 도메인 코드 두 출처로 분산된 STANDARD 트랙의 referenceType / feedbackPerspective 를 `QuestionType` enum 단일 출처로 환원하고 question / question_pool 의 분류 메타 컬럼을 제거한다.

## Evidence

- **현재 구조**:
  - Enum: `QuestionType.java:6-11` — `MAIN(null,null)` / `FOLLOWUP(null,null)` sentinel + `RESUME_*` 4종 매핑.
  - Entity: `Question.java:38-44, 75` (`reference_type` / `feedback_perspective` 컬럼 적재) + 팩토리 `Question.resume()` (line 75 — RESUME_* 만 허용 가드).
  - Assembler: `QuestionSetAssembler.java:42-51` — STANDARD 신규 Question 적재 시 `questionType=MAIN` 고정 + perspective 도메인 계산 + referenceType LLM 결과 그대로 (이중 출처 핵심).
  - Follow-up 분기: `FollowUpTransactionHandler.java:67-110` — 메인 질문의 referenceType 으로 follow-up prompt 모드 분기 (MODEL_ANSWER → CONCEPT 모드, GUIDE → EXPERIENCE 모드). **데이터가 아닌 도메인 로직 결합**. 또한 line 99/110 에서 `QuestionType.FOLLOWUP` 직접 비교 + 빌더에 직접 사용 (Phase 1 enum 환원 영향 직접 영역).
  - Track Policy (FOLLOWUP enum 직접 비교 — Phase 1 환원 필수, 위치 = `domain/interview/service/`):
    - `StandardFollowUpPolicy.java:35` — `q.getQuestionType() == QuestionType.FOLLOWUP` count → `maxRounds(2)` cap.
    - `ResumeTrackPolicy.java:29` — 동일 비교 → `HARD_TURN_CAP(7)` cap. RESUME 트랙은 FOLLOWUP enum 미사용 → count 항상 0 → **dead code**. RESUME 종료 제어는 ChainStateTracker / ResumeModeTransitionPolicy / ClockWatcher 가 별도 보장.
  - DTO: `QuestionDetailResponse.java:18,28` (`referenceType`), `domain/question/dto/AnswerResponse.java:23-33` (`feedbackPerspective` 문자열).
  - Rubric: `RubricLoader.java:71`, `RubricFamily.java:54-58` — `feedbackPerspective` 로 매핑 (출처 변경만 — 값 동등).
  - LLM schema: `prompts/template/question-generation.txt:37-39, 71-74`, `GeneratedQuestion.java:30` (`@JsonProperty("reference_type")`), `MockAiClient.java:50-54`.
  - Migration 이력: V4 (`reference_type` 최초), V11 (`question_pool.reference_type`), V16 (`feedback_perspective`), V21 (`question_set.category` = InterviewType.name), V42 (RESUME meta drop), V44 (RESUME 통일).
  - Seed 18종: behavioral-* = `GUIDE`, cs-* / system-design-* / backend-* / frontend-* / devops-* / fullstack-* = `MODEL_ANSWER`. → referenceType 은 InterviewType 종속.
  - FE: `frontend/src/types/interview.ts:57,71` (type + 필드), `frontend/src/hooks/use-answer-flow.ts:378` (단일 하드코딩).
  - Lambda: `lambda/analysis/handler.py:205-218, 233-246` — `feedbackPerspective` payload (TECHNICAL default).
- **외부 레퍼런스**: `backend/.claude/rules/conventions.md` "Flyway = DDL only, DML 금지" — 백필 SQL 은 Flyway 미관여 운영 SQL 분리 (사용자 결정).
- **사용자 발화 / 결정**:
  - enum 분할 = TECH/BEHAVIORAL 2계열 (perspective 1차원 분류로 충분, YAGNI)
  - phase 분리 (P1 / P2 / P3)
  - FE `ReferenceType` type + `QuestionDetail.referenceType` + `use-answer-flow.ts:378` 하드코딩 동시 제거
  - question_pool `reference_type` 컬럼 + 시드 18개 정리도 본 Epic 포함
  - 백필 = 운영 SQL 분리 수동 실행
  - Phase 1 = path 전환만 (column 적재 중단)
  - 기존 `MAIN`/`FOLLOWUP` enum = Phase 1 백필 직후 제거
  - `QuestionDetailResponse.referenceType` 응답 필드 = Phase 3 에서 제거
  - **Phase 3 머지 순서 = FE 선행 → BE 후행** (FE 가 dead code 제거라 BE 응답 referenceType 잔존해도 무해. TS strict 충돌 자국 회피)
  - **`ResumeTrackPolicy` 정리 = 본 Epic 포함** (FOLLOWUP enum 제거 시 컴파일 깨짐 + RESUME 트랙 dead code 표면화)
- **추정 / 미확인 가정**:
  - 운영 기존 STANDARD row 백필 매핑 정확성 = `question_set.category` (InterviewType) 100% 단서. category=BEHAVIORAL → BEHAVIORAL_*, 그 외 → TECH_*. RESUME_BASED 카테고리 + questionType=MAIN 잔여 row 0건 가정 (V44 가 사전 제거). 실 DB 검증 필요.
  - 운영 row 수 ≤ 수만 추정 (서비스 규모) → 백필 SQL 1회성 부하 무시 가능.
  - LLM 응답 호환 — Phase 2 schema 변경 직후 GeneratedQuestion.referenceType 필드 제거. LLM 이 여전히 `reference_type` 출력해도 Jackson 이 무시 (UNKNOWN_PROPERTIES ignore 가정 — 코드 상 `@JsonIgnoreProperties(ignoreUnknown=true)` 또는 ObjectMapper 설정 확인 implement 단계).

## Trade-offs

### Option A (채택): Phase 분리 PR (3단계) + Java 마이그레이션 미사용 + 운영 SQL 백필
- 장점:
  - DDL 비가역 위험을 Phase 2 로 격리 — Phase 1 머지 후 회귀 확인 시간 확보
  - 백필 = 운영 SQL 1회성 → Flyway DDL-only 룰 준수
  - 롤백 단위 작음 (Phase 별)
  - BE Phase 1 머지 직후 FE / Lambda 병렬 진행 가능
- 단점:
  - Phase 1 단계에서 column 잔존 (적재 X / 읽기 X) — dead column 일시 노출
  - 운영 SQL 수동 실행 = 환경별 누락 가능성 (사전 체크리스트로 보완)
- 채택 사유: 데이터 손실 / 회귀 면적 동시 노출 회피. 사용자 결정 (phase 분리 + 운영 SQL 분리) 정합.

### Option B (폐기): 단일 PR + Flyway 백필 (DML 위반)
- 장점: 빠른 일괄 적용 / 코드 일관성 유지 기간 짧음
- 폐기 사유: (1) DDL DROP 비가역 — 회귀 발견 시 롤백 곤란. (2) Flyway DML 도입 = `conventions.md` 위배 (룰 일관성 약화). (3) 3 영역 (BE/FE/Lambda) 회귀 면적 동시 노출.

### Option C (참고 — 폐기): InterviewType 세분 enum (24종)
- 장점: 분류 그라울러리티 최대
- 폐기 사유: perspective 1차원 분류로 follow-up 모드 분기 충분 (YAGNI). enum 폭증 + 백필 매핑 비용 ↑.

## Architecture

### 핵심 변경 시퀀스

```
[Phase 1 — BE 단일 PR]
QuestionType enum 확장 (MAIN/FOLLOWUP 보존 — enum 동시 비교 안정성 확보용)
  + TECH_MAIN(MODEL_ANSWER, TECHNICAL)
  + TECH_FOLLOWUP(MODEL_ANSWER, TECHNICAL)
  + BEHAVIORAL_MAIN(GUIDE, BEHAVIORAL)
  + BEHAVIORAL_FOLLOWUP(GUIDE, BEHAVIORAL)
  + helper: QuestionType.isFollowUp() / isMain() / isResume()
   ↓
QuestionSetAssembler.fromGenerated/fromPool — category 기반 questionType 결정
   QuestionSetCategory.BEHAVIORAL → BEHAVIORAL_MAIN
   그 외 → TECH_MAIN
   ↓ (referenceType / feedbackPerspective column 적재 중단 — null 전달)
FollowUpTransactionHandler.saveFollowUpResult (line 99, 110)
   메인 질문의 questionType 으로 sub-type FOLLOWUP 결정
   (TECH_MAIN → TECH_FOLLOWUP / BEHAVIORAL_MAIN → BEHAVIORAL_FOLLOWUP / RESUME_* → 기존 path)
   FOLLOWUP enum 직접 비교 → questionType.isFollowUp() 환원
   ↓
FollowUpTransactionHandler.resolveMainReferenceType (line 81-91) — questionType.referenceType() 환원
   ※ NPE 방어 (Phase 1 한시): MAIN/FOLLOWUP enum 보존 동안 referenceType()=null 인 sentinel row 만나면 **questionSet.category 추론 fallback**:
     • category = BEHAVIORAL → GUIDE
     • 그 외 (CS / SystemDesign / Backend / Frontend / DevOps / Fullstack / RESUME_BASED) → MODEL_ANSWER
   사유: 단순 null → MODEL_ANSWER fallback 시 잔존 BEHAVIORAL row 가 CONCEPT 모드로 잘못 분기 (실제 = EXPERIENCE / GUIDE) → follow-up prompt 모드 결함. category 추론으로 안전 보장.
   백필 완료 + enum 제거 시점에 fallback 도 제거.
   ↓
StandardFollowUpPolicy.assertCanContinue (line 35) — FOLLOWUP enum 비교 → questionType.isFollowUp() 환원
   ↓
ResumeTrackPolicy.assertCanContinue (line 28-30) — dead code 정리
   • Option (구체 = implement 단계): (a) `assertCanContinue` 빈 구현 (no-op — 종료 제어를 ChainStateTracker 일임) / (b) `RESUME_INTERROGATION` count 로 교체 (cap 7 의미 유지). 기본은 (a) 권장.
   ↓
Question.resume() 팩토리 가드 (Question.java:75) — 신규 enum sub-type 환원 (RESUME_* 만 허용 가드 보존, MAIN/FOLLOWUP enum 제거 시 가드 단순화)
   ↓
DTO from(question) — question.getQuestionType().referenceType() / .feedbackPerspective() 환원
   (DB column 무시 — 적재되어 있어도 enum 값 우선)
   ※ NPE 방어 (Phase 1 한시): 백필 미실행 row 잔존 시 (null,null) 매핑 → category 추론 fallback 동일 적용 (BEHAVIORAL → GUIDE / BEHAVIORAL perspective 적용, 그 외 → MODEL_ANSWER / TECHNICAL). 환원 헬퍼 (`QuestionType.referenceTypeOrFallback(QuestionSetCategory)` / `.feedbackPerspectiveOrFallback(QuestionSetCategory)`) 신설하여 Handler / DTO 공용. enum 제거 시 헬퍼도 제거.
   ↓
운영 SQL 1회성 백필 (사용자 수동 실행 — Phase 1 코드 머지 후, dev → prod 순)
   UPDATE question SET question_type = CASE
     WHEN qs.category = 'BEHAVIORAL' AND q.question_type = 'MAIN' THEN 'BEHAVIORAL_MAIN'
     WHEN qs.category = 'BEHAVIORAL' AND q.question_type = 'FOLLOWUP' THEN 'BEHAVIORAL_FOLLOWUP'
     WHEN q.question_type = 'MAIN' THEN 'TECH_MAIN'
     WHEN q.question_type = 'FOLLOWUP' THEN 'TECH_FOLLOWUP'
     ELSE q.question_type
   END
   ...
   ↓
Phase 1 후속 커밋 (PR1.5 — 별도 PR 강제, 단일 commit 묶음):
   • MAIN/FOLLOWUP enum 값 제거
   • Question.resume() 가드 단순화 (RESUME_* 만 허용 가드 명시)
   • category 추론 fallback 헬퍼 제거 (`referenceTypeOrFallback` / `feedbackPerspectiveOrFallback`)
   • Handler / DTO 호출처 = 헬퍼 → 직접 enum 메서드 환원
   • Policy 비교 잔여 정리 (helper isFollowUp 정착)
   ※ 단일 commit 강제 사유: 부분 적용 시 Compile 오류 가능성 (enum 제거 + 헬퍼 제거 + 호출처 환원 = atomic 변경).

[Phase 2 — BE 단일 PR]
ALTER TABLE question DROP COLUMN reference_type, DROP COLUMN feedback_perspective
ALTER TABLE question_pool DROP COLUMN reference_type
   ↓
Question entity / QuestionPool entity / 모든 Builder / 매핑 제거
   ↓
question-generation.txt schema 에서 reference_type 제거
GeneratedQuestion.referenceType 필드 제거
MockAiClient mock 응답에서 reference_type 제거
   ↓
seed 18개 SQL — INSERT 컬럼 목록 / VALUES 에서 reference_type 제거

[Phase 3 — FE / Lambda 병렬 PR]
FE: types/interview.ts ReferenceType type + QuestionDetail.referenceType 필드 제거
    use-answer-flow.ts:378 referenceType 하드코딩 제거
    QuestionDetailResponse 응답 필드 제거 후속 (BE 변경과 정합)
BE: QuestionDetailResponse.referenceType 응답 필드 제거 (Phase 3 BE 부분)
Lambda: handler.py — feedbackPerspective payload 출처 변경 후 동등 검증 (코드 변경 없음 추정 — payload 형태 동일)
```

### 컴포넌트 영향 매핑

| 컴포넌트 | Phase 1 | Phase 2 | Phase 3 |
|---------|---------|---------|---------|
| `QuestionType` enum | 4종 추가 + helper 메서드 (`isFollowUp`/`isMain`/`isResume`). 후속 커밋에서 MAIN/FOLLOWUP 제거 | — | — |
| `Question` entity (`Question.java:38-44`) | column 적재 중단 (null 전달) | column 필드 제거 | — |
| `Question.resume()` 가드 (`Question.java:75`) | RESUME_* enum 비교 보존. MAIN/FOLLOWUP 제거 후속 커밋에서 가드 단순화 | — | — |
| `QuestionPool` entity | — | `referenceType` 필드 제거 | — |
| `QuestionSetAssembler` (line 42-51) | category 기반 questionType 결정 + column null | LLM `reference_type` 필드 제거 정합 | — |
| `FollowUpTransactionHandler.resolveMainReferenceType` (line 81-91) | questionType.referenceType() 환원 + null fallback (MAIN/FOLLOWUP 보존 동안) | — | — |
| `FollowUpTransactionHandler.saveFollowUpResult` (line 99, 110) | FOLLOWUP enum 직접 비교 → `isFollowUp()` 환원 + sub-type 결정 | — | — |
| `StandardFollowUpPolicy.assertCanContinue` (line 35) | FOLLOWUP enum 비교 → `isFollowUp()` 환원 | — | — |
| `ResumeTrackPolicy.assertCanContinue` (line 28-30) | dead code 정리 (no-op or RESUME_INTERROGATION cap 교체 — implement 정밀화) | — | — |
| `QuestionDetailResponse.from` | enum 환원 + null fallback | — | `referenceType` 필드 제거 |
| `AnswerResponse` | enum 환원 (값 동등) | — | — |
| `RubricLoader` / `RubricFamily` | — | — | — (값 동등) |
| LLM schema (`question-generation.txt`) | — | `reference_type` 제거 | — |
| `GeneratedQuestion` DTO | — | `referenceType` 필드 제거 | — |
| `MockAiClient` | — | mock 응답 schema 갱신 | — |
| Flyway migration (V46) | — | DROP COLUMN 3종 + Seed 정리 (단일 PR / 단일 commit 강제) | — |
| 운영 SQL (Flyway 외) | 백필 1회성 (코드 머지 후 dev → prod) | — | — |
| Seed 18개 SQL | — | INSERT 컬럼 정리 (V46 와 동일 PR/commit 묶음) | — |
| `R__seed_local.sql` | — | 컬럼 정리 (V46 동일 PR) | — |
| FE `types/interview.ts` (line 57, 71) | — | — | type + 필드 제거 |
| FE `use-answer-flow.ts:378` | — | — | 하드코딩 제거 |
| Lambda `handler.py` | — | — | payload 동등 검증 (코드 변경 0 추정) |

### Phase 의존 / 머지 순서

```
Phase 1 (BE) PR 머지
   ↓ 운영 SQL 백필 수동 실행 (dev → prod) — Phase 1 코드 배포 후
   ↓ 검증 SQL: MAIN/FOLLOWUP 잔여 0 확인
   ↓ 회귀 관찰 1-2일
Phase 1 후속 커밋 (또는 별도 PR): MAIN/FOLLOWUP enum 제거 + null fallback 제거 + Question.resume() 가드 단순화
   ↓
Phase 2 (BE) PR 생성 — V46 DDL + Seed 18종 + R__ + LLM schema 정리 단일 PR/commit
   ↓ 머지
Phase 3 (FE 선행 → BE 후행 → Lambda) — Phase 1 enum 제거 후속 커밋 머지 직후 시작 가능
   ※ Phase 3 머지 순서:
     1) FE PR 머지 (referenceType type / 필드 / 하드코딩 제거)
     2) BE PR 머지 (QuestionDetailResponse.referenceType 응답 필드 제거)
     3) Lambda 검증 (코드 변경 0 추정 → 회귀 검증만)
```

## Data Model

### Phase 1 — Flyway 변경 없음

운영 SQL (Flyway 외, 사용자 수동 실행):

```sql
-- backfill-V46-pre.sql (수동 실행)
-- 환경별 (local / dev / prod) 사전 백업 후 실행

-- 1) STANDARD 트랙 row 매핑
UPDATE question q
JOIN question_set qs ON q.question_set_id = qs.id
SET q.question_type = CASE
    WHEN qs.category = 'BEHAVIORAL'   AND q.question_type = 'MAIN'     THEN 'BEHAVIORAL_MAIN'
    WHEN qs.category = 'BEHAVIORAL'   AND q.question_type = 'FOLLOWUP' THEN 'BEHAVIORAL_FOLLOWUP'
    WHEN qs.category <> 'RESUME_BASED' AND q.question_type = 'MAIN'     THEN 'TECH_MAIN'
    WHEN qs.category <> 'RESUME_BASED' AND q.question_type = 'FOLLOWUP' THEN 'TECH_FOLLOWUP'
    ELSE q.question_type
END
WHERE q.question_type IN ('MAIN', 'FOLLOWUP');

-- 2) 검증: 잔여 MAIN/FOLLOWUP 0 확인
SELECT COUNT(*) FROM question WHERE question_type IN ('MAIN', 'FOLLOWUP');
-- 기대: 0

-- 3) 검증: RESUME_BASED 카테고리 + MAIN/FOLLOWUP 잔여 0 확인 (V44 정리 가정)
SELECT q.id, q.question_type, qs.category
  FROM question q
  JOIN question_set qs ON q.question_set_id = qs.id
 WHERE qs.category = 'RESUME_BASED' AND q.question_type IN ('MAIN','FOLLOWUP');
-- 기대: 0 row. 1+ row 시 별도 분석 후 진행 보류.
```

### Phase 2 — Flyway V46

```sql
-- V46__drop_question_classification_meta.sql
ALTER TABLE question
    DROP COLUMN reference_type,
    DROP COLUMN feedback_perspective;

ALTER TABLE question_pool
    DROP COLUMN reference_type;
```

롤백 (V46__rollback.sql 보조):

```sql
-- V46__rollback.sql (수동 실행 — 컬럼 복원만 가능, 데이터 복원 X)
ALTER TABLE question
    ADD COLUMN reference_type VARCHAR(20) NULL,
    ADD COLUMN feedback_perspective VARCHAR(20) NULL;

ALTER TABLE question_pool
    ADD COLUMN reference_type VARCHAR(50) NULL;
```

### Entity 변경 (Phase 1)

```java
// QuestionType.java (Phase 1 종료 후)
public enum QuestionType {
    TECH_MAIN(ReferenceType.MODEL_ANSWER, FeedbackPerspective.TECHNICAL),
    TECH_FOLLOWUP(ReferenceType.MODEL_ANSWER, FeedbackPerspective.TECHNICAL),
    BEHAVIORAL_MAIN(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL),
    BEHAVIORAL_FOLLOWUP(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL),
    RESUME_OPENER(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_PLAYGROUND(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_INTERROGATION(ReferenceType.GUIDE, FeedbackPerspective.TECHNICAL),
    RESUME_WRAP_UP(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL);
    // 기존 MAIN/FOLLOWUP 제거
    ...
}
```

### Entity 변경 (Phase 2)

```java
// Question.java — reference_type / feedback_perspective 필드 / 컬럼 / Builder 인자 제거
// QuestionPool.java — referenceType 필드 / 컬럼 / Builder 인자 제거
```

## API Contract

### Phase 1 — 응답 shape 동등

`QuestionDetailResponse.referenceType` 유지 (값 출처만 enum 환원). `AnswerResponse.feedbackPerspective` 유지.

### Phase 3 — `QuestionDetailResponse.referenceType` 제거

```diff
 GET /api/v1/interviews/{id}/question-sets
 Response 200:
 {
   "questions": [
     {
       "id": 1,
       "questionType": "TECH_MAIN",
       "questionText": "...",
       "modelAnswer": "...",
-      "referenceType": "MODEL_ANSWER",
       "orderIndex": 0
     }
   ]
 }
```

FE 동시 변경: `QuestionDetail.referenceType` 필드 제거 + `use-answer-flow.ts:378` 하드코딩 제거.

**머지 순서 = FE 선행 → BE 후행** (사용자 결정):
- FE = dead code 제거 (`referenceType` 필드 어디서도 안 읽음 — `use-answer-flow.ts:378` 단일 하드코딩만 존재).
- BE 응답에 `referenceType` 잔존하는 동안 FE TS 인터페이스에서 필드 제거되어 있어도 무해 (FE 가 응답에서 해당 필드 무시).
- 역순 (BE 선행) 시 = FE 머지 전까지 TS strict 빌드 자국 가능성 + 부주의 시 FE 가 BE 응답에서 사라진 필드 참조해 런타임 결함 위험.

`AnswerResponse` shape 변경 없음 (값 동등).

Error 코드 변경 없음.

## Verification (완료 판정)

> AC 매핑 표기: 각 verification 항목 끝에 (AC-N / G-N) 표기. product-spec AC 1~8 / G 1~6 와 1:1 매핑.

### Phase 1 (BE)

- [ ] **Domain Unit**: `QuestionTypeTest` — 신규 4종 (TECH_*, BEHAVIORAL_*) referenceType / feedbackPerspective 매핑 + helper (`isFollowUp/isMain/isResume`) 검증. (G-1)
- [ ] **Domain Unit**: `QuestionTest` — 신규 enum 으로 builder / `Question.resume()` 가드 검증. (G-1)
- [ ] **Domain Unit**: `QuestionSetAssemblerTest` — `fromGenerated` / `fromPool` 호출 시 category 기반 신규 sub-type 결정 + column null 전달 검증 (의존 = QuestionType / QuestionSetCategory enum 만, repository 의존 X → Domain Unit). (G-1)
- [ ] **Service Integration (Testcontainers MySQL)**: `FollowUpTransactionHandlerTest` — TECH_MAIN → TECH_FOLLOWUP / BEHAVIORAL_MAIN → BEHAVIORAL_FOLLOWUP / RESUME_* 기존 path 회귀 + `resolveMainReferenceType` null fallback (MAIN/FOLLOWUP 잔존 row) 안전성. (AC-1)
- [ ] **Service Integration**: `StandardFollowUpPolicyTest` — TECH_FOLLOWUP / BEHAVIORAL_FOLLOWUP 카운트 cap 정상 트리거. (AC-1)
- [ ] **Service Integration**: `ResumeTrackPolicyTest` — RESUME 트랙 시나리오 (chain L1~L4 진행 + chain switch + exhausted) 변경 전 동등. (AC-8)
- [ ] **Service Integration**: `RubricLoaderTest` — feedbackPerspective enum 환원 후 매핑 결과 동등 (회귀). (AC-1)
- [ ] **DTO Unit**: `QuestionDetailResponseTest` / `AnswerResponseTest` — enum 환원 값 동등 + null fallback 동작 검증. (AC-1)
- [ ] **빌드**: `./gradlew build`
- [ ] **관찰 가능 동작 (수동, dev)**: 운영 SQL 백필 dev 실행 → `SELECT COUNT(*) FROM question WHERE question_type IN ('MAIN','FOLLOWUP')` = 0 확인 + RESUME_BASED 잔여 0 확인. (AC-5)
- [ ] **회귀 체크 (자동 — service integration)**: CS / Behavioral / SystemDesign 트랙 인터뷰 생성 → 답변 → follow-up → 점수 산출 → 피드백 표시. (AC-1)
- [ ] **회귀 체크 (수동 — dev E2E)**: Resume 트랙 4단계 (PLAYGROUND → INTERROGATION L1~L4 → WRAP_UP) 1회 진행. (AC-8)

### Phase 2 (BE)

- [ ] **Domain Unit**: `QuestionTest` — `referenceType` / `feedbackPerspective` 필드 제거 후 빌더 정합. (G-2)
- [ ] **Repository (Testcontainers)**: V46 마이그레이션 적용 후 `information_schema.columns` 조회로 `reference_type` / `feedback_perspective` (question 테이블) + `reference_type` (question_pool 테이블) 부재 확인. (AC-2 / AC-3)
- [ ] **Infra Integration**: `OpenAiQuestionGeneratorTest` (Mock) — 신규 schema 응답 파싱 정상 (reference_type 출력 안 함, Jackson UNKNOWN_PROPERTIES ignore 호환 검증). (AC-4)
- [ ] **Domain Unit**: `MockAiClientTest` — mock 응답 schema 갱신 후 GeneratedQuestion 매핑 검증. (G-1)
- [ ] **빌드**: `./gradlew build` (Flyway V46 + R__ + Seed 18종 단일 PR / commit 묶음 검증). (AC-2 / AC-3)
- [ ] **회귀 체크 (자동)**: CS / Behavioral / SystemDesign 트랙 service integration 통과. (AC-1)
- [ ] **회귀 체크 (수동 — dev E2E)**: Resume 트랙 1회 진행. (AC-8)

### Phase 3 (FE 선행 → BE 후행 → Lambda)

- [ ] **FE Integration**: `use-answer-flow.test.ts` — `addQuestionToSet` 호출에 `referenceType` 누락 정상 통과. (AC-6)
- [ ] **FE Build**: `npm run build`, `npm run lint`, `npm run test`. (AC-6)
- [ ] **FE 머지 후 관찰 (수동)**: BE 응답에 `referenceType` 잔존 상태에서 FE 정상 동작 (24h 모니터링 또는 dev 1회 시나리오). (AC-6)
- [ ] **BE Phase 3 부분 — DTO Unit**: `QuestionDetailResponseTest` — `referenceType` 응답 필드 제거 정합 (필드 부재 + 기타 필드 동등). (AC-1)
- [ ] **BE 빌드**: `./gradlew build`. (AC-1)
- [ ] **Lambda**: `pytest lambda/analysis/tests/` — payload `feedbackPerspective` 동등 (코드 변경 0 가정 검증). (AC-7)
- [ ] **E2E (수동)**: dev 환경 인터뷰 4 트랙 진행 후 피드백 화면 정합. (AC-1 / AC-6 / AC-7 / AC-8)

## Pre / Post State

### Pre (현재)

- `QuestionType` = MAIN/FOLLOWUP (null,null) + RESUME_* 4종
- `Question` 테이블 = `reference_type` / `feedback_perspective` 컬럼 적재
- `question_pool` 테이블 = `reference_type` 컬럼 적재
- LLM 응답 schema = `reference_type` 출력
- `QuestionSetAssembler` = MAIN 고정 + perspective 도메인 계산 + LLM referenceType 그대로
- `FollowUpTransactionHandler.resolveMainReferenceType` = column 직접 조회
- `FollowUpTransactionHandler.saveFollowUpResult` = `QuestionType.FOLLOWUP` enum 직접 사용
- `StandardFollowUpPolicy.assertCanContinue` = `QuestionType.FOLLOWUP` enum 직접 비교
- `ResumeTrackPolicy.assertCanContinue` = `QuestionType.FOLLOWUP` enum 직접 비교 (count 항상 0 = dead code)
- DTO = entity column 직접 조회
- FE = `ReferenceType` type + `QuestionDetail.referenceType` 필드 + `use-answer-flow.ts:378` 'CS' 하드코딩
- Seed 18종 = `reference_type` 컬럼 적재

### Post (Phase 3 종료)

- `QuestionType` = TECH_*/BEHAVIORAL_* 4종 + RESUME_* 4종 (총 8종, MAIN/FOLLOWUP 부재) + helper (`isFollowUp`/`isMain`/`isResume`)
- `Question` 테이블 = 분류 메타 컬럼 부재
- `question_pool` 테이블 = `reference_type` 컬럼 부재
- LLM 응답 schema = `reference_type` 미출력
- 모든 Java path = `questionType.referenceType()` / `.feedbackPerspective()` 환원
- 모든 FOLLOWUP 비교 = `questionType.isFollowUp()` 환원
- `ResumeTrackPolicy` = no-op 또는 `RESUME_INTERROGATION` count cap (의미 명확화)
- DTO `QuestionDetailResponse` = `referenceType` 응답 필드 부재
- FE = type / 필드 / 하드코딩 모두 부재
- Seed 18종 = `reference_type` 컬럼 미사용

## 위험 / 마이그레이션 / 롤백

### 위험

- **백필 정확성** — `question_set.category` 단서로 매핑. RESUME_BASED + MAIN/FOLLOWUP 잔여 row 발견 시 매핑 불가 → Phase 1 백필 시 검증 SQL (위 데이터 모델 섹션) 잔여 0 확인 후 진행. 1+ row 시 분석 후 별도 처리.
- **백필 미실행 환경에서 NPE / 잘못된 분기 위험** — Phase 1 머지 직후 백필 미실행 환경 (local / 신규 dev) 에서 `MAIN`/`FOLLOWUP` row 가 잔존하면 `questionType.referenceType()` = null.
  - 완화: (1) **category 추론 fallback** — Handler / DTO 공용 헬퍼 (`referenceTypeOrFallback(category)` / `feedbackPerspectiveOrFallback(category)`) — BEHAVIORAL → GUIDE / BEHAVIORAL_perspective, 그 외 → MODEL_ANSWER / TECHNICAL. 단순 MODEL_ANSWER fallback 시 BEHAVIORAL row 가 CONCEPT 모드로 잘못 분기되는 결함 회피. (2) Phase 1 PR description 에 "**백필 SQL 실행 필수**" 체크리스트 동봉. (3) 백필 후속 검증 SQL 잔여 0 확인 후에 MAIN/FOLLOWUP enum 제거 커밋 (PR1.5).
- **백필 SQL 실행 시점 race** — 백필 실행 중 신규 인터뷰 트랜잭션이 동시 발생 = 신규 row 가 구 enum (MAIN) 으로 적재될 가능성. 완화: Phase 1 코드 머지 → 신규 인터뷰는 모두 신규 enum 적재 → 백필 SQL 은 선행 row 만 대상. 운영 SQL 헤더에 "**Phase 1 코드 배포 후 실행**" 강제 주석. 백필 후 재검증 SQL 1회 더 실행.
- **DDL 비가역 (Phase 2)** — `DROP COLUMN` 3종. 사전 운영 백업 + Phase 1 머지 후 회귀 관찰 1-2일 확보.
- **운영 SQL 수동 실행 누락** — 환경별 (local / dev / prod) 누락 가능성. 체크리스트 + 검증 SQL 강제. (체크리스트 위치 = `implement-be.md` task 또는 plan 폴더 내 `runbook.md` — implement 단계 결정).
- **LLM 응답 호환** — Phase 2 schema 변경 시 LLM 이 여전히 `reference_type` 출력하면 Jackson 무시 필요. `ObjectMapper` `FAIL_ON_UNKNOWN_PROPERTIES=false` 또는 `@JsonIgnoreProperties` 확인 (implement 단계 사전 검증 — `infra/ai/` 5분 grep).
- **시드 18개 + V46 동일 PR 강제 (Phase 2)** — 컬럼 부재 후 INSERT 실패 가능. seed SQL (V18 / V19 / R__seed_local.sql 등) 의 INSERT 컬럼 목록 / VALUES 에서 `reference_type` 제거 필수. **V46 DDL + 시드 18종 + R__seed_local.sql 변경 = 단일 PR / 단일 commit 묶음 강제** (R__ Flyway 체크섬 변경 시 재실행 → V46 직후 R__ 재실행 시 INSERT (...reference_type...) 가 컬럼 부재로 실패).
- **잔여 sentinel row 관찰성** — 백필 누락 / 신규 race row 발견 시 즉시 알림 부재. 완화: Phase 1 fallback 트리거 시 WARN 로그 (questionId / questionSetId / category 컨텍스트) 출력. 운영 모니터링에서 검색 가능.

### 마이그레이션 전략

- **Zero-downtime 보장**: Phase 1 머지 후에도 column 잔존 → 기존 row 읽기 안전. Phase 2 DROP 시점은 enum 환원 path 100% 적용 후 + 백필 100% + 회귀 검증 후.
- **Phase 1 → 백필 SQL → Phase 2 → Phase 3** 순서 강제. Phase 2 가 Phase 1 + 백필 미완 상태에서 머지되면 신규 인터뷰 생성 시 NPE / 잘못된 enum 매핑 위험.
- **Read-then-write 불필요** — 컬럼 적재 중단 (Phase 1) → 컬럼 무시 (Phase 1) → 컬럼 제거 (Phase 2). dual-write 구간 없음.

### 롤백 시나리오

- **Phase 1 회귀 발견**: PR revert. 신규 enum 사용 row 가 잔존하면 MAIN/FOLLOWUP 으로 역매핑 필요 (운영 SQL 별도 작성).
- **Phase 2 회귀 발견 (DDL DROP 후)**: column 복원 가능 (V46__rollback.sql), 데이터 복원 불가 — 어차피 Phase 1 부터 적재 안 했으므로 손실 없음. 단 LLM schema 복원 + GeneratedQuestion / MockAiClient 코드 복원 PR 필요.
- **Phase 3 FE 회귀 발견 (BE PR4 머지 전)**: FE PR3 revert. BE 응답 referenceType 잔존 상태로 복구 — 안전.
- **Phase 3 BE 회귀 발견 (BE PR4 머지 후)**: BE PR4 revert. FE 는 이미 referenceType 미사용 상태라 무해.
- **Lambda 회귀 발견**: 코드 변경 0 추정이라 통상 발생 X. 발견 시 payload 동등성 재검증 후 코드 fix.

## 분기 결정

본 작업은 **BE+FE+Lambda 3 영역 동시 변경**.

- [x] **BE 선행 강제 (Phase 1 + Phase 2)** + **Phase 3 = FE 선행 → BE 후행 → Lambda 검증** (사용자 결정).
- [ ] 단일 영역 → 해당 안 됨
- [ ] BE+FE 완전 병렬 → 해당 안 됨 (Phase 3 머지 순서 강제)

implement.md 분리:
- `implement-be.md` — Phase 1 (PR1: enum 확장 + path 환원 + category 추론 fallback 헬퍼 + Policy 정리) + Phase 1 후속 (PR1.5: MAIN/FOLLOWUP 제거 + fallback 헬퍼 제거 — 단일 commit 강제) + Phase 2 (PR2: V46 DDL + Seed 18종 + R__ + LLM schema 정리 단일 PR/commit) + Phase 3 BE (PR4: QuestionDetailResponse.referenceType 응답 필드 제거 — FE PR3 머지 후 머지)
- `implement-fe.md` — Phase 3 FE (PR3: type / 필드 / 하드코딩 제거 — Phase 1 후속 PR1.5 머지 후 시작 가능, BE PR4 보다 선행 머지)
- Lambda = `implement-be.md` Phase 3 부분에 검증 task (코드 변경 0 추정 → payload 동등 회귀만)

**Path 정정 정책 (P0-B 사용자 결정)**: tech-spec 의 라인 번호 / 패키지 경로는 작성 시점 추정. implement agent 가 작업 시 grep / Read 로 실제 위치 검증하면서 **tech-spec 동시 갱신** (단순 라인 번호 정정은 commit 별도 분리 — 구현 commit 과 혼재 금지).

PR 의존 그래프:
```
PR1 (BE Phase 1) → 백필 → PR1.5 (BE Phase 1 후속) → PR2 (BE Phase 2) ──┐
                                       ↓                                  │
                                       └─ PR3 (FE Phase 3) → PR4 (BE Phase 3) ─→ Lambda 검증
```
