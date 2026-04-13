# Plan 13: ClaudeApiClient 테스트 `[blocking]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Claude API 호출의 재시도(3회, 지수 백오프), HTTP 상태별 분기(429/4xx/5xx), 응답 파싱 검증. 외부 장애 시 행위를 테스트로 보장.

## 의존성

- 선행: Plan 12 (AiResponseParser 테스트로 파서 행위 검증 완료)
- 후행: Plan 14 (ResilientAiClient가 이 클라이언트에 의존)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../infra/ai/ClaudeApiClientTest.java` | 신규 생성 (~16 tests) |

## 상세

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `RestClient` (빌더 체인 모킹), `QuestionGenerationPromptBuilder`, `FollowUpPromptBuilder`, `AiResponseParser`

### @Nested 그룹

**GenerateQuestions**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `generateQuestions_success_returnsQuestions` | 정상 응답 | 질문 리스트 반환 |
| 2 | `generateQuestions_emptyResponse_throwsException` | 빈 응답 | EMPTY_RESPONSE 예외 |
| 3 | `generateQuestions_nullQuestionsList_throwsException` | 파싱 후 null | PARSE_FAILED 예외 |
| 4 | `generateQuestions_callsPromptBuilder` | 프롬프트 확인 | buildSystemPrompt + buildUserPrompt 호출 |

**GenerateFollowUp**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 5 | `generateFollowUp_success_returnsFollowUp` | 정상 응답 | GeneratedFollowUp 반환 |
| 6 | `generateFollowUp_usesHaikuModel` | 모델 확인 | FOLLOW_UP_MODEL 사용 |
| 7 | `generateFollowUp_usesSmallerMaxTokens` | 토큰 확인 | MAX_TOKENS_FOLLOW_UP(1024) |

**RetryBehavior**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 8 | `callApi_429_retriesUpToMaxAttempts` | 429 Rate Limit | 3회 재시도 |
| 9 | `callApi_5xx_retriesWithBackoff` | 500 서버 에러 | 재시도 + 지수 백오프 |
| 10 | `callApi_4xx_throwsImmediately` | 400 클라이언트 에러 | 재시도 없이 즉시 예외 |
| 11 | `callApi_allRetriesExhausted_throwsTimeout` | 3회 모두 실패 | TIMEOUT 예외 |
| 12 | `callApi_interruptDuringRetry_restoresFlag` | 재시도 중 인터럽트 | interrupt 플래그 복원 |

**ResponseHandling**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 13 | `callApi_nullResponse_throwsException` | 응답 null | 예외 발생 |
| 14 | `callApi_maxTokensStopReason_logsWarning` | max_tokens 도달 | 경고 로그 |
| 15 | `callApi_logsTokenUsage` | 정상 응답 | usage 로깅 |
| 16 | `callApi_promptCaching_setsSystemCacheControl` | 캐싱 설정 | SystemContent.withCaching() |

## 담당 에이전트

- Implement: `test-engineer` — 1개 파일 작성
- Review: `architect-reviewer` — 재시도/에러 분류 로직 정확성

## 검증

- [ ] `./gradlew test --tests "ClaudeApiClientTest"` 통과
- [ ] 429 → 재시도, 400 → 즉시 예외, 500 → 재시도 분기 검증
- [ ] 재시도 횟수 3회 검증
- [ ] @Nested 그룹 4개
- [ ] `progress.md` 상태 업데이트 (Plan 13 → Completed)
