# [FE] refactor: 프론트엔드 대규모 코드 리팩토링 — 기획서

> `frontend-coding-guide.md` 기준 코드 품질 통일 및 유지보수성 확보
> 2026.03.13

---

## 1. Why — 리팩토링 배경

### 문제 정의

MVP 기능 구현을 우선하며 빠르게 성장한 프론트엔드 코드베이스에 `docs/frontend-coding-guide.md`에 정의된 규칙과의 괴리가 누적되었다.

| 문제 | 현황 | 가이드 기준 | 괴리 |
|------|------|-----------|------|
| 대형 컴포넌트 | `interview-setup-page.tsx` **581줄** | Stateful 250줄 초과 시 분리 | **2.3배 초과** |
| God Hook | `use-interview-session.ts` **373줄**, 6가지 관심사 혼합 | 하나의 훅 = 하나의 관심사 | **6배 위반** |
| 코드 중복 | `formatTime` 함수 **3곳** 동일 복사 | DRY / Rule of Three | **즉시 추상화 대상** |
| Import 불일치 | **~25개 파일**이 상대경로(`../`) 사용 | `@/` 절대경로 통일 | **전체의 ~60%** |
| 매직 넘버 | **5개+ 파일**에 하드코딩된 숫자 | 의미 있는 상수 추출 | 가독성 저해 |
| 미사용 디렉토리 | `components/mediapipe/`, `lib/audio/` | — | 정리 필요 |

### 목표

1. `frontend-coding-guide.md`의 모든 규칙을 **100% 준수**하는 코드베이스 달성
2. **기능 변경 없이** 코드 구조만 개선 (순수 리팩토링)
3. 각 Phase가 **독립적으로 머지 가능**한 단위로 분리
4. 향후 기능 추가 및 코드 리뷰 비용 절감

### 근거 (Evidence)

- 코딩 가이드 문서가 이미 존재하나, 기존 코드의 ~40%가 미준수 상태
- `interview-setup-page.tsx`는 5가지 책임(상태관리, 파일업로드, Step UI 4개, 폼검증, API호출)을 혼합하여 수정 시 전체를 이해해야 함
- `use-interview-session.ts`는 TTS/STT/녹화/이벤트기록/후속질문/타이머를 모두 포함하여 버그 수정 시 사이드이펙트 위험이 높음

### 트레이드오프

| 선택 | 포기 | 이유 |
|------|------|------|
| 6개 Phase로 분리 | 한 번에 전체 리팩토링 | 리뷰 가능한 PR 크기 유지, 리스크 분산 |
| 기능 변경 없음 | 성능 최적화/기능 개선 | 리팩토링 범위를 명확히 제한하여 검증 용이 |
| Phase 4 수동 E2E 필수 | 자동 테스트만으로 검증 | TTS/STT 타이밍 의존성이 복잡하여 자동 테스트로 커버 불가 |

---

## 2. 현황 분석 — 정량 데이터

### 2-1. 대형 파일 (가이드 기준 초과)

| 파일 | 줄 수 | 가이드 기준 | 책임 수 | 분리 방향 |
|------|------|-----------|--------|----------|
| `pages/interview-setup-page.tsx` | **581** | 250 | 5 | 커스텀 훅 + Step별 컴포넌트 |
| `hooks/use-interview-session.ts` | **373** | 단일 관심사 | 6 | greeting/answer-flow 훅 분리 |
| `stores/interview-store.ts` | 239 | 250 | 2 | 현재 기준 내 (모니터링) |
| `types/interview.ts` | 235 | — | 혼합 | 타입/상수 분리 |
| `hooks/use-speech-recognition.ts` | 227 | 단일 관심사 | 3 | 현재 기준 내 (모니터링) |
| `components/interview/device-test-section.tsx` | 222 | 40(Stateless) | 3 | 기기별 Row 분리 |
| `pages/interview-page.tsx` | 199 | 250 | 2 | 현재 기준 내 (모니터링) |
| `pages/home-page.tsx` | 190 | 250 | 3 | 섹션별 분리 |

### 2-2. 코드 중복 — `formatTime` 함수 (3곳 동일)

```typescript
// 동일한 코드가 3개 파일에 존재
const formatTime = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}
```

| 파일 | 줄 번호 |
|------|--------|
| `components/review/feedback-panel.tsx` | 21-24 |
| `components/review/feedback-timeline.tsx` | 9-12 |
| `components/review/timeline-marker.tsx` | 16-19 |
| `components/interview/interview-timer.tsx` | 11-22 (유사, 시:분:초 포맷) |
| `pages/interview-setup-page.tsx` | 170-174 (`formatFileSize` — 별도 유틸) |

### 2-3. Import 경로 불일치

**절대경로(`@/`) 사용 파일** (~15개): interview-setup-page, home-page, app, use-interviews, use-feedback, use-report, device-test-section 등

**상대경로(`../`) 사용 파일** (~25개):

| 분류 | 파일들 |
|------|--------|
| pages (4) | interview-page, interview-complete-page, interview-review-page, interview-report-page |
| hooks (7) | use-interview-session, use-speech-recognition, use-audio-analyzer, use-face-mesh, use-pose-detection, use-video-sync, use-interview-event-recorder |
| stores (2) | interview-store, review-store |
| components (7) | feedback-panel, feedback-timeline, timeline-marker, video-player, interview-controls, question-display, transcript-display |
| lib (2) | mediapipe/event-detector, __tests__/api-client.test |

### 2-4. 매직 넘버

| 파일 | 줄 | 값 | 의미 | 상수화 |
|------|-----|-----|------|--------|
| `interview-timer.tsx` | 53, 61 | `120_000` | 2분 경고 | `TIME_WARNING_THRESHOLD_MS` |
| `audio-waveform.tsx` | — | `0.15, 0.12, 0.8, 0.1` | 애니메이션 파라미터 | 상수 블록 |
| `device-test-section.tsx` | 122 | `0.4, 8` | 마이크 레벨 스케일 | `MIC_LEVEL_SCALE_FACTOR` |
| `use-speech-recognition.ts` | 98 | `1000` | 지수 백오프 기본값 | `BACKOFF_BASE_MS` |
| `interview-setup-page.tsx` | 66 | `=== 4` | 마지막 스텝 | `totalSteps` 변수 활용 |

### 2-5. 에러 처리 비일관성

| 파일 | 패턴 | 문제 |
|------|------|------|
| `lib/api-client.ts` | 구조화된 `ApiError` 클래스 | 우수 (기준) |
| `use-speech-recognition.ts` | `catch {}` 빈 블록 | 조용한 실패, 로깅 없음 |
| `use-tts.ts` | `utterance.onerror` | 에러 타입 미확인 |
| `interview-setup-page.tsx` | `ApiError` instanceof 체크 | 부분적 (양호) |

---

## 3. Phase별 작업 계획

### Phase 1: 유틸리티 추출 + 매직 넘버 상수화 + 정리

> **난이도**: 낮 | **영향도**: 중 | **변경 파일**: ~15개 | **PR**: `[FE] refactor: 유틸리티 함수 통합 및 매직 넘버 상수화`

#### Task 1-1: `formatTime` 유틸리티 통합 `[parallel]`
- **Implement**: `frontend` — 유틸리티 함수 추출
- **Review**: `code-reviewer` — DRY 준수 확인

**작업 내용:**
1. `src/lib/format-utils.ts` 생성
   - `formatTimeMinSec(seconds: number): string` — `m:ss` 포맷
   - `formatTimeFull(ms: number): string` — `hh:mm:ss` 또는 `mm:ss` 포맷
   - `formatFileSize(bytes: number): string` — 파일 크기 포맷
2. 5개 파일의 로컬 함수 삭제 → import 교체

#### Task 1-2: 매직 넘버 상수 추출 `[parallel]`
- **Implement**: `frontend` — 상수화
- **Review**: `code-reviewer` — 네이밍 적절성

**작업 내용:** §2-4 표의 5개 항목 상수화

#### Task 1-3: 미사용 디렉토리 정리 `[parallel]`
- **Implement**: `frontend` — 삭제
- `components/mediapipe/.gitkeep`, `lib/audio/.gitkeep` 삭제

---

### Phase 2: Import 경로 통일 (`../` → `@/`)

> **난이도**: 낮 | **영향도**: 높 | **변경 파일**: ~25개 | **PR**: `[FE] refactor: import 경로 절대경로 통일`
> **Phase 1과 병렬 가능**

#### Task 2-1: 상대경로 → 절대경로 일괄 변환
- **Implement**: `frontend` — import 변환
- **Review**: `code-reviewer` — 빌드 확인

**작업 내용:** §2-3 표의 ~25개 파일 변환

**변환 규칙:**
```
../stores/interview-store  →  @/stores/interview-store
../../stores/review-store   →  @/stores/review-store
../types/interview          →  @/types/interview
../hooks/use-interviews     →  @/hooks/use-interviews
../lib/video-storage        →  @/lib/video-storage
../components/interview/*   →  @/components/interview/*
```

> **주의**: 대량 파일 변경이므로 다른 기능 브랜치와 충돌 없는 타이밍에 머지

---

### Phase 3: `interview-setup-page.tsx` 분리 (581줄 → ~70줄)

> **난이도**: 중 | **영향도**: 높 | **변경 파일**: ~8개 | **PR**: `[FE] refactor: 면접 Setup 위저드 컴포넌트 분리`
> **Phase 4와 병렬 가능**

#### Task 3-1: `useInterviewSetup` 커스텀 훅 추출
- **Implement**: `frontend` — 훅 추출
- **Review**: `architect-reviewer` — SRP, 관심사 분리

**작업 내용:**
1. `src/hooks/use-interview-setup.ts` 생성
2. 이동할 상태 (9개): `currentStep`, `position`, `level`, `durationMinutes`, `interviewTypes`, `csSubTopics`, `resumeFile`, `serverError`, `dragOver`
3. 이동할 핸들러 (9개): `handleNext`, `handlePrev`, `handlePositionSelect`, `handleTypeToggle`, `handleCsSubTopicToggle`, `handleFileSelect`, `handleFileRemove`, `handleDrop`, `handleSubmit`
4. 이동할 파생값: `canNext`, `isSubmitStep`, `isLoading`
5. 상수: `POSITIONS`, `LEVELS`, `CS_SUB_TOPICS`, `DURATION_PRESETS`, `MAX_FILE_SIZE`, `Step` 타입

#### Task 3-2: Step 컴포넌트 분리
- **Implement**: `frontend` — UI 분리
- **Review**: `designer` — UI 일관성

**신규 파일:**

| 파일 | 내용 | 예상 줄 수 |
|------|------|-----------|
| `components/setup/step-position.tsx` | Step 1: 직무 선택 그리드 | ~35 |
| `components/setup/step-level.tsx` | Step 2: 레벨 선택 리스트 | ~50 |
| `components/setup/step-duration.tsx` | Step 3: 시간 프리셋 카드 | ~35 |
| `components/setup/step-interview-type.tsx` | Step 4: 유형 + CS주제 | ~80 |
| `components/setup/resume-upload.tsx` | 이력서 드래그&드롭 업로드 | ~60 |
| `components/setup/setup-progress-bar.tsx` | 상단 프로그레스 바 | ~30 |
| `components/setup/setup-navigation.tsx` | 하단 이전/다음/시작 버튼 | ~30 |

**결과:** `interview-setup-page.tsx`는 훅 호출 + 컴포넌트 조합만 담당 (~70줄)

---

### Phase 4: `use-interview-session.ts` 관심사 분리 (373줄 → ~150줄)

> **난이도**: 높 | **영향도**: 높 | **변경 파일**: ~4개 | **PR**: `[FE] refactor: 면접 세션 훅 관심사 분리`
> **Phase 3과 병렬 가능**

#### Task 4-1: Greeting 흐름 분리
- **Implement**: `frontend` — 훅 추출
- **Review**: `architect-reviewer` — 라이프사이클 무결성

**작업 내용:**
1. `src/hooks/use-interview-greeting.ts` 생성
2. 이동할 로직:
   - `greetingPhaseRef` 관리
   - greeting phase 진입 시 인사 TTS 시작
   - greeting → ready 전환 (자기소개 완료 후 첫 질문 TTS)
3. 관련 useEffect 이동 (phase === 'greeting' 관련 2개)

#### Task 4-2: 답변 처리 + 전환 로직 분리
- **Implement**: `frontend` — 훅 추출
- **Review**: `architect-reviewer` — 상태 전환 정합성

**작업 내용:**
1. `src/hooks/use-answer-flow.ts` 생성
2. 이동할 로직:
   - `doStartAnswer` — 답변 시작 (녹화 재개, STT 시작)
   - `handleStopAnswer` — 답변 완료 (후속질문 요청, 전환 TTS)
   - `processAnswer` — 후속질문 mutation 호출
   - `pendingTtsActionRef` — TTS 완료 후 nextQuestion/finish 예약
3. 이동할 상수: `TRANSITION_PHRASES`, `CLOSING_PHRASES`, `pickRandom`

#### Task 4-3: eslint-disable 최소화
- **Implement**: `frontend` — ref 패턴 적용
- **Review**: `code-reviewer` — deps 정확성

**결과:** `use-interview-session.ts`는 오케스트레이션 훅으로 축소 (~150줄). 각 하위 훅이 단일 관심사 담당.

> **리스크**: TTS/STT/녹화의 타이밍 의존성이 복잡. 분리 후 **반드시 수동 E2E 테스트** 필수 (면접 전체 플로우: 인사 → 질문 → 답변 → 전환 → 종료)

---

### Phase 5: 타입 파일 분리 + Setup 코드 품질 개선

> **난이도**: 중 | **영향도**: 중 | **변경 파일**: ~12개 | **PR**: `[FE] refactor: 타입/상수 분리 + Setup 코드 품질 개선`
> Phase 3-4 완료 후

#### Task 5-1: `types/interview.ts` 분리
- **Implement**: `frontend` — 파일 분할
- **Review**: `code-reviewer` — import 경로 확인

**작업 내용:**
1. `types/interview.ts` — 순수 타입/인터페이스만 유지
   - `Position`, `Level`, `InterviewType`, `CsSubTopic`, `InterviewStatus`
   - `InterviewSession`, `Question`, `TranscriptSegment`, `NonVerbalEvent`, `VoiceEvent`
   - `TimestampFeedback`, `FollowUpResponse`, `QuestionAnswer` 등
2. `constants/interview-labels.ts` 생성 — UI 라벨 매핑 상수 이동
   - `POSITION_LABELS`, `LEVEL_LABELS`, `INTERVIEW_TYPE_LABELS`
   - `CS_SUB_TOPIC_LABELS`, `POSITION_INTERVIEW_TYPES`

#### Task 5-2: Setup 역방향 의존성 해소 (Phase 3 코드리뷰 반영)
- **Implement**: `frontend` — 상수/타입 추출
- **Review**: `architect-reviewer` — 의존성 방향 확인

**배경:** Phase 3 리뷰에서 Step 컴포넌트들이 `use-interview-setup.ts` 훅에서 상수/타입을 import하는 역방향 의존성 발견.

**작업 내용:**
1. `constants/setup.ts` 생성 — Setup 전용 상수 이동
   - `POSITIONS`, `LEVELS`, `CS_SUB_TOPICS`, `DURATION_PRESETS`, `MAX_FILE_SIZE`
   - `Step` 타입
2. Step 컴포넌트 + 훅의 import를 `constants/setup.ts`로 변경
   - `step-position.tsx`, `step-level.tsx`, `step-duration.tsx`, `step-interview-type.tsx`
   - `setup-navigation.tsx`, `setup-progress-bar.tsx`
   - `use-interview-setup.ts`

#### Task 5-3: Setup 훅/컴포넌트 품질 개선 (Phase 3 코드리뷰 반영)
- **Implement**: `frontend` — 코드 품질 개선
- **Review**: `code-reviewer` — 메모이제이션, 타입 안전성

**작업 내용:**
1. **핸들러 메모이제이션 일관성**: `handleNext`, `handlePrev` 등 모든 핸들러에 `useCallback` 적용 또는 전체 제거하여 일관성 확보
2. **`as Step` 타입 단언 제거**: 범위 검증 후 안전한 타입 변환으로 교체
3. **setter 직접 노출 제거**: `setDragOver`, `setLevel`, `setDurationMinutes`를 핸들러로 래핑
4. **인라인 화살표 함수 제거**: 페이지에서 `onDragOver={() => ...}` 패턴을 훅 내부 핸들러로 이동
5. **체크마크 SVG 공통화**: `CheckIcon` 컴포넌트 추출 (step-level, step-interview-type, setup-progress-bar)
6. **접근성 속성 추가**: Step 선택 버튼에 `aria-pressed`/`aria-selected`, 체크박스 버튼에 `role="checkbox"` + `aria-checked`

---

### Phase 6: 나머지 컴포넌트 정리

> **난이도**: 낮 | **영향도**: 낮 | **변경 파일**: ~8개 | **PR**: `[FE] refactor: 컴포넌트 SRP 적용 및 에러 처리 일관성`

#### Task 6-1: `device-test-section.tsx` 분리 `[parallel]`
- **Implement**: `frontend` — 컴포넌트 분리
- **Review**: `code-reviewer` — SRP

**작업 내용:**
1. 기기별 Row 컴포넌트 분리:
   - `components/interview/camera-test-row.tsx`
   - `components/interview/mic-test-row.tsx`
   - `components/interview/speaker-test-row.tsx`
2. 공통 요소(`StatusIcon`, `StatusMessage`, `statusBorder`)는 `device-test-section.tsx`에 유지

#### Task 6-2: `home-page.tsx` 섹션 분리 `[parallel]`
- **Implement**: `frontend` — UI 분리
- **Review**: `designer` — UI 일관성

**작업 내용:**
1. `components/home/hero-section.tsx` — Hero 영역
2. `components/home/journey-section.tsx` — 진행 과정 소개 (3단계 목업)
3. `components/home/cta-section.tsx` — CTA 영역
4. `home-page.tsx`는 섹션 조합만 담당 (~40줄)

#### Task 6-3: 에러 처리 일관성 개선 `[parallel]`
- **Implement**: `frontend` — 에러 핸들링 통일
- **Review**: `code-reviewer` — 보안, 에러 처리

**작업 내용:**
1. `use-speech-recognition.ts`: 빈 `catch {}` 블록에 에러 상태 업데이트 또는 의도적 무시 주석 추가
2. `use-tts.ts`: `utterance.onerror`에서 에러 타입별 분기 처리
3. 모든 의도적 무시 catch에 `// 의도적 무시: cleanup 단계에서 stop 실패는 무해` 형태 주석

---

## 4. 실행 순서 및 병렬화

```
Phase 1 + Phase 2    [parallel]  ── PR #1, #2  (낮은 난이도, 즉시 착수)
         ↓
Phase 3 + Phase 4    [parallel]  ── PR #3, #4  (핵심 리팩토링)
         ↓
Phase 5                          ── PR #5       (import 충돌 방지 위해 순차)
         ↓
Phase 6                          ── PR #6       (독립적, 언제든 가능)
```

---

## 5. 검증 방법

### 각 PR 공통
1. `npm run build` — 빌드 성공
2. `npm run lint` — 린트 통과
3. 기능 변경 없음 확인 (동일 동작 보장)

### Phase별 추가 검증

| Phase | 추가 검증 |
|-------|----------|
| 1 | 시간 표시 포맷이 기존과 동일한지 UI 확인 |
| 2 | 모든 페이지 라우팅 정상 동작 |
| 3 | Setup 위저드 전체 플로우 (Step 1→4→제출) 수동 테스트 |
| 4 | **면접 전체 플로우 E2E 수동 테스트** (인사→질문→답변→전환→종료) |
| 5 | import 경로 변경 후 빌드 확인 |
| 6 | 디바이스 테스트, 홈페이지 정상 표시 |

---

## 6. 리스크

| 리스크 | 심각도 | 대응 |
|--------|--------|------|
| Phase 2 대량 파일 변경 → 머지 충돌 | 중 | 다른 기능 브랜치 없는 타이밍에 머지 |
| Phase 4 TTS/STT 타이밍 깨짐 | **높** | 분리 후 수동 E2E 필수, 문제 시 롤백 |
| Phase 3 Step 분리 시 상태 전달 누락 | 중 | Props 인터페이스 명시적 정의, 빌드 타임 체크 |
| 전체: 기능 변경 혼입 | 중 | PR 리뷰 시 "기능 변경 없음" 체크리스트 필수 |

---

## 7. 참조 문서

- `docs/frontend-coding-guide.md` — 코딩 가이드 (모든 리팩토링의 기준)
- `docs/product/PLAN.md` — 제품 기획서
- `CLAUDE.md` — 프로젝트 규칙 (커밋/PR 컨벤션, 금지사항)

---

> Status: `Completed` — Phase 1~6 전체 완료
