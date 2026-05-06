# Implement — interview 도메인 발견 이슈 통합 (보안/안정성/cleanup 7건)

> 작성자: Staff Engineer (자율 분해)
> 답하는 질문: 어떤 순서로 실행?
> 단일 영역 (BE only)
> 승인 게이트: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 의존 | 커밋 |
|-------|------|--------------|------|------|
| 1 | 기반: 마이그레이션 + enum + ErrorCode + dead code | `backend` | - | 1 |
| 2 | #1 권한 + #3 audio validation | `backend` | Phase 1 | 1 |
| 3 | #2 enum 적용 + #4 retry counter/cooldown + #5 retry 분리 | `backend` | Phase 1 | 1 |
| 4 | #7 replan API | `backend` | Phase 1 | 1 |
| 5 | 통합 테스트 + 외부화 설정 | `backend` | Phase 2/3/4 | 1 |

분리 임계 미초과 → 단일 `implement.md`. PR 1개. 총 5 커밋. BE only — 모든 Phase `backend` agent (Read 강제: `backend/.claude/rules/conventions.md` + `testing.md`).

---

## Phase 1: 기반 — 마이그레이션 + enum + ErrorCode + dead code

- **구현**: `backend` — Flyway DDL + Entity 매핑 + enum 정의 (BE 컨벤션 / Lombok / `@Transactional` 룰)

### 변경 파일
- `backend/src/main/resources/db/migration/V10__add_question_gen_retry_to_interviews.sql` (신규) — `interviews` 컬럼 2개 추가
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/CsSubTopic.java` (신규) — enum 정의 (#2 기반)
- `backend/src/main/java/com/rehearse/api/domain/interview/exception/InterviewErrorCode.java` (수정) — 신규 에러 코드 + #8 dead code 제거
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/Interview.java` (수정) — `questionGenRetryCount`, `questionGenLastRetriedAt` 컬럼 매핑 + helper

### 핵심 로직
```sql
-- V10__add_question_gen_retry_to_interviews.sql
ALTER TABLE interviews
  ADD COLUMN question_gen_retry_count INT NOT NULL DEFAULT 0,
  ADD COLUMN question_gen_last_retried_at DATETIME(6) NULL;
```

```java
// CsSubTopic.java — 운영 DB interview_cs_sub_topics 사용 값 조사 후 확정
public enum CsSubTopic {
    DATA_STRUCTURE, ALGORITHM, OPERATING_SYSTEM, NETWORK, DATABASE, WEB
    // 운영 조사 결과 따라 추가/조정
}
```

```java
// InterviewErrorCode.java
// 추가
RETRY_LIMIT_EXCEEDED(429, "재시도 한도 초과"),
RETRY_COOLDOWN(429, "재시도 대기 중"),
INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED(409, "이력서 분석 실패. 처음부터 다시 시작 필요"),
CS_SUB_TOPIC_INVALID(400, "허용되지 않는 CS 주제"),
AUDIO_MIME_NOT_ALLOWED(400, "허용되지 않는 오디오 형식"),
AUDIO_DURATION_EXCEEDED(400, "오디오 길이 초과"),
AUDIO_MAGIC_BYTE_MISMATCH(400, "오디오 헤더 불일치"),
INTERVIEW_NOT_RESUME_BASED(400, "RESUME_BASED 인터뷰 아님"),
// 제거
CANNOT_DELETE_COMPLETED // ← 삭제 (#8)
```

```java
// Interview.java
@Column(name = "question_gen_retry_count", nullable = false)
private int questionGenRetryCount = 0;

@Column(name = "question_gen_last_retried_at")
private LocalDateTime questionGenLastRetriedAt;

void recordRetryAttempt() {
    this.questionGenRetryCount++;
    this.questionGenLastRetriedAt = LocalDateTime.now();
}
```

### 운영 데이터 조사 (선행)
- 운영 DB `SELECT DISTINCT cs_sub_topic FROM interview_cs_sub_topics` 결과 확인 → enum 정의 근거.
- 운영 DML 조회만 (변경 X). 결과 = enum 정의 가이드.

### 의존
- 선행: 없음
- 외부: Flyway 마이그레이션 적용 후 `Interview` 컬럼 매핑 작동

### Verification
- `./gradlew compileJava` 통과
- `./gradlew flywayMigrate` (local profile) 적용 → `DESCRIBE interviews` 신규 컬럼 2개 확인
- 단위: `InterviewTest.recordRetryAttempt_counter증가_시각업데이트()`
- `CANNOT_DELETE_COMPLETED` 호출처 0건 (grep 재확인)

### 커밋 메시지
```
feat(BE): interview 도메인 retry 컬럼 + CsSubTopic enum + ErrorCode 정비
```

---

## Phase 2: #1 권한 + #3 audio validation

- **구현**: `backend` — 도메인 검증 / `@Component` validator / controller 통합 (보안 룰 `.claude/rules/security.md` A01·A03)

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/Interview.java` (수정) — `validateOwner` userId NULL 거부
- `backend/src/main/java/com/rehearse/api/domain/interview/validation/AudioValidator.java` (신규) — mime / 길이 / 매직바이트 검증 컴포넌트
- `backend/src/main/java/com/rehearse/api/domain/interview/controller/InterviewController.java` (수정) — followUp endpoint `AudioValidator` 호출

### 핵심 로직
```java
// Interview.validateOwner (#1)
public void validateOwner(Long userId) {
    if (this.userId == null || !this.userId.equals(userId)) {
        throw new BusinessException(InterviewErrorCode.INTERVIEW_NOT_FOUND);
    }
}
```

```java
// AudioValidator.java (#3) — controller 단 컴포넌트
@Component
public class AudioValidator {
    private final Set<String> mimeWhitelist;       // 외부화
    private final long maxBytes;                    // 외부화
    private final int maxDurationSeconds;           // 외부화

    public void validate(MultipartFile file) {
        if (!mimeWhitelist.contains(file.getContentType())) {
            throw new BusinessException(AUDIO_MIME_NOT_ALLOWED);
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(AUDIO_DURATION_EXCEEDED);
        }
        // stream 첫 N byte read → 매직바이트 (webm/mp4/mp3/wav) 매칭
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            if (!matchesMagicBytes(header, file.getContentType())) {
                throw new BusinessException(AUDIO_MAGIC_BYTE_MISMATCH);
            }
        }
    }
}
```

```java
// InterviewController.java followUp (line 94 부근)
@PostMapping(...)
public ResponseEntity<...> followUp(
    @PathVariable Long id,
    @AuthenticationPrincipal Long userId,
    @RequestPart FollowUpRequest request,
    @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
    if (audioFile != null) {
        audioValidator.validate(audioFile);  // 신규
    }
    // ... 기존 로직
}
```

### 의존
- 선행: Phase 1 (`InterviewErrorCode` 신규 코드)
- 외부: Phase 5 외부화 적용 전까지 default 값 (mime/bytes/duration) 코드 상수

### Verification
- 단위: `AudioValidatorTest` (mime 거부, 길이 초과, 매직바이트 불일치 각각)
- 단위: `InterviewTest.validateOwner_userIdNull_INTERVIEW_NOT_FOUND()`
- `./gradlew test --tests "*AudioValidatorTest"` green
- `log.warn` 보안 우회 시도 로그 확인

### 커밋 메시지
```
feat(BE): interview userId NULL 권한 우회 차단 + audio 입력 검증 도입
```

---

## Phase 3: #2 enum 적용 + #4 retry counter/cooldown + #5 retry 분리

- **구현**: `backend` — enum 변환 / Service retry 정책 / 이벤트 페이로드 / `@Transactional` 짧은 tx + AI 호출 tx 외부

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/Interview.java` (수정) — `Set<String> csSubTopics` → `Set<CsSubTopic>`
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/CreateInterviewRequest.java` (수정) — `List<CsSubTopic>` 검증
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/InterviewResponse.java` / `InterviewListResponse.java` (수정) — enum 변환 응답
- `backend/src/main/java/com/rehearse/api/domain/interview/repository/InterviewRepository.java` (수정 X 또는 attributePaths 그대로)
- `backend/src/main/java/com/rehearse/api/domain/interview/service/QuestionGenerationPromptBuilder.java` (수정) — enum 사용
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java` (수정) — retry counter/cooldown + skeleton 부재 거부
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewCreationService.java` (수정) — enum 변환 시 csSubTopics 전달
- `backend/src/main/java/com/rehearse/api/domain/interview/event/QuestionGenerationRequestedEvent.java` (수정) — `List<CsSubTopic>`

### 핵심 로직

#### #2 enum 적용
```java
// Interview.java
@Enumerated(EnumType.STRING)
@Column(name = "cs_sub_topic", length = 50)
private Set<CsSubTopic> csSubTopics = new HashSet<>();

// QuestionGenerationPromptBuilder.java
String topics = csSubTopics.stream()
    .map(CsSubTopic::name)
    .collect(Collectors.joining(", "));
```

DTO 검증 = Spring `@Valid` + `List<CsSubTopic>` 자동 거부 (`HttpMessageNotReadableException` → `CS_SUB_TOPIC_INVALID` 매핑).

#### #4 retry counter / cooldown
```java
// InterviewService.retryQuestionGeneration
public void retryQuestionGeneration(Long interviewId, Long userId) {
    Interview interview = interviewFinder.findOwned(interviewId, userId);

    // skeleton 부재 거부 (#5)
    if (interview.getInterviewType() == RESUME_BASED) {
        if (resumeSkeletonRepository.findByInterviewId(interviewId).isEmpty()) {
            throw new BusinessException(INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED);
        }
    }

    // counter / cooldown 검사
    if (interview.getQuestionGenRetryCount() >= maxAttempts) {
        throw new BusinessException(RETRY_LIMIT_EXCEEDED);
    }
    LocalDateTime last = interview.getQuestionGenLastRetriedAt();
    if (last != null && last.plusSeconds(cooldownSeconds).isAfter(LocalDateTime.now())) {
        throw new BusinessException(RETRY_COOLDOWN);
    }

    // counter++ tx 내 (짧은 tx, AI 호출은 tx 밖)
    incrementRetryAttempt(interview);  // @Transactional self-invoke 회피 위해 별도 컴포넌트 또는 Repository update

    // AI 호출 (tx 외부)
    questionGenerationOrchestrator.generate(interview);
}

@Transactional
void incrementRetryAttempt(Interview interview) {
    interview.recordRetryAttempt();
    interviewRepository.save(interview);
}
```

#### #5 retry 분리
- `INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED` 응답 (위 코드)
- 사용자 메시지 = "이력서 분석 실패. 처음부터 다시 시작 필요"

### 의존
- 선행: Phase 1 (`CsSubTopic`, `RETRY_LIMIT_EXCEEDED`, `RETRY_COOLDOWN`, `INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED`, retry 컬럼)
- 외부: maxAttempts / cooldownSeconds = Phase 5 외부화 전까지 코드 상수 (5 / 30)

### Verification
- 단위: `InterviewServiceTest.retry_한도초과_RETRY_LIMIT_EXCEEDED()`, `retry_쿨다운미경과_RETRY_COOLDOWN()`, `retry_skeleton부재_RECOVERY_REQUIRED()`
- 단위: `QuestionGenerationPromptBuilderTest.csSubTopics_enum_join()`
- `./gradlew test --tests "*InterviewServiceTest"` green

### 커밋 메시지
```
feat(BE): csSubTopics enum 적용 + retry 한도/쿨다운/skeleton 분리
```

---

## Phase 4: #7 replan API

- **구현**: `backend` — controller + service `replan` 메소드 + 기존 plan 갱신 로직 재사용

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/interview/controller/InterviewController.java` (수정) — `POST /api/v1/interviews/{id}/replan` 엔드포인트
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/ReplanResponse.java` (신규)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumePlanPreparationService.java` (수정) — `replan(interviewId)` 메소드 추가

### 핵심 로직
```java
// InterviewController.java
@PostMapping("/{id}/replan")
public ResponseEntity<ReplanResponse> replan(
    @PathVariable Long id,
    @AuthenticationPrincipal Long userId) {
    ReplanResponse result = resumePlanPreparationService.replan(id, userId);
    return ResponseEntity.ok(result);
}
```

```java
// ResumePlanPreparationService.replan
@Transactional
public ReplanResponse replan(Long interviewId, Long userId) {
    Interview interview = interviewFinder.findOwned(interviewId, userId);
    if (interview.getInterviewType() != RESUME_BASED) {
        throw new BusinessException(INTERVIEW_NOT_RESUME_BASED);
    }
    ResumeSkeletonEntity skeleton = resumeSkeletonRepository.findByInterviewId(interviewId)
        .orElseThrow(() -> new BusinessException(RESUME_PLAN_NOT_READY));

    // 기존 plan update (없으면 신규)
    InterviewPlan newPlan = planGenerator.generateFromSkeleton(skeleton);
    InterviewPlan saved = interviewPlanRepository.findByInterviewId(interviewId)
        .map(existing -> existing.replaceFrom(newPlan))
        .orElseGet(() -> interviewPlanRepository.save(newPlan));

    log.info("interview replan — interviewId={}, planId={}", interviewId, saved.getId());
    return ReplanResponse.of(interviewId, saved.getId(), true);
}
```

### 의존
- 선행: Phase 1 (`INTERVIEW_NOT_RESUME_BASED` ErrorCode)
- 외부: 기존 plan 생성 로직 재사용

### Verification
- 단위: `ResumePlanPreparationServiceTest.replan_정상_plan갱신()`, `replan_RESUME_BASED아님_400()`, `replan_skeleton부재_409()`
- `./gradlew test --tests "*ResumePlanPreparationServiceTest"` green

### 커밋 메시지
```
feat(BE): interview replan API 추가 — 운영 SQL 없이 plan 정정
```

---

## Phase 5: 통합 테스트 + 외부화 설정

- **구현**: `backend` — Testcontainers + MockMvc 통합 테스트 / `@ConfigurationProperties` 외부화 / yml 환경별 default

### 변경 파일
- `backend/src/test/java/com/rehearse/api/domain/interview/integration/InterviewSecurityIntegrationTest.java` (신규)
- `backend/src/test/java/com/rehearse/api/domain/interview/integration/InterviewRetryIntegrationTest.java` (신규)
- `backend/src/test/java/com/rehearse/api/domain/interview/integration/InterviewReplanIntegrationTest.java` (신규)
- `backend/src/main/resources/application.yml` (수정) — 외부화 default
- `backend/src/main/resources/application-local.yml`, `application-dev.yml`, `application-prod.yml` (수정 — 환경별 override 필요 시)
- `backend/src/main/java/com/rehearse/api/domain/interview/config/InterviewProperties.java` (신규) — `@ConfigurationProperties`

### 핵심 로직

#### 외부화
```yaml
# application.yml
rehearse:
  interview:
    retry:
      max-attempts: 5
      cooldown-seconds: 30
    audio:
      max-bytes: 10485760
      max-duration-seconds: 300
      mime-whitelist:
        - audio/webm
        - audio/mp4
        - audio/mpeg
        - audio/wav
```

```java
// InterviewProperties.java
@ConfigurationProperties("rehearse.interview")
public record InterviewProperties(Retry retry, Audio audio) {
    public record Retry(int maxAttempts, int cooldownSeconds) {}
    public record Audio(long maxBytes, int maxDurationSeconds, Set<String> mimeWhitelist) {}
}
```

`InterviewService` / `AudioValidator` 가 `InterviewProperties` 주입받아 코드 상수 제거.

#### 통합 테스트 (Testcontainers + MockMvc)
시나리오:
- #1 NULL row 다른 user 조회 → 404 `INTERVIEW_NOT_FOUND`
- #2 enum 외 csSubTopics → 400 `CS_SUB_TOPIC_INVALID`
- #3 audio mime 위조 / 5분 초과 / 매직바이트 불일치 → 400
- #4 한도 초과 → 429 `RETRY_LIMIT_EXCEEDED`. cooldown 미경과 → 429 `RETRY_COOLDOWN`
- #5 RESUME_BASED skeleton 부재 + retry → 409 `INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED`
- #7 replan 정상 호출 → 200, plan row 갱신
- #7 RESUME_BASED 외 호출 → 400 `INTERVIEW_NOT_RESUME_BASED`
- #7 skeleton 부재 호출 → 409 `RESUME_PLAN_NOT_READY`

### 의존
- 선행: Phase 2/3/4 (모든 기능 구현 완료)

### Verification
- `./gradlew test` 전체 green
- `./gradlew build` 통과
- BE CI 그린
- tech-spec.md Verification 항목 모두 충족

### 커밋 메시지
```
test(BE): interview 보안/안정성/replan 통합 테스트 + 외부화 설정
```

---

## 통합 Verification

- [ ] `tech-spec.md` Verification 항목 모두 통과
- [ ] Flyway V10 마이그레이션 zero-downtime 적용 (NOT NULL DEFAULT 0)
- [ ] BE CI green
- [ ] PR diff = 7건 (#1~#5, #7, #8) 모두 반영. #6 미포함

## 리뷰 게이트

- [ ] `code-reviewer-backend` 실행 (BE only)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md`, `testing.md`)
- [ ] Pre/Post State diff 일치
- [ ] 보안 회귀 테스트 (#1~#4) 모두 작성

## PR / 커밋 전략

- 단일 PR. base: `develop`. branch: `fix/interview-domain-findings-bundle`
- 5 커밋 (Phase 별 1)
- PR title: `[BE] fix: interview 도메인 발견 이슈 7건 정비 (#404)`
- `Closes #404`

## 비고

- #6 (`@Version`) 별도 결정 후 진행. 보류 기간 race 위험 인수 (tech-spec Trade-offs 참조)
- 운영 데이터 `interview_cs_sub_topics` 사용 값 조사 = Phase 1 시작 직전 1회. 결과 따라 enum 확정
- audio 매직바이트 검증 = stream 첫 12 byte. 큰 파일 통째 read 금지
- `@Transactional` 경계: counter 증가 = tx 내, AI 호출 = tx 외부 (긴 lock 방지)
