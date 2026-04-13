# Plan 12: AiResponseParser 테스트 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Phase 4 시작. AI 응답 파싱은 모든 AI 클라이언트(`ClaudeApiClient`, `OpenAiClient`)의 기반. 마크다운 코드블록 추출, JSON 역직렬화, 에러 핸들링 검증.

## 의존성

- 선행: Plan 01-04 (컨벤션 확립)
- 후행: Plan 13 (ClaudeApiClient가 이 파서에 의존)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../infra/ai/AiResponseParserTest.java` | 신규 생성 (~12 tests) |

## 상세

테스트 유형: Unit (순수 Java, 실제 `ObjectMapper` 사용 — Mock 아님)

### @Nested 그룹

**ExtractJson** — 마크다운 추출 로직

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `extractJson_plainJson_returnsAsIs` | 마크다운 없음 | 입력 그대로 반환 |
| 2 | `extractJson_jsonCodeBlock_extractsContent` | ` ```json ... ``` ` | 내부 JSON만 추출 |
| 3 | `extractJson_codeBlockWithoutTag_extractsContent` | ` ``` ... ``` ` | json 태그 없어도 추출 |
| 4 | `extractJson_unclosedBlock_returnsRemainingText` | ` ``` ... (닫힘 없음)` | 나머지 텍스트 반환 + 경고 로그 |
| 5 | `extractJson_whitespace_trimmed` | 앞뒤 공백 | trim된 결과 |
| 6 | `extractJson_multipleBlocks_takesFirst` | 블록 2개 | 첫 번째 블록만 |

**ParseJsonResponse** — JSON 역직렬화

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 7 | `parseJsonResponse_validJson_deserializes` | 정상 JSON | DTO 반환 |
| 8 | `parseJsonResponse_markdownWrapped_deserializes` | 마크다운 래핑 | 추출 후 역직렬화 |
| 9 | `parseJsonResponse_invalidJson_throwsBusinessException` | 잘못된 JSON | PARSE_FAILED |
| 10 | `parseJsonResponse_emptyString_throwsBusinessException` | 빈 문자열 | PARSE_FAILED |
| 11 | `parseJsonResponse_nullField_handledGracefully` | 선택 필드 null | 정상 역직렬화 |
| 12 | `parseJsonResponse_differentTypes_worksGenerically` | 다른 DTO 클래스 | 제네릭 정상 동작 |

## 담당 에이전트

- Implement: `test-engineer` — 1개 파일 작성
- Review: `qa` — 마크다운 엣지 케이스 커버리지

## 검증

- [ ] `./gradlew test --tests "AiResponseParserTest"` 통과
- [ ] Mock 0개 (실제 ObjectMapper 사용)
- [ ] extractJson의 4가지 분기 전부 커버
- [ ] `progress.md` 상태 업데이트 (Plan 12 → Completed)
