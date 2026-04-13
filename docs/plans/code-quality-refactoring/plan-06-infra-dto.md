# Plan 06: infra.ai.dto 프로바이더별 패키지 분리

> 상태: Draft
> 작성일: 2026-04-13

## Why

`infra/ai/dto/`에 11개 DTO가 프로바이더 구분 없이 평탄하게 배치되어 있다. Claude/OpenAI 전용 DTO와 공유 DTO를 분리하여 탐색 용이성을 높인다.

## 전제조건: 테스트 확인

패키지 이동만 수행하므로 로직 변경 없음. import 업데이트 후 `./gradlew compileJava` 통과가 검증 기준.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `infra/ai/dto/claude/ClaudeRequest.java` | dto/에서 이동 |
| `infra/ai/dto/claude/ClaudeResponse.java` | dto/에서 이동 |
| `infra/ai/dto/claude/SystemContent.java` | dto/에서 이동 |
| `infra/ai/dto/claude/CacheControl.java` | dto/에서 이동 |
| `infra/ai/dto/openai/OpenAiRequest.java` | dto/에서 이동 |
| `infra/ai/dto/openai/OpenAiResponse.java` | dto/에서 이동 |
| `infra/ai/ClaudeApiClient.java` | import 변경 |
| `infra/ai/OpenAiClient.java` | import 변경 |
| `infra/ai/ResilientAiClient.java` | import 변경 |
| `infra/ai/AiResponseParser.java` | import 변경 |
| 관련 테스트 파일 | import 업데이트 |

공유 DTO(GeneratedQuestion, GeneratedFollowUp 등 5개)는 `infra/ai/dto/` 루트에 잔류.

## 담당 에이전트

- Implement: `backend` — 패키지 이동
- Review: `code-reviewer` — import 누락 검증

## 검증

- `./gradlew compileJava` — 컴파일 에러 없음
- `./gradlew test` — 전체 통과
- `progress.md` 상태 업데이트 (Task 6 → Completed)
