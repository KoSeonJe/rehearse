# GPT-4o-mini-audio 후속질문 통합 — 진행 상황

## 태스크 상태

| # | 태스크 | Plan | 상태 | 태그 |
|---|--------|------|------|------|
| 1 | AiClient 인터페이스 + OpenAiClient audio 메서드 | plan-01 | Completed | [blocking] |
| 2 | ResilientAiClient fallback 경로 | plan-02 | Completed | |
| 3 | InterviewService 통합 | plan-03 | Completed | |
| 4 | DTO 수정 + 코드 정리 | plan-04 | Completed | [parallel] |
| 5 | 테스트 + 검증 | plan-05 | Completed | |

## 의존성

```
Task 1 (AiClient + OpenAiClient) [blocking]
  ├── Task 2 (ResilientAiClient fallback)
  │     └── Task 3 (InterviewService 통합)
  │           └── Task 5 (테스트)
  └── Task 4 (DTO + 정리) [parallel with Task 2]
```

## 진행 로그

### 2026-03-27
- 요구사항 정의 + 플랜 문서 작성
- GPT-4o-mini-audio / Gemini 2.5 Flash / Whisper 실측 비교 완료
- 방향 결정: Backend 후속질문만 GPT-audio 전환, Lambda는 Gemini 유지
- Task 1~5 전체 구현 완료
- 수정 파일:
  - `AiClient.java` — `generateFollowUpWithAudio()` 메서드 추가
  - `OpenAiClient.java` — GPT-4o-mini-audio-preview 호출 구현 + finish_reason 체크
  - `ResilientAiClient.java` — SttService 의존성 추가, audio fallback 경로(Whisper+Claude)
  - `MockAiClient.java` — `generateFollowUpWithAudio()` mock 구현
  - `GeneratedFollowUp.java` — `answerText` 필드 + `withAnswerText()` 추가
  - `FollowUpPromptBuilder.java` — `buildUserPromptForAudio()` 추가
  - `InterviewService.java` — `resolveAnswerText()` 삭제, `generateFollowUpWithAudio()` 직접 호출
  - `InterviewServiceTest.java` — 테스트 5개를 새 구조에 맞게 수정
- critic 리뷰 피드백 반영:
  - requirements.md에 answerText 영속화 설계 결정 문서화
  - requirements.md에 ClaudeApiClient 구조 설명 추가
  - plan-02 코드 스니펫을 실제 구현과 일치시킴 (isNonRetryableError, record 생성자)
  - plan-04 ClaudeApiClient "변경 없음" 명시, DTO 코드 스니펫 수정
  - OpenAiClient.callOpenAiAudioApi에 finish_reason 체크 추가
- `./gradlew test` 전체 통과 (BUILD SUCCESSFUL)
