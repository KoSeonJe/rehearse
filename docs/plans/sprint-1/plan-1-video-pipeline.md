# Task 1: 영상 파이프라인 재구축

## Status: Not Started

## Why

현재 답변 구간에서만 영상을 녹화하는 설계가 5개 이상 버그의 근본 원인.
타임스탬프 불일치(#56), A/V 싱크(#55), 초기 위치 오류(#53), 답변 병합(#54) 등은 모두 분리 녹화 방식에서 기인하므로 개별 패치가 아닌 파이프라인 전면 재구축이 필요.

## Issues

| # | 제목 | 타입 |
|---|------|------|
| #64 | 영상 파이프라인 시각화 문서 작성 | docs |
| #60 | 영상 녹화/저장/재생 파이프라인 전면 재구축 | enhancement |
| #57 | 답변 시에만 녹화 → 전체 면접 연속 녹화 전환 | enhancement |
| #56 | 타임스탬프 피드백이 실제 영상과 불일치 | bug |
| #55 | 영상과 음성 싱크 불일치 | bug |
| #54 | 자기소개와 질문1 답변이 함께 저장 | bug |
| #53 | 피드백 페이지 영상 초기 위치 ≠ 0초 | bug |

## 구현 계획

### PR 1: docs — 영상 파이프라인 시각화 문서 (#64)
- `docs/architecture/video-pipeline.md` 작성
- Mermaid 다이어그램: 녹화 → 저장 → 재생 → 분석 파이프라인
- 현재 문제점 + 재구축 후 목표 아키텍처 비교
- **Agent**: `documentation-expert`

### PR 2: [BE] — 타임스탬프 검증 로직 (#56)
- `ClaudePromptBuilder`: 피드백 타임스탬프가 영상 길이 초과하지 않도록 검증
- BE에서 `timestampSeconds` 상한값 검증 후 클램핑
- **Agent**: `backend` (구현), `architect-reviewer` (리뷰)

### PR 3: [FE] — 영상 파이프라인 재작성 (#60, #57, #55, #54, #53)
핵심 변경:
1. **연속 녹화**: 면접 시작~종료 단일 `MediaRecorder` 세션
   - `use-media-recorder.ts`: start/stop을 면접 생명주기와 동기화
   - `MediaRecorder.start()` 파라미터 없이 호출 → stop 시 단일 Blob
2. **타임라인 통합**: 녹화 타임코드 = 면접 타임라인
   - 질문 전환, 답변 시작/종료 시각을 녹화 시작 기준 상대 시간으로 기록
   - `interview-store.ts`: 타임라인 이벤트 배열 관리
3. **자기소개 분리**: `questionIndex` 체계 정비 (자기소개 = index 0, 질문 = 1~N)
4. **재생 초기화**: `video-player.tsx`에서 `onLoadedMetadata` 시 `currentTime = 0`
5. **A/V 싱크**: VP9+Opus 코덱 명시, 타임슬라이스 제거
6. **저장**: IndexedDB에 단일 Blob 저장, 대용량 대응

관련 파일:
- `frontend/src/hooks/use-media-recorder.ts`
- `frontend/src/hooks/use-interview-session.ts`
- `frontend/src/hooks/use-answer-flow.ts`
- `frontend/src/hooks/use-speech-recognition.ts`
- `frontend/src/stores/interview-store.ts`
- `frontend/src/lib/video-storage.ts`
- `frontend/src/components/review/video-player.tsx`
- `frontend/src/hooks/use-video-sync.ts`

**Agent**: `frontend` (구현), `architect-reviewer` + `code-reviewer` (리뷰)

## Acceptance Criteria

- [ ] 면접 전체가 단일 영상으로 녹화됨
- [ ] 영상 길이 = 면접 경과 시간
- [ ] 타임스탬프 피드백이 영상 내용과 일치
- [ ] A/V 싱크 정상
- [ ] 피드백 페이지 영상이 0초부터 시작
- [ ] 자기소개와 질문1 답변이 분리 저장됨
- [ ] BE 타임스탬프 검증 로직 존재
- [ ] 파이프라인 문서가 최신 상태
