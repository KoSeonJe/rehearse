# Task 0: Legacy 파이프라인 정리

## Status: Not Started

## Issue: #77

## Why

새 녹화-분석-피드백 파이프라인 구현 전, 기존 클라이언트 실시간 분석 코드(MediaPipe, Web Audio, feedback 도메인)를 제거해야 함.
남아있으면 타입 충돌, 불필요한 의존성, Store 오염이 발생하여 새 파이프라인 구현이 어려워짐.

상세 스펙: `.omc/plans/2026-03-17-legacy-pipeline-removal.md`

## 의존성

- 선행: 없음 (Phase 0, blocking)
- 후행: Task 1~10 모두 이 태스크 완료 후 진행

## 유지 대상 (제거 금지)

| 파일 | 이유 |
|------|------|
| `use-speech-recognition.ts` | 후속 질문 생성에 STT 필수 |
| `use-media-recorder.ts` | 영상 녹화 유지 (Task 8에서 개조) |
| `use-media-stream.ts` | 카메라/마이크 스트림 획득 |
| `lib/video-storage.ts` | S3 전환 전까지 유지 |
| `InterviewService` (BE) | 면접 생성 + 질문 생성 |
| `ReportService` (BE) | 리포트 생성 |

## 구현 계획

### PR 1: [FE] Legacy 클라이언트 분석 코드 제거

**파일 삭제 (16개):**
- MediaPipe 5개: `use-face-mesh.ts`, `use-pose-detection.ts`, `lib/mediapipe/*`
- Audio 2개: `use-audio-analyzer.ts`, `audio-level-indicator.tsx`
- Review 8개: `interview-review-page.tsx`, `components/review/*`, `review-store.ts`, `use-video-sync.ts`

**코드 수정:**
- `types/interview.ts`: NonVerbalEvent, VoiceEvent, 관련 필드 제거
- `interview-store.ts`: 비언어/음성 필드 및 액션 제거
- `interview-page.tsx`: audio 관련 코드 제거
- `interview-complete-page.tsx`: nonVerbalSummary, voiceSummary 로직 제거
- `app.tsx`: review 라우트 제거

**NPM 패키지:**
- `@mediapipe/tasks-vision` 제거

**검증:** `npm run build` + `npm run lint` 통과

- Implement: `frontend`
- Review: `code-reviewer` — 미참조 import 잔존 확인

### PR 2: [BE] feedback 도메인 전체 제거

**폴더 삭제:**
- `domain/feedback/` 전체 (controller, service, entity, dto, repository, exception)
- 관련 테스트

**코드 수정:**
- `AiClient`: `generateFeedback()` 시그니처 제거
- `ClaudeApiClient`: `generateFeedback()` 구현 + `MAX_TOKENS_FEEDBACK` 제거
- `MockAiClient`: `generateFeedback()` Mock 제거
- `ClaudePromptBuilder`: feedback 프롬프트 메서드 제거
- `ReportService`: FeedbackRepository 의존성 확인/제거

**검증:** `./gradlew build` + 전체 테스트 통과

- Implement: `backend`
- Review: `architect-reviewer` — 레이어 정합성, 미참조 확인

## Acceptance Criteria

- [ ] FE 빌드 에러 0, 린트 에러 0
- [ ] BE 빌드 + 테스트 전체 통과
- [ ] 면접 생성 → 질문 출제 → STT → 후속 질문 생성 플로우 정상 동작
- [ ] `@mediapipe/tasks-vision` 패키지 제거됨
- [ ] `domain/feedback/` 폴더 완전 삭제됨
