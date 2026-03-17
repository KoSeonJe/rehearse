# Task 2: AI 질문 품질 + 후속질문

## Status: Not Started

## Why

AI 면접의 핵심 가치는 질문 품질. 현재 하나의 질문에 여러 주제가 섞이고, 후속질문이 아예 작동하지 않아 실제 면접 경험과 괴리가 큼. 질문 수도 고정적이라 면접 시간 활용이 비효율적.

## Issues

| # | 제목 | 타입 |
|---|------|------|
| #49 | AI 면접 질문 품질 개선 (단일 질문, 맥락, 의도) | enhancement |
| #48 | 후속질문이 작동하지 않는 문제 | bug |
| #50 | 면접 시간 기반 질문 수 동적 생성 개선 | enhancement |

## 구현 계획

### PR 1: [BE] — 프롬프트 튜닝 + 후속질문 수정 + 동적 질문수 (#49, #48-BE, #50)

1. **질문 품질 개선** (`ClaudePromptBuilder.java`)
   - 시스템 프롬프트에 "하나의 질문에 반드시 하나의 주제만" 지시 추가
   - "이전 질문과 자연스러운 맥락 연결" 지시 추가
   - 각 질문에 `intent` 필드 추가 (출제 의도)

2. **후속질문 수정** (`InterviewService.java`)
   - follow-up 엔드포인트 정상 동작 검증
   - `answerText` 빈 값일 때 fallback 처리
   - 프롬프트에서 후속질문 생성 조건 명확화

3. **동적 질문 수** (`ClaudePromptBuilder.java`)
   - 후속질문 포함 시간 계산: 메인질문 + 후속질문(최대 3라운드) ≈ 8~10분/질문
   - "예비 질문" 개념: 넉넉히 생성 후 시간 초과 시 자연스럽게 종료

관련 파일:
- `backend/src/.../ClaudePromptBuilder.java`
- `backend/src/.../InterviewService.java`
- `backend/src/.../InterviewController.java`

**Agent**: `backend` (구현), `architect-reviewer` (리뷰)

### PR 2: [FE] — 후속질문 UI 수정 (#48-FE)

1. `use-answer-flow.ts` 디버깅
   - `canDoMoreFollowUps` 조건 검증
   - follow-up API 호출 트리거 확인
   - `handleStopAnswer()` → follow-up 플로우 연결

2. 후속질문 UI 상태 관리
   - 후속질문 로딩 상태 표시
   - 후속질문 실패 시 에러 핸들링

관련 파일:
- `frontend/src/hooks/use-answer-flow.ts`
- `frontend/src/stores/interview-store.ts`

**Agent**: `frontend` (구현), `code-reviewer` (리뷰)

## Acceptance Criteria

- [ ] 각 질문이 단일 주제만 포함
- [ ] 질문 간 맥락이 자연스럽게 연결
- [ ] 후속질문이 정상 작동 (최대 3라운드)
- [ ] 빈 답변 시에도 후속질문 또는 다음 질문으로 진행
- [ ] 면접 시간에 비례하여 질문 수가 동적 조절
- [ ] BE follow-up 엔드포인트 단독 테스트 통과
