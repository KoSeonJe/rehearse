# Handoff — 404-interview-domain-findings

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 / Phase 5 완료 상태 (모든 Phase 종료)
> **다음 세션**: plan 폴더 진입 시 **이 파일 먼저 읽음**

---

## 현재 상태

- 진행: `implement.md` **Phase 5 완료** — 모든 Phase 종료. 커밋 `b2f704f` (`test(BE): interview 외부화 설정 + WebMvcTest 슬라이스 보강`)
- 브랜치: `fix/interview-domain-findings-bundle` (워크트리 `/Users/koseonje/dev/devlens-interview-fix`)
- 관련 PR: **미생성** (다음 세션에서 PR 생성)
- 빌드: `./gradlew build` SUCCESSFUL
- 테스트: `./gradlew test` 전체 green
- Flyway migrate 로컬 미실행 (Phase 1 자동화 스킵 — 필요 시 사용자 별도 확인)

### 커밋 이력 (Phase 별)

1. `8086406` Phase 1 — `feat(BE): interview 도메인 retry 컬럼 + CsSubTopic 이동 + ErrorCode 정비`
2. `d925a63` Phase 2 — `feat(BE): interview userId NULL 권한 우회 차단 + audio 입력 검증 도입`
3. `e1c5040` Phase 3 — `feat(BE): csSubTopics enum 적용 + retry 한도/쿨다운/skeleton 분리`
4. `ac586fe` Phase 4 — `feat(BE): interview replan API 추가 — 운영 SQL 없이 plan 정정`
5. `b2f704f` Phase 5 — `test(BE): interview 외부화 설정 + WebMvcTest 슬라이스 보강`

### Phase 1 변경 파일 6개

- **신규**: `backend/src/main/resources/db/migration/V45__add_question_gen_retry_to_interviews.sql`
- **이동**: `domain/question/entity/CsSubTopic.java` → `domain/interview/entity/CsSubTopic.java` (4값 + categoryName 매핑 그대로)
- **수정**: `Interview.java` (retry 컬럼 2개 + `recordRetryAttempt()` helper)
- **수정**: `InterviewErrorCode.java` (`CANNOT_DELETE_COMPLETED` 제거 + INTERVIEW_012~019 8개 추가)
- **수정**: `CacheableQuestionProvider.java` (CsSubTopic import 갱신)
- **수정**: `backend/src/test/java/com/rehearse/api/domain/interview/entity/InterviewTest.java` (recordRetryAttempt 2케이스)

### Phase 2 변경 파일 7개

- **수정**: `Interview.java` (`validateOwner` userId NULL 거부 + `log.warn` / `@Slf4j`)
- **신규**: `validation/AudioValidator.java` (`@Component`, mime 화이트리스트 + maxBytes + 매직바이트 12 byte)
- **수정**: `InterviewController.java` (followUp endpoint `AudioValidator` 호출)
- **테스트 수정**: `InterviewTest.java` (`validateOwner` Nested 4 케이스 추가)
- **테스트 신규**: `AudioValidatorTest.java` (11 케이스)
- **테스트 수정**: `InterviewControllerTest.java` (`@MockitoBean AudioValidator`)
- **테스트 수정**: `InterviewQueryServiceTest.java` (`FORBIDDEN INTERVIEW_008` → `NOT_FOUND INTERVIEW_001` 갱신)

### Phase 3 변경 파일 12개

- **신규**: `domain/interview/service/InterviewRetryRecorder.java` (`@Component` + `@Transactional` retry counter 증가 분리, self-invoke 회피)
- **수정**: `Interview.java` (csSubTopics `Set<CsSubTopic>`)
- **수정**: `CreateInterviewRequest.java` (`List<CsSubTopic>`)
- **수정**: `InterviewResponse.java`
- **수정**: `InterviewListResponse.java`
- **수정**: `QuestionGenerationRequestedEvent.java` (`List<CsSubTopic>` 페이로드)
- **수정**: `QuestionGenerationEventHandler.java` (line 41-46 enum→String 변환)
- **수정**: `InterviewService.java` (`retryQuestionGeneration`: skeleton 부재 → counter/cooldown → `recorder.record` → 이벤트 발행)
- **수정**: `GlobalExceptionHandler.java` (`HttpMessageNotReadableException` → `CS_SUB_TOPIC_INVALID` 매핑)
- **테스트 수정**: `InterviewServiceTest.java` (retry 4 케이스 추가)
- **테스트 수정**: `QuestionGenerationEventHandlerTest.java`
- **테스트 수정**: `InterviewRepositoryTest.java` (한국어 fixture → enum 4값 갱신)

### Phase 4 변경 파일

- **신규**: `dto/ReplanResponse.java` (record — interviewId / planId / replaced)
- **수정**: `controller/InterviewController.java` (POST /replan endpoint 추가)
- **수정**: `service/ResumePlanPreparationService.java` (`replan(interviewId, userId)` `@Transactional`)
- **테스트 신규**: `ResumePlanPreparationServiceTest` — 6 케이스 (정상 / RESUME_BASED 아님 400 / skeleton 부재 409 / ...)
- **테스트 수정**: `InterviewControllerTest.java` (`@MockitoBean ResumePlanPreparationService` 추가, 회귀 차단)

### Phase 5 변경 파일

- **신규**: `global/config/InterviewProperties.java` (`@ConfigurationProperties("rehearse.interview")` record — Retry / Audio nested)
- **수정 main**: `InterviewService.java` (코드 상수 → properties 주입), `AudioValidator.java` (DEFAULT 상수 → properties 주입), `application.yml` (`rehearse.interview.retry` / `audio` key 추가)
- **수정 test**: `InterviewControllerTest.java` (Replan 3 / Retry +3 / FollowUp audio +3 / publicId mock NOT_FOUND 갱신), `InterviewServiceTest.java` (InterviewProperties Spy), `AudioValidatorTest.java` (properties 직접 생성 주입)

### 사전 결정 사항 (재진입 시 참조)

- **Flyway 버전**: 마지막 V44. implement.md `V10` 오기재 → **V45** 사용 확정
- **CsSubTopic enum**: `domain/question/entity` → `domain/interview/entity` 이동 + 4값 유지 (DATA_STRUCTURE / OS / NETWORK / DATABASE). 운영 DB distinct 확인 완료
- **V45 SQL 테이블명**: `interview` (단수). entity `@Table(name="interview")` + 기존 마이그레이션 모두 단수. implement.md 의 `interviews` 오기재 정정
- **`INTERVIEW_009` 슬롯 비어 있음** — 재사용 정책 미정 (운영 무영향)
- **enum 적용 경계**: enum 적용 = **interview 도메인만**. `QuestionGenerationEventHandler.java:41-46` 경계에서 `enum.name()` → `List<String>` 변환. question 도메인 + `infra/ai` 시그니처 변경 X. (사용자 확정 옵션 A)
- **운영 DB 호환**: `interview_cs_sub_topics.cs_sub_topic` 저장값 = `enum.name()` 일치 (대문자 4값 DATA_STRUCTURE/OS/NETWORK/DATABASE) → `@Enumerated(EnumType.STRING)` zero-downtime 호환. spec 의 PromptBuilder enum 시그니처 미준수 = 의미적 동일성 OK.
- **`InterviewErrorCode.FORBIDDEN` (`INTERVIEW_008`)**: 보존 확정. 이번 PR 스코프 외. `INTERVIEW_009` 슬롯과 동일 정책. (사용자 확정)
- **InterviewProperties 위치**: implement.md spec `domain/interview/config/` → 컨벤션 위반. `global/config/InterviewProperties.java` 로 배치 (기존 `IntentClassifierProperties` 패턴 준수). 컨벤션 우선.
- **외부화 yml 환경별 상속**: base `application.yml` default 값 (5/30/10MB/300s/4 MIME) 모든 profile (`local/dev/prod/test/loadtest/llm-e2e`) 자동 상속. profile 별 override 미적용. `@ConfigurationPropertiesScan("com.rehearse.api.global.config")` 빈 자동 등록.
- **handoff 미해결 `getInterviewByPublicId_otherUser_forbidden`**: Phase 5 에서 mock 응답 NOT_FOUND 로 갱신 + 테스트명 `_otherUser_notFound` 로 rename → 실거동 일치. 해소 완료.

## 다음 세션 시작점

- **다음 작업**: **PR 생성** — `[BE] fix: interview 도메인 발견 이슈 7건 정비 (#404)` / `Closes #404` / base `develop` / head `fix/interview-domain-findings-bundle` / 5 커밋
- **첫 명령**: `git-manager` agent 위임 (`/create-pr` 스킬)

## 미해결 질문 / Blocker

- **`INTERVIEW_009` 슬롯 공석**: 재사용 정책 미정. 운영 무영향. 본 PR 외 별도 결정.
- **#6 `@Version` 낙관락**: 별도 Issue 결정 후 진행. 이번 PR 미포함 (tech-spec Trade-offs 인수).

## 컨텍스트 메모

- **워크트리**: `/Users/koseonje/dev/devlens-interview-fix` (브랜치 `fix/interview-domain-findings-bundle`). 메인 (`/Users/koseonje/dev/devlens`, `develop`) 와 별개
- **메인 디렉토리 변경 제외**: 404 plan 폴더 / 409 plan 폴더는 메인에 untracked 로 잔존 (PR 분리 — `chore/claude-meta-update` PR #414 별도 진행 / 머지 완료)
- **통합 리뷰 1회**: Phase 별 리뷰 X. 구현 종료 후 `code-reviewer-backend` 1회 (PR 직전)
- **audio 매직바이트**: stream 첫 12 byte read. 큰 파일 통째 read 절대 금지 (메모리 문제)
- **`@Transactional` 경계**: counter 증가 = tx 내 (짧은 tx) / AI 호출 = tx 외부. self-invoke 방지 위해 별도 컴포넌트 분리 고려
- **retry self-invoke 회피**: `InterviewRetryRecorder` 별도 컴포넌트 (`@Transactional` 분리). AI 호출은 `@TransactionalEventListener(AFTER_COMMIT)` 으로 자연 분리.
- **`RESUME_PLAN_RECOVERY_REQUIRED` 실제 코드명**: `InterviewErrorCode.RESUME_PLAN_RECOVERY_REQUIRED` (INTERVIEW_014). implement.md 의 `INTERVIEW_RESUME_PLAN_RECOVERY_REQUIRED` 는 spec 작성 시 prefix 차이.
- **Phase 2 #1 보안**: `userId == null` → `INTERVIEW_NOT_FOUND` (404). `FORBIDDEN` 노출 X (정보 누출 방지)
- **Phase 2 #3 audio**: mime / 길이 / 매직바이트 셋. mime whitelist + maxBytes / maxDurationSeconds 는 Phase 5 외부화 전까지 코드 상수 (default 값 유지)
- **Phase 의존 관계**: Phase 3 / 4 = Phase 2 와 의존 X (병렬 가능). Phase 5 = Phase 2/3/4 완료 후
- **Phase 5 옵션 A 채택**: 외부화 + 슬라이스 보강. 신규 infra 0. 본격 ServiceIntegrationTest 미작성 (별도 plan 분리 가능).
- **WebMvcTest 슬라이스 14 cases 추가**: controller 경계 회귀 보장. Service-Level 회귀 = unit test (Spy InterviewProperties) 커버.

### 통합 Verification (implement.md 항목 8개)

- [x] tech-spec Verification 통과 (단위/슬라이스 회귀)
- [x] Flyway V45 zero-downtime 적용 (NOT NULL DEFAULT 0)
- [x] BE CI green 예상 (`./gradlew test` + `./gradlew build` 로컬 통과)
- [x] PR diff = 7건 (#1~#5, #7, #8) 반영. #6 미포함

## 참고 명령

```bash
# 워크트리 진입
cd /Users/koseonje/dev/devlens-interview-fix/backend

# Phase 3 회귀 보장 (Phase 4 시작 직전 필수)
./gradlew test

# 전체 빌드
./gradlew build
```

---

업데이트: 2026-05-07 (Phase 5 완료 / PR 생성 대기)
