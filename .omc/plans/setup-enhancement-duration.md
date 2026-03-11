# Setup 페이지 개선 + 면접 시간 설정 + 서버 오류 수정

- **Status**: Completed
- **Date**: 2026-03-10
- **영역**: FE/BE

## Why

사용자 피드백 반영:
1. RESUME_BASED가 숨겨져 있어 이력서 기반 면접을 명시적으로 선택할 수 없음
2. 면접 시간을 설정할 수 없어 질문 수 조정이 불가능
3. 이력서 없이 "건너뛰고 면접 시작하기" 시 서버 오류 발생

## 변경 요약

### BE
- `Interview` 엔티티: `durationMinutes` 필드 추가
- `CreateInterviewRequest`: `durationMinutes` 검증 (@Min(5) @Max(120))
- `InterviewResponse`: `durationMinutes` 반환
- `ClaudePromptBuilder`: 시간 비례 질문 수 계산 (분/5 반올림)
- `MockAiClient`: 동적 질문 수 반환
- `InterviewController`: resumeFile 없이 전송 시 오류 수정 확인

### FE
- `interview.ts`: `durationMinutes` 타입 추가
- Setup 위저드 5스텝: 직무 → 레벨 → 시간 → 유형(RESUME_BASED 포함) → 이력서(조건부)
- `InterviewTimer`: 남은 시간 표시 + 2분전 경고 + 자동 종료
- `interview-ready-page`: 면접 시간 표시

### 스텝 순서
```
Step 1: 직무 선택
Step 2: 레벨 선택
Step 3: 면접 시간 입력 (신규, 기본 30분, 5~120분)
Step 4: 면접 유형 선택 (RESUME_BASED 포함)
Step 5: 이력서 업로드 (RESUME_BASED 선택 시만)
```

## 관련 후속 작업
- AI 면접 종료 유도: `ai-interview-closing.md` (TODO)

## 검증
- `./gradlew clean test` — PASSED
- `npx tsc --noEmit` — PASSED
