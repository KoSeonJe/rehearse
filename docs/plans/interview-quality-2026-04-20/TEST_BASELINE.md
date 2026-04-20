# Test Baseline — Interview Quality 2026-04-20

> 실측일: 2026-04-20
> 실행 커맨드: `./gradlew test` (working dir: `/Users/koseonje/dev/devlens/backend`)
> Status: Completed

---

## 요약

| 항목 | 값 |
|------|----|
| 총 테스트 수 | 606 |
| 실패 | 0 |
| 무시(ignored) | 0 |
| 실행 시간 | 56s |
| BUILD 결과 | SUCCESS |
| 테스트 클래스 수 | 71개 |
| @Test 메서드 수 | ~586개 (나머지 ~20개는 parameterized 등) |

---

## 테스트 클래스 목록 (71개, 도메인별 그룹)

### Bootstrap (1개)
| 클래스 | 경로 |
|--------|------|
| `RehearseApiApplicationTest` | `com/rehearse/api/` |

### Interview 도메인 (22개)

| 클래스 | 경로 |
|--------|------|
| `InterviewTest` | `domain/interview/entity/` |
| `TechStackTest` | `domain/interview/entity/` |
| `QuestionPoolTest` | `domain/interview/generation/pool/entity/` |
| `CacheableQuestionProviderTest` | `domain/interview/generation/pool/service/` |
| `FreshQuestionProviderTest` | `domain/interview/generation/pool/service/` |
| `KeywordMatcherTest` | `domain/interview/generation/pool/service/` |
| `QuestionGenerationLockTest` | `domain/interview/generation/pool/service/` |
| `QuestionPoolServiceTest` | `domain/interview/generation/pool/service/` |
| `QuestionGenerationEventHandlerTest` | `domain/interview/generation/service/` |
| `QuestionGenerationServiceTest` | `domain/interview/generation/service/` |
| `QuestionGenerationTransactionHandlerTest` | `domain/interview/generation/service/` |
| `InterviewRepositoryTest` | `domain/interview/repository/` |
| `FollowUpServiceTest` | `domain/interview/service/` |
| `FollowUpTransactionHandlerTest` | `domain/interview/service/` |
| `InterviewCompletionServiceTest` | `domain/interview/service/` |
| `InterviewCreationServiceTest` | `domain/interview/service/` |
| `InterviewDeletionServiceTest` | `domain/interview/service/` |
| `InterviewFinderTest` | `domain/interview/service/` |
| `InterviewQueryServiceTest` | `domain/interview/service/` |
| `InterviewServiceTest` | `domain/interview/service/` |
| `InterviewControllerTest` | `domain/interview/controller/` |
| `QuestionDistributionTest` | `domain/interview/vo/` |

### Feedback 도메인 (2개)

| 클래스 | 경로 |
|--------|------|
| `TimestampFeedbackResponseTest` | `domain/feedback/dto/` |
| `TimestampFeedbackMapperTest` | `domain/feedback/service/` |

### QuestionSet 도메인 (11개)

| 클래스 | 경로 |
|--------|------|
| `AnalysisStatusTest` | `domain/questionset/entity/` |
| `ConvertStatusTest` | `domain/questionset/entity/` |
| `QuestionAnswerTest` | `domain/questionset/entity/` |
| `QuestionSetAnalysisTest` | `domain/questionset/entity/` |
| `QuestionSetDelegationTest` | `domain/questionset/entity/` |
| `QuestionSetFeedbackTest` | `domain/questionset/entity/` |
| `QuestionSetTest` | `domain/questionset/entity/` |
| `QuestionTest` | `domain/questionset/entity/` |
| `TimestampFeedbackTest` | `domain/questionset/entity/` |
| `AnalysisSchedulerTest` | `domain/questionset/service/` |
| `InternalQuestionSetServiceTest` | `domain/questionset/service/` |
| `QuestionSetServiceTest` | `domain/questionset/service/` |
| `InternalQuestionSetControllerTest` | `domain/questionset/controller/` |
| `QuestionSetControllerTest` | `domain/questionset/controller/` |

> 주의: 위 14개 중 `InternalQuestionSetControllerTest`, `QuestionSetControllerTest`를 포함하면 실제 14개이나, 도메인 그룹 합산 71개 기준으로 questionset은 14개로 간주.

### ReviewBookmark 도메인 (6개)

| 클래스 | 경로 |
|--------|------|
| `ReviewBookmarkControllerTest` | `domain/reviewbookmark/controller/` |
| `ReviewBookmarkEntityTest` | `domain/reviewbookmark/entity/` |
| `ReviewBookmarkRepositoryTest` | `domain/reviewbookmark/repository/` |
| `ReviewBookmarkFinderTest` | `domain/reviewbookmark/service/` |
| `ReviewBookmarkQueryServiceTest` | `domain/reviewbookmark/service/` |
| `ReviewBookmarkServiceTest` | `domain/reviewbookmark/service/` |

### ServiceFeedback 도메인 (3개)

| 클래스 | 경로 |
|--------|------|
| `AdminFeedbackControllerTest` | `domain/servicefeedback/controller/` |
| `ServiceFeedbackControllerTest` | `domain/servicefeedback/controller/` |
| `ServiceFeedbackServiceTest` | `domain/servicefeedback/service/` |

### Auth / Admin / User (3개)

| 클래스 | 경로 |
|--------|------|
| `AuthControllerTest` | `domain/auth/controller/` |
| `AdminControllerTest` | `domain/admin/controller/` |
| `UserServiceTest` | `domain/user/service/` |

### File 도메인 (3개)

| 클래스 | 경로 |
|--------|------|
| `FileMetadataTest` | `domain/file/entity/` |
| `InternalFileControllerTest` | `domain/file/controller/` |
| `InternalFileServiceTest` | `domain/file/service/` |

### AI Infrastructure (8개)

| 클래스 | 경로 |
|--------|------|
| `AiResponseParserTest` | `infra/ai/` |
| `ClaudeApiClientTest` | `infra/ai/` |
| `MockSttServiceTest` | `infra/ai/` |
| `ResilientAiClientTest` | `infra/ai/` |
| `PersonaResolverTest` | `infra/ai/persona/` |
| `FollowUpPromptBuilderTest` | `infra/ai/prompt/` |
| `QuestionCountCalculatorTest` | `infra/ai/prompt/` |
| `QuestionGenerationPromptBuilderTest` | `infra/ai/prompt/` |

### AWS / GCS / TTS 인프라 (4개)

| 클래스 | 경로 |
|--------|------|
| `AwsS3ServiceTest` | `infra/aws/` |
| `S3KeyGeneratorTest` | `infra/aws/` |
| `GoogleTtsClassLoadingTest` | `infra/google/` |
| `TtsControllerTest` | `infra/tts/` |

### Security / Global (5개)

| 클래스 | 경로 |
|--------|------|
| `GlobalRateLimiterFilterTest` | `global/config/` |
| `InternalApiKeyFilterTest` | `global/config/` |
| `GlobalExceptionHandlerTest` | `global/exception/` |
| `JwtAuthenticationFilterTest` | `global/security/jwt/` |
| `JwtTokenProviderTest` | `global/security/jwt/` |
| `OAuth2SuccessHandlerTest` | `global/security/oauth2/` |

---

## Spring Test 분류

| 애노테이션 | 클래스 수 | 비고 |
|-----------|----------|------|
| `@SpringBootTest` | 1 | `RehearseApiApplicationTest` — 컨텍스트 bootstrap만 |
| `@DataJpaTest` | 2 | `InterviewRepositoryTest`, `ReviewBookmarkRepositoryTest` |
| 순수 단위 테스트 | ~68 | Mockito/JUnit, Spring 컨텍스트 없음 |

---

## 주목할 헤비 테스트 클래스

| 클래스 | @Test 수 | 비고 |
|--------|----------|------|
| `AnalysisStatusTest` | ~30 | QuestionSet 상태 기계 전수 |
| `InterviewTest` | ~24 | Interview 도메인 규칙 전수 |
| AI 관련 합계 | ~48 | ResilientAiClientTest + ClaudeApiClientTest + AiResponseParserTest 등 |
| Interview 도메인 합계 | ~165 | 22개 클래스 |

---

## JaCoCo 상태

**현재 미설정**: `backend/build.gradle.kts`에 `jacoco` plugin 없음.
커버리지 baseline 측정 불가 상태.

### 권장 설정 (향후 적용 시 참고 템플릿)

```kotlin
// backend/build.gradle.kts
plugins {
    // ... 기존 플러그인 ...
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    // 엔티티/DTO/생성 코드 제외
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(
                "**/dto/**",
                "**/entity/**",
                "**/*Application*"
            )
        }
    )
}

// 커버리지 최소 기준 (신규 plan 도입 후 조정)
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}
```

---

## 최소 보호선 (Phase 1~4 각 plan 머지 기준)

| 기준 | 내용 |
|------|------|
| 회귀 금지 | 각 plan 머지 시 기존 606 tests 전부 passing 유지 |
| 숫자 증가 허용 조건 | 신규 테스트 추가 시에만 총 수 증가 허용 |
| 실패 허용 | 0건 (기존 테스트 실패 1건도 허용 안 함) |
| 측정 커맨드 | `./gradlew test` (backend 디렉토리에서) |
| 보고서 위치 | `backend/build/reports/tests/test/index.html` |

---

## 신규 테스트 계획 (각 plan 추가 예정)

| plan | 신규 테스트 클래스 |
|------|------------------|
| plan-00b | `AiClientChatTest`, `ResilientAiClientFallbackTest`, `AiResponseParserRetryTest` |
| plan-08 | `RubricLoaderTest`, `RubricScorerTest` |
| plan-11 | `test_nonverbal_rubric_mapper.py` (Python, Lambda) |
