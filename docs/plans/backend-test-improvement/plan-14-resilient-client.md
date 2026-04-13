# Plan 14: ResilientAiClient 테스트 `[blocking]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Phase 4 완성. 이중 클라이언트(OpenAI→Claude) 폴백, 에러 분류(retryable vs non-retryable), 오디오 STT 폴백 검증. 외부 AI 서비스 장애 시 사용자 영향을 최소화하는 핵심 레질리언스 계층.

## 의존성

- 선행: Plan 13 (ClaudeApiClient 테스트 완료)
- 후행: 없음 (Phase 4 최종)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../infra/ai/ResilientAiClientTest.java` | 신규 생성 (~18 tests) |

## 상세

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `OpenAiClient` (nullable), `ClaudeApiClient` (nullable), `SttService` (nullable)
주의: 생성자 파라미터가 nullable이므로 `@Mock` 대신 직접 생성하여 null/non-null 조합 테스트

### @Nested 그룹

**Initialization**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `init_bothNull_throwsIllegalStateException` | 둘 다 null | 예외 |
| 2 | `init_openAiOnly_succeeds` | OpenAI만 | 정상 초기화 |
| 3 | `init_claudeOnly_succeeds` | Claude만 | 정상 초기화 |
| 4 | `init_bothPresent_succeeds` | 둘 다 존재 | 정상 초기화 |

**GenerateQuestions**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 5 | `generateQuestions_openAiSuccess_returnsDirect` | OpenAI 성공 | Claude 미호출, 직접 반환 |
| 6 | `generateQuestions_openAiFails_fallbackToClaude` | OpenAI 실패 | Claude 폴백 성공 |
| 7 | `generateQuestions_bothFail_throwsServiceUnavailable` | 둘 다 실패 | SERVICE_UNAVAILABLE |
| 8 | `generateQuestions_clientError_noFallback` | CLIENT_ERROR | 폴백 없이 즉시 예외 |
| 9 | `generateQuestions_parseFailed_noFallback` | PARSE_FAILED | 폴백 없이 즉시 예외 |
| 10 | `generateQuestions_openAiNull_directClaude` | OpenAI null | Claude 직접 호출 |

**GenerateFollowUp**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 11 | `generateFollowUp_openAiSuccess_returnsDirect` | 정상 | 직접 반환 |
| 12 | `generateFollowUp_openAiFails_fallbackToClaude` | 폴백 | Claude 호출 |

**GenerateFollowUpWithAudio**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 13 | `generateFollowUpWithAudio_openAiSuccess_returnsDirect` | 정상 | 직접 반환 |
| 14 | `generateFollowUpWithAudio_fallbackWithSttAndClaude` | OpenAI 실패 | STT → Claude 폴백 |
| 15 | `generateFollowUpWithAudio_sttNull_throwsServiceUnavailable` | SttService null | SERVICE_UNAVAILABLE |
| 16 | `generateFollowUpWithAudio_sttFails_throwsServiceUnavailable` | STT 실패 | SERVICE_UNAVAILABLE |
| 17 | `generateFollowUpWithAudio_sttAndClaudeFail_throwsDualFailure` | 이중 장애 | SERVICE_UNAVAILABLE |
| 18 | `generateFollowUpWithAudio_transcriptPassedToClaude` | STT 결과 | 텍스트가 Claude에 전달 |

## 담당 에이전트

- Implement: `test-engineer` — 1개 파일 작성
- Review: `architect-reviewer` — 폴백 전략 정합성, nullable 의존성 처리

## 검증

- [ ] `./gradlew test --tests "ResilientAiClientTest"` 통과
- [ ] nullable 생성자 파라미터 시나리오 전부 커버 (null/non-null 조합)
- [ ] CLIENT_ERROR, PARSE_FAILED → non-retryable (폴백 없음) 검증
- [ ] @Nested 그룹 4개
- [ ] `progress.md` 상태 업데이트 (Plan 14 → Completed)
