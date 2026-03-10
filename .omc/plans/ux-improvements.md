# UX 개선

- **Status**: Completed
- **Created**: 2026-03-10
- **Branch**: feat/ux-improvements

## 목표
UI/UX 평가 기반 Critical + High 우선순위 7건 개선. API 계약 변경 없음.

## 작업 항목

### U1: VAD 자동 답변 감지
- 수동 "답변 시작" → VAD 기반 자동 감지로 전환
- `useAudioAnalyzer`의 AnalyserNode 활용
- "준비 완료" 버튼 → VAD 활성화 → 음성 감지 시 자동 녹음 시작
- 3초 무음 시 자동 일시정지, 재음성 시 자동 재개

### U2: 면접 중 질문 항상 보이게
- `QuestionDisplay`에 sticky 적용
- `VideoPreview`에 질문 텍스트 오버레이 (텔레프롬프터 스타일)

### U3: prefers-reduced-motion 접근성
- `index.css`에 reduced-motion 미디어 쿼리 추가
- WCAG 2.1 AA 준수

### U4: 피드백 리뷰 auto-scroll + 하이라이트
- 활성 피드백 카드 자동 스크롤
- opacity 변경 → accent left-border + shadow 강화

### U5: 면접 완료 가짜 프로그레스바 → 단계별 표시
- 하드코딩 60% 프로그레스바 제거
- 시간별 분석 단계 표시 (답변 분석 → 비언어 확인 → 피드백 생성)

### U6: 모바일 면접 컨트롤 하단 고정
- `InterviewControls`에 모바일 fixed bottom 적용
- 데스크톱은 기존 레이아웃 유지

### U7: 온보딩 가이드 스텝 제거
- 3단계 → 2단계 (직무 선택 + 디바이스 테스트)
- `TOTAL_STEPS = 2`, 가이드 관련 코드 제거

## 검증
- `npm run build` 성공
- `npm run lint` 통과
