# AI 면접 종료 유도 기능

- **Status**: TODO
- **Date**: 2026-03-10
- **영역**: FE/BE
- **선행 작업**: setup-enhancement-duration.md (Completed)

## Why

현재는 FE 타이머가 시간 만료를 감지하고 강제 종료하는 방식.
AI가 자연스럽게 "면접을 마치겠습니다"라고 안내하면 UX가 훨씬 자연스러움.

## 현재 아키텍처 한계

- 면접 진행: FE에서 질문별 순차 진행 (질문 → 답변 → 다음 질문)
- 후속 질문 생성: 개별 API 호출 (`/follow-up`)
- AI가 남은 시간을 알 수 없음 → 종료 판단 불가

## 구현 방안

### BE
1. 후속 질문 API (`POST /api/v1/interviews/{id}/follow-up`)에 `remainingSeconds` 파라미터 추가
2. `ClaudePromptBuilder.buildFollowUpUserPrompt`에 남은 시간 정보 포함
3. AI 프롬프트에 지침 추가: "남은 시간이 2분 이하이면 마무리 질문을 하세요"
4. `GeneratedFollowUp` 응답에 `isClosing: boolean` 플래그 추가

### FE
1. 후속 질문 요청 시 `remainingSeconds` 계산하여 전달
2. `isClosing: true` 응답 시 → 해당 질문을 마지막으로 처리
3. AI 응답이 "면접을 마치겠습니다" 류이면 자동으로 종료 플로우 진입

## 검증
- 남은 시간 3분일 때 후속 질문 생성 → 일반 질문
- 남은 시간 1분일 때 후속 질문 생성 → 마무리 질문 + isClosing: true
- isClosing 수신 시 FE가 자동 종료 플로우 진입
