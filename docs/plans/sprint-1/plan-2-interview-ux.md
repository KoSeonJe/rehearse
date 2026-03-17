# Task 3: 면접 페이지 UX

## Status: Not Started

## Why

면접 진행 중 사용자가 종료하고 싶을 때 방법을 찾기 어렵고, 질문 생성 대기 시 단순 텍스트 변경만으로는 이탈 가능성이 높음. 면접 경험의 기본적인 UX 품질 확보 필요.

## Issues

| # | 제목 | 타입 |
|---|------|------|
| #51 | 면접 중간 종료 버튼 UX 개선 | enhancement |
| #52 | 면접 질문 생성 중 로딩 UX 개선 | enhancement |

## 구현 계획

### PR 1: [FE] — 면접 페이지 UX 개선 (#51, #52)

1. **중간 종료 UX** (#51)
   - 상단 고정 헤더에 명확한 "면접 종료" 버튼 배치
   - 종료 확인 모달: "정말 종료하시겠습니까? 현재까지의 답변은 저장됩니다."
   - 종료 시 현재까지 답변한 내용으로 피드백/리포트 생성 가능하도록 처리
   - 관련: `interview-controls.tsx`, `interview-page.tsx`

2. **로딩 UX** (#52)
   - 단계별 진행 표시: "이력서 분석 중..." → "질문 생성 중..." → "거의 완료..."
   - 예상 소요시간 표시 ("약 15~30초 소요")
   - 로딩 중 면접 팁/안내 메시지 순환 표시
   - 스켈레톤 UI 또는 애니메이션 로딩
   - 관련: `interview-setup-page.tsx`

관련 파일:
- `frontend/src/components/interview/interview-controls.tsx`
- `frontend/src/pages/interview-page.tsx`
- `frontend/src/pages/interview-setup-page.tsx`

**Agent**: `frontend` (구현), `code-reviewer` + `designer` (리뷰)

## Acceptance Criteria

- [ ] 면접 중 상단에 "종료" 버튼이 항상 보임
- [ ] 종료 시 확인 모달 표시
- [ ] 중간 종료 후 현재까지 답변으로 피드백 요청 가능
- [ ] 질문 생성 중 단계별 진행 상태 표시
- [ ] 예상 소요시간 안내 표시
- [ ] 로딩 중 면접 팁 메시지 순환
