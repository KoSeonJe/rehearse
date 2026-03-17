# Legacy 파이프라인 제거

## Status: Pending

## Why

새 녹화-분석-피드백 파이프라인(`docs/architecture/recording-analysis-pipeline.md`)을 구현하기 위해,
현재 클라이언트 실시간 분석 기반의 Legacy 파이프라인을 먼저 제거해야 한다.

현재 Legacy 구조는:
- **실시간 비언어 분석** (MediaPipe) → 새 파이프라인에서 GPT-4o Vision 서버 후처리로 대체
- **실시간 음성 분석** (Web Audio) → 새 파이프라인에서 Whisper + LLM 서버 후처리로 대체
- **실시간 피드백 생성** (Claude API 텍스트 요약) → 새 파이프라인에서 Lambda 기반 분석으로 대체
- **클라이언트 영상 저장** (IndexedDB) → 새 파이프라인에서 S3 업로드로 대체
- **피드백 리뷰 UI** (타임스탬프 동기화) → 새 파이프라인의 피드백 뷰어로 재구축

이 코드들이 남아있으면 새 파이프라인 구현 시 타입 충돌, 불필요한 의존성, Store 오염이 발생한다.
**먼저 정리하고 깨끗한 상태에서 새 파이프라인을 구축**하는 것이 올바른 순서다.

## Goal

- Legacy 실시간 분석 코드 완전 제거
- 새 파이프라인 구현에 방해되는 FE/BE 코드 정리
- 제거 후에도 **면접 진행 + 후속 질문 생성**은 정상 동작해야 함
- 빌드 에러 0, 기존 테스트 통과

## 유지해야 할 것 (절대 제거 금지)

| 파일 | 이유 |
|------|------|
| `use-speech-recognition.ts` | 후속 질문 생성에 STT 필수 |
| `use-media-recorder.ts` | 영상 녹화 유지 |
| `use-media-stream.ts` | 카메라/마이크 스트림 획득 |
| `lib/video-storage.ts` | S3 전환 전까지 유지 |
| `InterviewService` (BE) | 면접 생성 + 질문 생성 |
| `ReportService` (BE) | 리포트 생성 (피드백과 분리) |

---

## 제거 대상 전체 목록

### Frontend — 파일 삭제 (18개)

**MediaPipe 관련 (5개):**
- `frontend/src/hooks/use-face-mesh.ts`
- `frontend/src/hooks/use-pose-detection.ts`
- `frontend/src/lib/mediapipe/face-analyzer.ts`
- `frontend/src/lib/mediapipe/pose-analyzer.ts`
- `frontend/src/lib/mediapipe/event-detector.ts`

**Audio Analyzer (2개):**
- `frontend/src/hooks/use-audio-analyzer.ts`
- `frontend/src/components/interview/audio-level-indicator.tsx`

**리뷰 페이지 + 컴포넌트 (8개):**
- `frontend/src/pages/interview-review-page.tsx`
- `frontend/src/components/review/video-player.tsx`
- `frontend/src/components/review/feedback-timeline.tsx`
- `frontend/src/components/review/feedback-panel.tsx`
- `frontend/src/components/review/timeline-marker.tsx`
- `frontend/src/components/review/score-card.tsx`
- `frontend/src/components/review/improvement-list.tsx`
- `frontend/src/hooks/use-video-sync.ts`

**Store (1개):**
- `frontend/src/stores/review-store.ts`

### Frontend — 코드 수정 (4개 파일)

**`frontend/src/types/interview.ts`:**
- `NonVerbalEventType` 타입 제거
- `NonVerbalEvent` 인터페이스 제거
- `VoiceEvent` 인터페이스 제거
- `QuestionAnswer`에서 `nonVerbalEvents`, `voiceEvents` 필드 제거
- `AnswerData`에서 `nonVerbalSummary`, `voiceSummary` 필드 제거
- `FollowUpRequest`에서 `nonVerbalSummary` 필드 제거

**`frontend/src/stores/interview-store.ts`:**
- `NonVerbalEvent`, `VoiceEvent` import 제거
- state에서 `nonVerbalEvents`, `voiceEvents` 필드 제거
- `addNonVerbalEvent`, `addVoiceEvent` 액션 및 구현 제거
- initialState에서 해당 초기값 제거

**`frontend/src/pages/interview-complete-page.tsx`:**
- `nonVerbalSummary` 생성 로직 제거
- `voiceSummary` 생성 로직 제거
- `AnswerData`에 해당 필드 전달하는 부분 제거

**`frontend/src/app.tsx`:**
- `InterviewReviewPage` import 및 라우트 제거
- `InterviewReportPage`는 피드백 의존성 확인 후 판단

**`frontend/src/pages/interview-page.tsx`:**
- `useAudioAnalyzer()` 호출 제거
- `AudioLevelIndicator` 컴포넌트 제거
- audio 객체를 다른 hook에 전달하는 코드 제거

**`frontend/src/hooks/use-interview-session.ts`:**
- audio 파라미터 제거
- VoiceEvent 콜백 등록 코드 제거
- NonVerbalEvent 콜백 등록 코드 제거

### Backend — 폴더 삭제 (1개 도메인)

**`backend/src/main/java/com/rehearse/api/domain/feedback/` 전체 삭제:**
- `controller/FeedbackController.java`
- `service/FeedbackService.java`
- `entity/Feedback.java`
- `entity/FeedbackCategory.java`
- `entity/FeedbackSeverity.java`
- `entity/InterviewAnswer.java`
- `dto/GenerateFeedbackRequest.java`
- `dto/AnswerData.java`
- `dto/FeedbackResponse.java`
- `dto/FeedbackListResponse.java`
- `repository/FeedbackRepository.java`
- `repository/InterviewAnswerRepository.java`
- `exception/FeedbackErrorCode.java`

**테스트 삭제:**
- `backend/src/test/.../feedback/controller/FeedbackControllerTest.java`
- `backend/src/test/.../feedback/service/FeedbackServiceTest.java`

### Backend — 코드 수정 (3개 파일)

**`AiClient.java` (인터페이스):**
- `generateFeedback(String answersJson)` 메서드 시그니처 제거

**`ClaudeApiClient.java`:**
- `MAX_TOKENS_FEEDBACK` 상수 제거
- `generateFeedback()` 메서드 구현 제거

**`MockAiClient.java`:**
- `generateFeedback()` 메서드 Mock 구현 제거

**`ClaudePromptBuilder.java`:**
- `buildFeedbackSystemPrompt()` 메서드 제거
- `buildFeedbackUserPrompt()` 메서드 제거

**`ReportService.java`:**
- `FeedbackRepository` 의존성 확인 → 제거 또는 수정

### NPM 패키지 제거

- `@mediapipe/tasks-vision` (package.json에서 제거)

---

## 작업 순서 (의존성 역순)

### Task 1: [FE] 리뷰 페이지 + 컴포넌트 제거
- 삭제: `interview-review-page.tsx`, `components/review/*`, `review-store.ts`, `use-video-sync.ts`
- 수정: `app.tsx`에서 라우트 제거
- Implement: `frontend` — 파일 삭제 + 라우트 정리
- Review: `code-reviewer` — 미참조 import 잔존 확인

### Task 2: [FE] MediaPipe 전체 제거
- 삭제: `use-face-mesh.ts`, `use-pose-detection.ts`, `lib/mediapipe/*`
- 수정: 해당 hook을 사용하는 곳에서 import/호출 제거
- Implement: `frontend` — 파일 삭제 + 참조 정리
- Review: `code-reviewer` — 빌드 에러 확인

### Task 3: [FE] Audio Analyzer + UI 제거
- 삭제: `use-audio-analyzer.ts`, `audio-level-indicator.tsx`
- 수정: `interview-page.tsx`에서 audio 관련 코드 제거, `use-interview-session.ts`에서 audio 파라미터/콜백 제거
- Implement: `frontend` — 파일 삭제 + 참조 정리
- Review: `code-reviewer` — interview-page 정상 동작 확인

### Task 4: [FE] 타입 + Store 정리
- 수정: `types/interview.ts`에서 NonVerbalEvent, VoiceEvent, 관련 필드 제거
- 수정: `interview-store.ts`에서 비언어/음성 필드 및 메서드 제거
- 수정: `interview-complete-page.tsx`에서 nonVerbalSummary, voiceSummary 로직 제거
- Implement: `frontend` — 타입/스토어 정리
- Review: `code-reviewer` — 타입 에러 0 확인

### Task 5: [FE] NPM 패키지 제거
- `@mediapipe/tasks-vision` 제거
- `npm install` 후 빌드 확인
- Implement: `frontend` — package.json 수정

### Task 6: [BE] feedback 도메인 전체 제거
- 삭제: `domain/feedback/` 폴더 전체 + 테스트
- 수정: `AiClient`, `ClaudeApiClient`, `MockAiClient`에서 피드백 메서드 제거
- 수정: `ClaudePromptBuilder`에서 피드백 프롬프트 메서드 제거
- 수정: `ReportService`의 FeedbackRepository 의존성 확인
- Implement: `backend` — 도메인 삭제 + 의존성 정리
- Review: `architect-reviewer` — 레이어 정합성, 미참조 확인

### Task 7: [FE/BE] 최종 검증 [parallel 불가]
- FE: `npm run build` 성공 확인
- FE: `npm run lint` 에러 0 확인
- BE: `./gradlew build` 성공 확인
- BE: 기존 테스트 전체 통과 확인
- 면접 생성 → 질문 출제 → STT → 후속 질문 생성 흐름 정상 동작 확인
- Implement: `qa` — 빌드 + 테스트 + 수동 검증
- Review: `architect-reviewer` — 전체 의존성 그래프 정합성

---

## 제거 후 상태

### 면접 진행 흐름 (유지)
```
면접 생성 → 질문 출제 → 녹화 시작 + STT 시작
→ 사용자 답변 → STT 텍스트 → 후속 질문 생성
→ 반복 → 면접 종료 → 영상 Blob 저장 (IndexedDB)
```

### 제거된 흐름
```
❌ 실시간 시선/자세 분석 (MediaPipe)
❌ 실시간 음성 레벨/침묵 감지 (Web Audio)
❌ nonVerbalSummary/voiceSummary → Claude 피드백 생성
❌ 타임스탬프 피드백 리뷰 UI
❌ Backend feedback 도메인 전체
```

### 새 파이프라인 구현 준비 완료 상태
- 녹화 인프라 유지 (MediaRecorder)
- STT 유지 (Web Speech API)
- 스트림 획득 유지 (getUserMedia)
- S3 업로드, Lambda 분석, 새 피드백 뷰어를 위한 깨끗한 코드베이스
