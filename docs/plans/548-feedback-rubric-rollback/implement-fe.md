# Implement (Frontend) — 피드백 루브릭 롤백

> **작성자**: frontend agent (create-implement-plan)
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★
> **강결합**: BE 선행 권장 (main 기지 contract 라 mock 병렬 가능, 단 위험 3 때문에 BE 머지 후 통합 권장)
> **범위**: PR1 (Phase 1~4) = 답변별 2탭 복원 / PR2 (Phase 5) = 세션 모달 재노출. PR1 머지 후 PR2 착수.

---

## Phase 0: 착수 전 확인 + Mock

- [ ] **develop 동기화** — `git checkout develop && git pull`. `git-manager` 위임.
- [ ] **API Contract 확인** — `tech-spec.md#api-contract` main `TimestampFeedbackResponse` 형태 (content/delivery). 조회 `GET /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/feedback`.
- [ ] **Mock** — main 응답 형태 fixture (content{verbalComment/accuracyIssues/coaching} + delivery{nonverbal/vocal/attitudeComment} + overallComment).
- [ ] **컨벤션 Read** — `frontend/.claude/rules/conventions.md`, `architecture.md`, `testing.md`. 디자인 작업 시 `frontend/DESIGN.md`.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | PR | 의존 |
|-------|------|------|----|------|
| 1 | feedback-viewer 타입 REVERT | `frontend` | PR1 | Phase 0 |
| 2 | 복원 컴포넌트 6종 (main RESTORE) | `frontend` | PR1 | Phase 1 |
| 3 | feedback-panel 2탭 + content-tab REVERT + rubric DELETE | `frontend` | PR1 | Phase 2 |
| 4 | 세션 모달/코치FAB 임시 숨김 | `frontend` | PR1 | Phase 3 |
| 5 | 세션 모달 재노출 + 점수행 제거 + 서술형 렌더 | `frontend` | PR2 | PR1 + BE PR2 머지 |

> 단일 implement-fe.md (tasks/ 분리 안 함). Phase 1~4 = 단일 frontend 세션 순차. Phase 1·2 는 mock 으로 BE 머지 전 병렬 착수 가능.

---

## Phase 1: feedback-viewer 타입 REVERT

- **구현**: `frontend` — 피드백 뷰어 타입을 main content/delivery 형태로 복원.

### 변경 파일
- `frontend/src/types/interview.ts` — **feedback-viewer 관련 타입만** REVERT (TimestampFeedback content/delivery 구조). dimension/score/observation/status 타입 제거
- `frontend/src/types/service-feedback.ts` — 세션 피드백 타입 (PR2 영향, PR1 은 건드리지 않거나 최소)

### 핵심 로직
- main `TimestampFeedbackResponse` 형태 타입. content{verbalComment/accuracyIssues[]/coaching} + delivery{nonverbal{eyeContactLevel/postureLevel/expressionLabel}/vocal{fillerWords/fillerWordCount/speechPace/toneConfidenceLevel/emotionLabel}/attitudeComment} + overallComment + bestAnswer + questionType.
- **건드리지 않음**: `QuestionType` enum (7종, 인터뷰 도메인 공용), FollowUp 타입 — 루브릭 무관.

### 의존
- 선행: Phase 0.

### Verification
- `npm run build` (tsc) 타입 통과.

### 커밋 메시지
```
refactor(FE): 피드백 뷰어 타입을 content/delivery 형태로 복원
```

---

## Phase 2: 복원 컴포넌트 6종 (main RESTORE)

- **구현**: `frontend` — develop 에서 삭제된 main 컴포넌트 복원. `bestAnswer` 필드 사용하도록 적응.

### 변경 파일 (main 에서 RESTORE)
- `frontend/src/components/feedback/delivery-tab.tsx` — Delivery 탭 (nonverbal + vocal + attitudeComment)
- `frontend/src/components/feedback/coaching-card.tsx` — 코칭 카드 (structure/improvement)
- `frontend/src/components/feedback/accuracy-issues.tsx` — 정확도 이슈 (claim/correction)
- `frontend/src/components/feedback/level-badge.tsx` — 레벨 배지
- `frontend/src/components/feedback/structured-comment.tsx` — 구조화 코멘트 (positive/negative/suggestion)
- `frontend/src/components/feedback/format-feedback-level.ts` — 레벨 포맷 유틸

### 핵심 로직
- main 원본 복원 + `modelAnswer` → `bestAnswer` 필드명 치환 (TO-2, grep 확인).
- 현행 디자인 토큰 (brand teal) 충돌 시 최소 적응 (레이아웃 개편 비스코프).

### 의존
- 선행: Phase 1 (타입).

### Verification
- `npm run build` + `npm run lint`. 컴포넌트 vitest 동작.

### 커밋 메시지
```
feat(FE): Delivery 탭 + 코칭/정확도/레벨배지/구조화코멘트 컴포넌트 복원
```

---

## Phase 3: feedback-panel 2탭 + content-tab REVERT + rubric DELETE

- **구현**: `frontend` — 단일 섹션 → Content/Delivery 2탭 복원, 루브릭 차원 카드 제거.

### 변경 파일
- `frontend/src/components/feedback/feedback-panel.tsx` — 2탭 (Content/Delivery) 구조 REVERT
- `frontend/src/components/feedback/content-tab.tsx` — main 형태 REVERT (StructuredComment + AccuracyIssues + CoachingCard), 비언어/차원 섹션 제거
- `frontend/src/components/feedback/rubric-dimension-card.tsx` — **삭제**
- `frontend/src/lib/feedback/dimension-label.ts` — 루브릭 전용이면 삭제 (사용처 grep 확인 — 비루브릭 사용 시 보존)
- `frontend/src/components/feedback/__tests__/{content-tab,feedback-panel,rubric-dimension-card}.test.tsx` — 2탭 재작성 / 루브릭 테스트 삭제
- `frontend/src/lib/feedback/__tests__/dimension-label.test.ts` — 삭제 (dimension-label 삭제 시)

### 핵심 로직
- ContentTab: StructuredComment(verbalComment) + AccuracyIssues + CoachingCard.
- DeliveryTab: nonverbal(eye/posture/expression LevelBadge) + vocal(filler/pace/tone/emotion) + attitudeComment.
- **건드리지 않음**: `use-feedback-sync.ts`, `lib/feedback/outline.ts`, `question-list.tsx`, `timeline-bar.tsx` (QuestionType-enum 결합, 루브릭 무관).

### 의존
- 선행: Phase 2 (복원 컴포넌트).

### Verification
- `npm run test` — Content/Delivery 2탭 렌더, 차원 점수 카드(1~5점) 부재. `npm run build && npm run lint`.

### 커밋 메시지
```
refactor(FE): 피드백 패널 Content/Delivery 2탭 복원 + 루브릭 차원 카드 제거
```

---

## Phase 4: 세션 모달/코치FAB 임시 숨김

- **구현**: `frontend` — PR1 동안 세션 피드백 진입점 숨김 (BE 생성 비활성 정합).

### 변경 파일
- `frontend/src/components/feedback/session-feedback-modal.tsx` — 진입점 임시 숨김 (렌더 가드)
- `frontend/src/components/feedback/coach-note-fab.tsx` — FAB 임시 숨김
- (호출처 피드백 페이지에서 조건부 비표시)

### 핵심 로직
- BE Phase 5 세션 생성 비활성 → 세션 피드백 데이터 없음 → 모달/FAB 숨김.
- 컴포넌트 삭제 아님 (PR2 Phase 5 에서 재노출). 진입점 가드만.

### 의존
- 선행: Phase 3.

### Verification
- `npm run test` — 세션 모달/FAB 미노출 확인.

### 커밋 메시지
```
refactor(FE): 세션 피드백 모달/코치노트 진입점 임시 숨김 (PR2 재노출 전)
```

---

## Phase 5: 세션 모달 재노출 + 서술형 렌더 (PR2)

- **구현**: `frontend` — 세션 피드백 모달 재노출, 차원 점수 행 제거, 서술형 렌더.
- **착수 조건**: PR1 머지 + BE PR2(세션 입력 재설계) 머지 후.

### 변경 파일
- `frontend/src/components/feedback/session-feedback-modal.tsx` — Phase 4 숨김 해제, 차원 점수 행 제거, 강점/약점/계획 서술형 렌더
- `frontend/src/components/feedback/coach-note-fab.tsx` — 재노출
- `frontend/src/types/service-feedback.ts` — 차원점수 미렌더 (BE 직렬화 유지하되 FE 미사용)

### 핵심 로직
- tech-spec §Architecture FE Post-PR2. 강점/약점/계획 서술형 텍스트 (루브릭 차원명·점수 표기 없음).
- 레이아웃 개편 비스코프 — 점수 행 제거 + 서술형 표시까지만.

### 의존
- 선행: PR1 머지 + BE Phase 7 머지.

### Verification
- `npm run test` — 세션 모달 재노출, 점수 행 부재, 강점/약점/계획 서술형 렌더.

### 커밋 메시지
```
feat(FE): 세션 피드백 모달 재노출 + 서술형 렌더 (차원 점수 행 제거)
```

---

## BE 통합

- BE PR1 머지 알림 수신 후 mock 제거 → 실제 endpoint 연결.
- `GET /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/feedback` 실제 응답 정상 처리.

### Verification
- [ ] mock 흔적 0건 (fixture 제거)
- [ ] 실제 BE 응답 처리 (200/404)

## 통합 Verification

- [ ] tech-spec.md §Verification PR1 FE (Content/Delivery 2탭, 차원 카드 부재, 세션 모달/FAB 숨김) 통과
- [ ] tech-spec.md §Verification PR2 FE (세션 모달 재노출, 점수 행 부재, 서술형) 통과
- [ ] `npm run build && npm run lint && npm run test`

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE 동시 작업 시 `code-reviewer-backend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec §Pre/Post)
