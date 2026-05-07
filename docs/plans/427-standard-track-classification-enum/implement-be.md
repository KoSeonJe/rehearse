# Implement (Backend) — STANDARD 트랙 분류 메타 enum 단일 출처화 + 컬럼 정규화

> **작성자**: backend agent (Staff Engineer 페르소나 — Claude)
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★
> **product-spec / tech-spec**: `./product-spec.md` / `./tech-spec.md`
> **Issue**: #427 (Epic, P2)

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` 의 응답 schema 확정 여부 확인. FE 합의 상태.

- [ ] Endpoint 경로 / 메서드 변경 없음 (기존 `GET /api/v1/interviews/{id}/question-sets` 등 유지)
- [ ] Phase 1 / Phase 2 = 응답 shape 동등 (값 출처만 enum 환원)
- [ ] Phase 3 = `QuestionDetailResponse.referenceType` 필드 제거 (FE PR3 선행 머지 → BE PR4 후행 머지)
- [ ] `AnswerResponse.feedbackPerspective` 응답 shape 변경 없음
- [ ] Error 코드 매핑 변경 없음

미합의 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | enum 4종 확장 + helper + path 환원 + category 추론 fallback 헬퍼 + Policy 정리 | `backend` | PR1 | Phase 0 |
| 1.5 | 운영 SQL 백필 수동 실행 (dev → prod) + 검증 | 사용자 + `backend` | (운영) | PR1 머지 |
| 1.6 | MAIN/FOLLOWUP enum 제거 + fallback 헬퍼 제거 + `Question.resume()` 가드 단순화 (atomic 단일 commit) | `backend` | PR1.5 | Phase 1.5 잔여 0 row 검증 |
| 2 | V46 DDL DROP COLUMN 3종 + Seed 18종 + R__seed_local.sql + LLM schema 정리 (단일 PR / 단일 commit) | `backend` | PR2 | PR1.5 머지 + 회귀 1-2일 |
| 3-BE | `QuestionDetailResponse.referenceType` 응답 필드 제거 + Lambda payload 동등 검증 | `backend` | PR4 | FE PR3 머지 |

> Path 정정 정책 (tech-spec `분기 결정` 섹션): implement 작업 시 grep / Read 로 line / package 검증. 차이 발견 시 tech-spec 동시 갱신 (단순 라인 번호 정정 commit 은 구현 commit 과 분리).

---

## Phase 1: enum 4종 확장 + path 환원 + fallback 헬퍼 + Policy 정리

- **구현**: `backend` — STANDARD 트랙 enum sub-type 도입 + 도메인 path 모두 enum 환원. 기존 `MAIN`/`FOLLOWUP` 보존 (Phase 1.6 에서 일괄 제거).

### 변경 파일

**Enum (1)**:
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java`
  - `TECH_MAIN(MODEL_ANSWER, TECHNICAL)` / `TECH_FOLLOWUP(MODEL_ANSWER, TECHNICAL)` / `BEHAVIORAL_MAIN(GUIDE, BEHAVIORAL)` / `BEHAVIORAL_FOLLOWUP(GUIDE, BEHAVIORAL)` 추가
  - 기존 `MAIN(null, null)` / `FOLLOWUP(null, null)` 보존
  - helper 메서드 추가: `isFollowUp()` / `isMain()` / `isResume()` / `referenceTypeOrFallback(QuestionSetCategory)` / `feedbackPerspectiveOrFallback(QuestionSetCategory)`

**Assembler (1)**:
- `.../question/service/QuestionSetAssembler.java` (line 42-51 영역)
  - `fromGenerated` / `fromPool` 시 `QuestionSetCategory.BEHAVIORAL` → `BEHAVIORAL_MAIN` / 그 외 → `TECH_MAIN`
  - `referenceType` / `feedbackPerspective` 컬럼 적재 = `null` 전달 (Builder 인자에서 분리)

**Follow-up Handler (1)**:
- `.../interview/service/FollowUpTransactionHandler.java`
  - `resolveMainReferenceType` (line 81-91 추정) → `mainQuestion.getQuestionType().referenceTypeOrFallback(category)` 환원
  - `saveFollowUpResult` (line 99 / 110 추정) → `q.getQuestionType().isFollowUp()` 환원 + sub-type 결정 (`TECH_MAIN` → `TECH_FOLLOWUP` / `BEHAVIORAL_MAIN` → `BEHAVIORAL_FOLLOWUP` / `RESUME_*` → 기존 path)

**Policy (2, 실제 위치 = `domain/interview/service/`)**:
- `.../interview/service/StandardFollowUpPolicy.java` (line 35) — `q.getQuestionType() == QuestionType.FOLLOWUP` → `q.getQuestionType().isFollowUp()`
- `.../interview/service/ResumeTrackPolicy.java` (line 28-30) — `assertCanContinue` no-op 으로 정리 (RESUME 종료 제어 = ChainStateTracker / ResumeModeTransitionPolicy / ClockWatcher 일임). dead `FOLLOWUP` count 가드 제거. tech-spec Architecture 섹션 옵션 (a) 채택. (b)(`RESUME_INTERROGATION` count cap) 선택 시 implement 단계 결정 + tech-spec 동시 갱신.

**DTO (2, AnswerResponse 실제 위치 = `domain/question/dto/`)**:
- `.../question/dto/QuestionDetailResponse.java` (line 18, 28) — `from(question)` 에서 `question.getReferenceType()` → `question.getQuestionType().referenceTypeOrFallback(category)` 환원
- `.../question/dto/AnswerResponse.java` (line 23-33) — `feedbackPerspective` 매핑을 `question.getQuestionType().feedbackPerspectiveOrFallback(category)` 환원

**Question Entity (가드 보존)**:
- `.../question/entity/Question.java` (line 75 — `Question.resume()` 팩토리)
  - 가드 = `RESUME_*` 만 허용. `MAIN`/`FOLLOWUP` 보존 동안 단순화 X (Phase 1.6 에서 단순화).

### 핵심 로직

```java
// QuestionType.java 신규 helper
public enum QuestionType {
    TECH_MAIN(ReferenceType.MODEL_ANSWER, FeedbackPerspective.TECHNICAL),
    TECH_FOLLOWUP(ReferenceType.MODEL_ANSWER, FeedbackPerspective.TECHNICAL),
    BEHAVIORAL_MAIN(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL),
    BEHAVIORAL_FOLLOWUP(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL),
    RESUME_OPENER(...), RESUME_PLAYGROUND(...), RESUME_INTERROGATION(...), RESUME_WRAP_UP(...),
    MAIN(null, null),     // Phase 1.6 제거
    FOLLOWUP(null, null); // Phase 1.6 제거

    public boolean isFollowUp() {
        return this == TECH_FOLLOWUP || this == BEHAVIORAL_FOLLOWUP || this == FOLLOWUP;
    }
    public boolean isMain() {
        return this == TECH_MAIN || this == BEHAVIORAL_MAIN || this == MAIN;
    }
    public boolean isResume() {
        return name().startsWith("RESUME_");
    }
    // category 추론 fallback (Phase 1.6 제거)
    public ReferenceType referenceTypeOrFallback(QuestionSetCategory category) {
        if (referenceType != null) return referenceType;
        if (category == QuestionSetCategory.BEHAVIORAL) return ReferenceType.GUIDE;
        return ReferenceType.MODEL_ANSWER;
        // WARN 로그: log.warn("[427-fallback] questionType={} category={} referenceType=null", this, category);
    }
    public FeedbackPerspective feedbackPerspectiveOrFallback(QuestionSetCategory category) {
        if (feedbackPerspective != null) return feedbackPerspective;
        if (category == QuestionSetCategory.BEHAVIORAL) return FeedbackPerspective.BEHAVIORAL;
        return FeedbackPerspective.TECHNICAL;
    }
}
```

```
QuestionSetAssembler.fromGenerated:
  category = questionSet.getCategory()
  questionType = (category == BEHAVIORAL) ? BEHAVIORAL_MAIN : TECH_MAIN
  Question.builder()
    .questionType(questionType)
    .referenceType(null)         // 컬럼 적재 중단
    .feedbackPerspective(null)   // 컬럼 적재 중단
    ...

FollowUpTransactionHandler.saveFollowUpResult:
  followUpType = switch (mainQuestion.getQuestionType()) {
    case TECH_MAIN -> TECH_FOLLOWUP
    case BEHAVIORAL_MAIN -> BEHAVIORAL_FOLLOWUP
    case MAIN -> (category == BEHAVIORAL ? BEHAVIORAL_FOLLOWUP : TECH_FOLLOWUP)  // 백필 미실행 fallback
    case RESUME_* -> (기존 path 유지)
  }
  // 기존 q.getQuestionType() == FOLLOWUP 비교는 .isFollowUp() 환원

FollowUpTransactionHandler.resolveMainReferenceType:
  return mainQuestion.getQuestionType().referenceTypeOrFallback(questionSet.getCategory())
  // GUIDE → EXPERIENCE 모드 / MODEL_ANSWER → CONCEPT 모드 분기 정합 보장

StandardFollowUpPolicy.assertCanContinue:
  long followUpCount = questions.stream().filter(q -> q.getQuestionType().isFollowUp()).count();
  if (followUpCount >= maxRounds) throw ...

ResumeTrackPolicy.assertCanContinue:
  // FOLLOWUP count 제거 (dead code). 종료 제어 = ChainStateTracker / ResumeModeTransitionPolicy / ClockWatcher
  // no-op (또는 신규 RESUME_INTERROGATION cap 교체 — tech-spec 동시 갱신)
```

### 의존
- 선행: Phase 0 (contract 합의)
- 외부: 없음

### Verification
- `./gradlew test --tests QuestionTypeTest` — 신규 4종 매핑 + helper / fallback 헬퍼 검증
- `./gradlew test --tests QuestionTest` — `Question.resume()` 가드 + builder 정합
- `./gradlew test --tests QuestionSetAssemblerTest` — category 기반 sub-type 결정 + column null 전달
- `./gradlew test --tests FollowUpTransactionHandlerTest` — TECH/BEHAVIORAL/RESUME path + null fallback (MAIN/FOLLOWUP 잔존 row) 안전성 (Testcontainers MySQL)
- `./gradlew test --tests StandardFollowUpPolicyTest` — TECH_FOLLOWUP / BEHAVIORAL_FOLLOWUP cap 트리거
- `./gradlew test --tests ResumeTrackPolicyTest` — chain L1~L4 + chain switch + exhausted 변경 전 동등
- `./gradlew test --tests RubricLoaderTest` — feedbackPerspective enum 환원 후 매핑 동등
- `./gradlew test --tests QuestionDetailResponseTest` / `AnswerResponseTest` — enum 환원 + null fallback
- `./gradlew build` 통과
- 통과 기준: 모든 테스트 green + 컴파일 / Checkstyle / SpotBugs 에러 0건

### 커밋 메시지
```
feat(BE): QuestionType enum sub-type 4종 추가 + path 단일 출처 환원
```

---

## Phase 1.5: 운영 SQL 백필 수동 실행 + 검증

- **구현**: 사용자 (운영자) + `backend` 검증 보조 — Phase 1 코드 머지 후 운영 SQL 1회성 실행. dev → prod 순.

### 변경 파일
- 없음 (코드 변경 X). 운영 작업.

### 운영 SQL (Flyway 외)

`docs/plans/427-standard-track-classification-enum/runbook.md` 또는 동등 위치에 사전 작성:

```sql
-- backfill-V46-pre.sql (수동 실행, 환경별 사전 백업 후)

-- 1) STANDARD 트랙 row 매핑
UPDATE question q
JOIN question_set qs ON q.question_set_id = qs.id
SET q.question_type = CASE
    WHEN qs.category = 'BEHAVIORAL'    AND q.question_type = 'MAIN'     THEN 'BEHAVIORAL_MAIN'
    WHEN qs.category = 'BEHAVIORAL'    AND q.question_type = 'FOLLOWUP' THEN 'BEHAVIORAL_FOLLOWUP'
    WHEN qs.category <> 'RESUME_BASED' AND q.question_type = 'MAIN'     THEN 'TECH_MAIN'
    WHEN qs.category <> 'RESUME_BASED' AND q.question_type = 'FOLLOWUP' THEN 'TECH_FOLLOWUP'
    ELSE q.question_type
END
WHERE q.question_type IN ('MAIN', 'FOLLOWUP');

-- 2) 검증: 잔여 MAIN/FOLLOWUP 0
SELECT COUNT(*) AS remain FROM question WHERE question_type IN ('MAIN', 'FOLLOWUP');
-- 기대: 0

-- 3) 검증: RESUME_BASED + MAIN/FOLLOWUP 잔여 0 (V44 가정)
SELECT q.id, q.question_type, qs.category
  FROM question q JOIN question_set qs ON q.question_set_id = qs.id
 WHERE qs.category = 'RESUME_BASED' AND q.question_type IN ('MAIN','FOLLOWUP');
-- 기대: 0 row. 1+ row 시 Phase 1.6 진행 보류 후 분석.
```

### 핵심 로직
- Phase 1 머지 → 신규 인터뷰 = 모두 신규 enum 적재. 백필 = 선행 row 한정.
- dev 백필 → 검증 SQL 잔여 0 확인 → prod 백필 → 검증 SQL 잔여 0 확인 → 회귀 관찰 1-2일 → Phase 1.6 진입.

### 의존
- 선행: PR1 머지 + dev 배포 완료
- 외부: dev / prod DB 직접 접근 권한

### Verification
- [ ] dev 백필 후 검증 SQL #2 = 0 (AC-5)
- [ ] dev 백필 후 검증 SQL #3 = 0 (RESUME_BASED 잔여)
- [ ] prod 백필 후 검증 SQL #2 = 0 (AC-5)
- [ ] prod 백필 후 검증 SQL #3 = 0
- [ ] dev 회귀 관찰 1-2일 — 인터뷰 생성 / follow-up / 점수 산출 정상

### 커밋 메시지
- 코드 commit 없음. 운영 SQL 파일은 plan 폴더 / runbook 에 보존 (별도 docs commit).

---

## Phase 1.6: MAIN/FOLLOWUP enum 제거 + fallback 헬퍼 제거 + 가드 단순화 (atomic)

- **구현**: `backend` — Phase 1.5 백필 검증 잔여 0 확인 후 진입. 단일 commit / 단일 PR 강제 (atomic).

### 변경 파일

**Enum**:
- `.../question/entity/QuestionType.java` — `MAIN(null,null)` / `FOLLOWUP(null,null)` enum 값 삭제

**Helper 제거**:
- `.../question/entity/QuestionType.java` — `referenceTypeOrFallback(category)` / `feedbackPerspectiveOrFallback(category)` 메서드 삭제 (Phase 1.6 에서 enum 제거 → null 케이스 소멸)

**Question Entity**:
- `.../question/entity/Question.java` (line 75) — `Question.resume()` 팩토리 가드 = `RESUME_*` 4종 화이트리스트로 단순화 (`MAIN`/`FOLLOWUP` 비교 코드 제거)

**Handler / DTO 호출처 환원**:
- `.../interview/service/FollowUpTransactionHandler.java`
  - `resolveMainReferenceType` = `mainQuestion.getQuestionType().referenceType()` 직접 호출
  - `saveFollowUpResult` switch 의 `case MAIN` 폴백 분기 제거
- `.../question/dto/QuestionDetailResponse.java` — `referenceTypeOrFallback(category)` → `referenceType()` 직접
- `.../interview/dto/AnswerResponse.java` — `feedbackPerspectiveOrFallback(category)` → `feedbackPerspective()` 직접

**잔여 grep**:
- `grep -rn "QuestionType.MAIN\|QuestionType.FOLLOWUP" backend/src/main backend/src/test` → 0건 (테스트 fixture 동시 정리)
- `grep -rn "referenceTypeOrFallback\|feedbackPerspectiveOrFallback" backend/src` → 0건

### 핵심 로직
```
단일 commit 강제 사유: enum 제거 + 헬퍼 제거 + 호출처 환원 = atomic 변경.
부분 적용 시 컴파일 깨짐.

순서:
1) 호출처 환원 먼저 적용 (helper → 직접 메서드)
2) helper 메서드 삭제
3) MAIN/FOLLOWUP enum 값 삭제
4) Question.resume() 가드 단순화
5) 테스트 fixture / Mock 의 MAIN/FOLLOWUP 사용 정리
모두 한 commit 으로 묶음.
```

### 의존
- 선행: Phase 1.5 dev + prod 백필 검증 잔여 0
- 외부: 없음

### Verification
- `grep -rn "QuestionType.MAIN\b\|QuestionType.FOLLOWUP\b" backend/src` → 0건
- `grep -rn "Or[Ff]allback" backend/src/main/java/.../question/entity/QuestionType.java` → 0건
- `./gradlew test` 전체 통과
- `./gradlew build` 통과
- 회귀: CS / Behavioral / SystemDesign / Resume 각 트랙 service integration 통과

### 커밋 메시지
```
refactor(BE): MAIN/FOLLOWUP enum sentinel 제거 + 추론 fallback 정리
```

---

## Phase 2: V46 DDL DROP COLUMN 3종 + Seed 18종 + R__ + LLM schema 정리

- **구현**: `backend` — DDL 비가역 단계. 단일 PR / 단일 commit 묶음 강제.

### 변경 파일

**Flyway**:
- `backend/src/main/resources/db/migration/V46__drop_question_classification_meta.sql` — 신규
  ```sql
  ALTER TABLE question
      DROP COLUMN reference_type,
      DROP COLUMN feedback_perspective;
  ALTER TABLE question_pool
      DROP COLUMN reference_type;
  ```
- `backend/src/main/resources/db/migration/V46__rollback.sql` — 보조 (수동 실행, 컬럼 복원만)

**Entity**:
- `.../question/entity/Question.java` (line 38-44) — `referenceType` / `feedbackPerspective` 필드 / `@Column` / Builder 인자 제거
- `.../question/entity/QuestionPool.java` — `referenceType` 필드 / `@Column` / Builder 인자 제거

**LLM schema**:
- `backend/src/main/resources/prompts/template/question-generation.txt` (line 37-39, 71-74) — `reference_type` 필드 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedQuestion.java` (line 30) — `@JsonProperty("reference_type") referenceType` 필드 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/MockAiClient.java` (line 50-54) — mock 응답 schema 에서 `reference_type` 제거

**Seed (18 + R__)**:
- `backend/src/main/resources/db/migration/` 내 `V18__*` / `V19__*` 등 시드 SQL — INSERT 컬럼 목록 / VALUES 에서 `reference_type` 제거 (실제 파일 grep 후 정리: `grep -rln "reference_type" backend/src/main/resources/db/migration/`)
- `backend/src/main/resources/db/migration/R__seed_local.sql` — 동일 정리

**Assembler 정합**:
- `.../question/service/QuestionSetAssembler.java` — `referenceType(null)` / `feedbackPerspective(null)` Builder 인자 제거 (필드 자체 제거됨)

**Jackson 호환 사전 검증** (5분 grep, 코드 변경 없을 수도):
- `grep -rn "FAIL_ON_UNKNOWN_PROPERTIES\|@JsonIgnoreProperties" backend/src/main/java/com/rehearse/api/infra/ai/` — `ObjectMapper` 가 unknown properties 무시하는지 확인. 미설정 시 `@JsonIgnoreProperties(ignoreUnknown=true)` 추가.

### 핵심 로직
```
단일 PR / 단일 commit 강제 사유:
  V46 DROP COLUMN 적용 → R__seed_local.sql 재실행 (Flyway 체크섬 변경 시 자동) →
  R__ INSERT (...reference_type...) 가 컬럼 부재로 실패.
  V46 + 시드 18종 + R__ + Entity / LLM schema 정리 = 단일 commit 묶음만 안전.

순서 (한 commit 안):
1) Entity 필드 제거 (Question / QuestionPool)
2) Assembler / 모든 Builder 호출 정리
3) LLM schema (txt) + GeneratedQuestion + MockAiClient 정리
4) Seed 18종 INSERT 컬럼 정리
5) R__seed_local.sql 정리
6) V46__drop_question_classification_meta.sql 추가
7) ./gradlew build 로컬 통과 확인 후 commit
```

### 의존
- 선행: PR1.5 머지 + 회귀 관찰 1-2일 + 운영 백업
- 외부: dev / prod 운영 백업 (사용자 확인)

### Verification
- `./gradlew test --tests QuestionTest` — `referenceType` / `feedbackPerspective` 필드 제거 후 빌더 정합
- Repository (Testcontainers): V46 적용 후 `information_schema.columns` 조회 → `question.reference_type` / `question.feedback_perspective` / `question_pool.reference_type` 부재 확인 (AC-2 / AC-3)
- `./gradlew test --tests OpenAiQuestionGeneratorTest` (Mock) — 신규 schema 응답 파싱 정상 (UNKNOWN_PROPERTIES ignore 호환) (AC-4)
- `./gradlew test --tests MockAiClientTest` — mock 응답 schema 갱신 후 `GeneratedQuestion` 매핑 (G-1)
- `./gradlew build` 통과 (Flyway V46 + R__ + Seed 18종 단일 PR/commit 검증) (AC-2 / AC-3)
- 회귀 (자동): CS / Behavioral / SystemDesign service integration 통과 (AC-1)
- 회귀 (수동 — dev E2E): Resume 트랙 1회 진행 (AC-8)

### 커밋 메시지
```
chore(BE): question 분류 메타 컬럼 제거 + LLM 응답 schema 정리
```

---

## Phase 3-BE: QuestionDetailResponse.referenceType 응답 필드 제거 + Lambda 검증

- **구현**: `backend` — FE PR3 (`implement-fe.md` Phase 1) 머지 후 진입. BE 응답에서 `referenceType` 필드 제거.

### 변경 파일

**DTO (1)**:
- `.../question/dto/QuestionDetailResponse.java` (line 18, 28) — `referenceType` 필드 + `from(question)` 매핑 제거. JSON 응답에서 키 부재.

**Lambda 검증 (코드 변경 0 추정)**:
- `lambda/analysis/handler.py` (line 205-218, 233-246) — `feedbackPerspective` payload 동등 검증. 출처 변경 후에도 같은 string 값 (`TECHNICAL` / `BEHAVIORAL` / `EXPERIENCE`) 전달되는지 service integration 결과로 확인.

### 핵심 로직
```
QuestionDetailResponse.from(question):
  - 기존: referenceType = question.getReferenceType() / 또는 enum 환원
  - 신규: referenceType 필드 자체 제거. JSON 응답 key 부재.

Lambda payload 동등성:
  - BE 가 보내는 feedbackPerspective string = QuestionType enum 의 feedbackPerspective().name()
  - Phase 1 환원 후 값 동등 (TECHNICAL / BEHAVIORAL / EXPERIENCE)
  - Lambda handler.py 가 default 'TECHNICAL' fallback 보유 → 안전
  - 회귀 = analysis 결과 (점수 / 코멘트) 변경 0건 확인
```

### 의존
- 선행: FE PR3 머지 (`implement-fe.md` Phase 1)
- 외부: Lambda dev 배포 (코드 변경 0 추정 시 미배포)

### Verification
- `./gradlew test --tests QuestionDetailResponseTest` — `referenceType` 필드 부재 + 기타 필드 동등 (AC-1)
- `./gradlew build` 통과 (AC-1)
- `pytest lambda/analysis/tests/` — payload `feedbackPerspective` 동등 (AC-7)
- E2E (수동): dev 환경 인터뷰 4 트랙 진행 후 피드백 화면 정합 (AC-1 / AC-6 / AC-7 / AC-8)

### 커밋 메시지
```
refactor(BE): QuestionDetailResponse referenceType 응답 필드 제거
```

---

## FE 와 통합 시점

- **Phase 1 / Phase 2 = 응답 shape 동등** → FE 영향 없음. FE PR 진입 = Phase 1.6 (PR1.5) 머지 후.
- **Phase 3 머지 순서 강제**: FE PR3 선행 머지 → BE PR4 후행 머지 (사용자 결정, tech-spec `API Contract` 섹션).
  - 사유: FE = dead code 제거 (referenceType 어디서도 안 읽음). BE 응답 잔존 무해. 역순 = TS strict 빌드 자국 위험.
- BE PR4 머지 직후 Issue #427 댓글로 Lambda 검증 진입 알림.

## 통합 Verification

- [ ] `tech-spec.md#verification` Phase 1 / Phase 2 / Phase 3 항목 모두 통과
- [ ] FE 통합 후 회귀 체크 (FE PR3 머지 후 dev 환경 4 트랙 정상 진행 1건 이상)
- [ ] Lambda 회귀 체크 (코드 변경 0 추정 검증 — analysis 결과 변경 0건)
- [ ] AC 1~8 / G 1~6 모든 항목 통과 (`product-spec.md`)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임). PR1 / PR1.5 / PR2 / PR4 각각 실행
- [ ] BE+FE 동시 작업 (Phase 3) = `code-reviewer-frontend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (`tech-spec.md#pre--post-state` 기준)
