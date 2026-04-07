# 피드백 패널 리디자인 — 프리뷰 HTML → React 포팅

- **Status**: Draft
- **Owner**: FE
- **Base branch**: `develop`
- **참고 프로토타입**: `frontend/feedback-preview.html` (untracked, 2026-04-07 작성)

---

## Why

### 문제
- PR #246(`[FE/BE] feat: 피드백 v2`)에서 v2 데이터 모델(`content.accuracyIssues`, `content.coaching`, `delivery.attitudeComment`)과 컴포넌트(`content-tab`, `delivery-tab`, `coaching-card`, `accuracy-issues`, `structured-comment`)를 도입했음.
- 그러나 사용자가 오늘 작성한 `frontend/feedback-preview.html` 디자인 시안을 보면, 현재 피드백 카드의 **정보 설계와 톤 앤 매너가 기대 수준과 다름**:
  - 섹션마다 "무엇을 어떻게 봤는지"를 설명해주는 **Hero copy**(`답변 내용을 분석했어요` + 보조 문구)가 없음
  - ✓/△/→ 아이콘만으로는 "잘한 점 / 아쉬운 점 / 이렇게 말하면 더 좋아요" 의미가 즉각 전달되지 않음
  - `AccuracyIssues`는 기술 오류를 경고 톤(주황)으로 강조하는데, 프리뷰는 "내가 한 말(취소선) → 정확한 내용" 의 **교정 형식**으로 부드럽게 제시
  - 비언어/음성 섹션에 **3컬럼 메트릭 그리드**(시선·자세·표정 / 속도·자신감·감정)가 없어 한눈에 스캔하기 어려움
  - 습관어가 fillerWords 파싱 결과로만 노출되고, **"5회 감지됐어요"** 같은 사용자 언어 카피가 없음
  - 탭 라벨이 `기술 분석` / `자세·말투` 로 기술 중심 — 프리뷰는 `내 답변은 어땠을까` / `어떤 인상을 줬을까` 로 사용자 관점

### 목표
1. `frontend/feedback-preview.html` 의 레이아웃·카피·톤을 실제 React 컴포넌트(`feedback-panel.tsx` 외 5개)에 반영한다.
2. **BE / Lambda / DB 변경 없이** 기존 데이터 계약(`ContentFeedback`, `DeliveryFeedback`, `NonverbalFeedback`, `VocalFeedback`)만으로 구현한다.
3. 기존 타입/프롭스 시그니처를 바꾸지 않아 다른 호출 지점(피드백 페이지, 테스트) 영향을 0으로 유지.

### 측정 기준
- 프리뷰 HTML 을 브라우저로 연 화면과 실제 `/interview/{publicId}/feedback` 화면의 **레이아웃·카피 일치**(섹션 순서, hero copy, 메트릭 그리드, 탭 라벨, 코칭 카피).
- FE CI(`npm run lint`, `npm run build`) 통과.
- 기존 피드백 데이터(현재 운영의 interviewId=94 등)로 렌더링해도 오류 없이 빈 섹션은 조용히 숨겨지고(`null` 가드 유지), 데이터가 있을 때만 섹션이 뜸.

### Trade-offs
- 프리뷰의 복사·섹션 분리를 그대로 따르면 컴포넌트당 JSX 가 길어져 한 파일이 ~200줄을 넘을 수 있음. 별도 소분 컴포넌트로 쪼개기보다는 **탭 하나 = 한 파일** 원칙을 지키는 편이 읽기 쉬움(트레이드오프: 재사용성 ↓, 가독성·수정 용이성 ↑).
- 기술 오류 표시 톤을 "경고(주황)" 에서 "교정(중립/회색 + 취소선)" 으로 낮춤 → 긴급성 신호가 약해짐. 반대로 **전체 카드의 톤 일관성**과 **자기 학습형 UX**에는 더 부합. 사용자 선호(토스 느낌 모노톤 + coral accent, minimal)에 더 부합하므로 채택.

---

## 스코프

### 변경 대상
- `frontend/src/components/feedback/feedback-panel.tsx` — 카드 껍데기·헤더·탭·푸터 리디자인
- `frontend/src/components/feedback/content-tab.tsx` — 3섹션(답변 분석 / 틀린 내용 / 다음엔 이렇게) hero copy + 교차 배경 구조
- `frontend/src/components/feedback/delivery-tab.tsx` — 3섹션(태도 인상 / 표정과 자세 / 목소리) + 3컬럼 메트릭 그리드
- `frontend/src/components/feedback/structured-comment.tsx` — 아이콘 대신 **섹션 라벨**("잘한 점 / 아쉬운 점 / 이렇게 말하면 더 좋아요") 기반 렌더링으로 전환. 라벨 세트를 prop 으로 주입해 섹션별로 교체 가능하게(`positiveLabel` / `negativeLabel` / `suggestionLabel`).
- `frontend/src/components/feedback/accuracy-issues.tsx` — "내가 한 말(취소선) → 정확한 내용" 교정 형식으로 변경
- `frontend/src/components/feedback/coaching-card.tsx` — 프리뷰 라벨 "답변 구조 / 설득력 높이기" 적용, 회색 박스 2단
- `frontend/src/components/feedback/level-badge.tsx` — **메트릭 셀** variant 추가 (기존 pill variant 도 유지, 두 가지 형태를 prop 으로 선택). 프리뷰의 3컬럼 카드 형태를 구현하기 위함.

### 변경 불가 / 하지 말 것
- `frontend/src/types/interview.ts` — 타입 시그니처 유지
- `backend/**` — DTO/엔티티/Flyway 변경 없음
- `lambda/**` — 프롬프트/핸들러 변경 없음
- `frontend/src/pages/**` 피드백 페이지 라우팅·데이터 패칭 로직 — 변경 없음

---

## 데이터 매핑 (프리뷰 섹션 ↔ 기존 필드)

프리뷰의 모든 텍스트 영역은 **이미 존재하는 필드**로 렌더링 가능. BE/Lambda 추가 작업 없음을 확인하는 표.

| 프리뷰 섹션 | 프리뷰 라벨 | 현재 FE 필드 | 현재 BE DTO 필드 | Lambda 소스 | 비고 |
|---|---|---|---|---|---|
| 카드 헤더 타임스탬프 | `0:00 — 1:32` | `feedback.startMs/endMs` | `TimestampFeedbackResponse.startMs/endMs` | n/a | 기존 그대로 |
| 카드 헤더 질문 유형 | `원본 답변 / 후속 질문` | `feedback.questionType` | 동 | n/a | MAIN/FOLLOWUP |
| 카드 헤더 질문 텍스트 | `Q. ...` | `findQuestion(fb).questionText` | QuestionWithAnswer | n/a | 기존 매핑 로직 재사용 |
| 카드 헤더 미분석 배지 | `미분석` | `!feedback.isAnalyzed` | 동 | n/a | |
| 탭 1 라벨 | `내 답변은 어땠을까` | - | - | - | 라벨만 변경 |
| 탭 2 라벨 | `어떤 인상을 줬을까` | - | - | - | 라벨만 변경, 비활성 조건은 기존 `isDeliveryAvailable` 유지 |
| **[기술분석] 답변 내용을 분석했어요** | 잘한 점 / 아쉬운 점 / 이렇게 말하면 더 좋아요 | `content.verbalComment` | `ContentFeedback.verbalComment` | `verbal_analyzer.py` (PR #246 프롬프트가 ✓/△/→ 구조로 생성 중) | `StructuredComment` 가 ✓/△/→ 프리픽스로 파싱하되, 아이콘 대신 **섹션 라벨**로 표시 |
| **[기술분석] 틀린 내용이 있었어요** | 내가 한 말(취소선) / 정확한 내용 | `content.accuracyIssues[]` | `ContentFeedback.accuracyIssues[]` | 동 | `AccuracyIssue.claim → "내가 한 말"`, `.correction → "정확한 내용"` |
| **[기술분석] 다음엔 이렇게 해보세요** | 답변 구조 / 설득력 높이기 | `content.coaching` | `ContentFeedback.coaching` | 동 | 필드명 `structure → "답변 구조"`, `improvement → "설득력 높이기"` (라벨만 변경) |
| **[자세말투] 면접관에게 이런 인상을 줬어요** | 좋은 인상 / 신경 쓰면 좋을 부분 / 이렇게 바꿔보세요 | `delivery.attitudeComment` | `DeliveryFeedback.attitudeComment` | 동 | `StructuredComment` 라벨 세트 교체로 구현 |
| **[자세말투] 표정과 자세를 살펴봤어요 — 3컬럼 메트릭** | 시선 / 자세 / 표정 | `delivery.nonverbal.eyeContactLevel` / `postureLevel` / `expressionLabel` | `NonverbalFeedback.*` | `vision_analyzer.py` | `FeedbackLevel → "좋음/보통/개선 필요"` 이미 LevelBadge 에 매핑 |
| **[자세말투] 표정과 자세 — 코멘트** | 잘한 점 / 아쉬운 점 / 이렇게 해보세요 | `delivery.nonverbal.nonverbalComment` | 동 | 동 | `StructuredComment` 라벨 세트 교체 |
| **[자세말투] 목소리를 분석했어요 — 3컬럼 메트릭** | 속도 / 자신감 / 감정 | `delivery.vocal.speechPace` / `toneConfidenceLevel` / `emotionLabel` | `VocalFeedback.*` | `verbal_analyzer.py` | 기존 필드 사용. `speechPace`/`emotionLabel`은 string 그대로 표시 |
| **[자세말투] 습관어 태그 + "5회 감지됐어요"** | `습관어가 N회 감지됐어요` + 태그 | `delivery.vocal.fillerWords`(JSON string) + `fillerWordCount` | `VocalFeedback.*` | 동 | 기존 `parseFillerWords` 로직 재사용. `N==0`이면 블록 숨김 |
| **[자세말투] 목소리 — 코멘트** | 잘한 점 / 아쉬운 점 / 이렇게 해보세요 | `delivery.vocal.vocalComment` | 동 | 동 | |
| 카드 푸터 답변 텍스트 | `답변 텍스트` 버튼 | `feedback.transcript` | 동 | 동 | 토글. `highlightFillers` 재사용 |
| 카드 푸터 모범답변 | `모범답변 비교` 버튼 | `question.modelAnswer` | 동 | n/a | 토글 |
| 미분석 카드 | `AI가 분석하고 있어요` | `!feedback.isAnalyzed` | 동 | n/a | content/delivery null 이어도 렌더 가능 |

**결론: 기존 데이터 계약 100% 호환. 프롬프트·스키마 변경 불필요.**

### 단 하나의 주의점 — `StructuredComment` 파싱 입력 포맷
- 현재 `verbalComment`, `attitudeComment`, `nonverbalComment`, `vocalComment` 는 모두 **`✓` / `△` / `→` 프리픽스로 시작하는 3줄 텍스트** 를 가정하고 Lambda/BE 프롬프트가 그 포맷으로 생성 중 (PR #246).
- 본 PR 의 `StructuredComment` 변경은 **파싱 규칙은 유지**하고, 각 라인을 아이콘 대신 섹션 라벨(prop 으로 주입)로 렌더링하는 **표현 계층 교체**에 한함. 따라서 기존 데이터 입력에 대해 그대로 동작한다. 만약 Lambda 가 어떤 이유로 ✓/△/→ 프리픽스 없는 문자열을 주면 기존 fallback 경로(가운데 `text-text-secondary` 평문 렌더링)가 그대로 유효.

---

## Task 분할

> 각 Task 는 단일 파일 기준으로 독립 실행 가능하도록 쪼갬. Task 1 완료 후 Task 2~6 은 병렬 실행 가능. Task 7 은 전체 통합 QA.

### Task 1: 프리뷰 HTML 을 스펙으로 확정
- Implement: `frontend` — 프리뷰 HTML 을 체크인할지 결정. 옵션:
  - (a) 그대로 `frontend/feedback-preview.html` 유지 (untracked)
  - (b) `docs/prototypes/feedback-panel-v3-preview.html` 로 이동 후 커밋
- Review: `planner` — 본 스펙이 프리뷰의 모든 섹션을 커버하는지 최종 검수
- **권장**: (b) 이동 + 커밋. 다른 시안 `docs/prototypes/feedback-v2-preview.html` 과 나란히 두어 이력 추적.
- [parallel] 다른 Task 와 병렬 가능

### Task 2: `StructuredComment` 라벨 세트 prop 화
- Implement: `frontend` — `structured-comment.tsx`
  - Props 확장: `{ comment, positiveLabel?, negativeLabel?, suggestionLabel? }`
  - 기본값: `잘한 점` / `아쉬운 점` / `이렇게 말하면 더 좋아요`
  - 렌더: 프리픽스로 구분한 라인 앞에 라벨(`text-[13px] font-bold text-gray-500 mb-1`) + 본문(`text-[15px] leading-[1.7] text-gray-700`)
  - ✓/△/→ 아이콘은 화면에서 제거 (파싱용으로만 사용)
- Review: `code-reviewer` — 기존 호출지(3곳) 가 prop 없이도 컴파일/렌더 가능한지, 기본값 회귀 없는지

### Task 3: `accuracy-issues.tsx` 교정 형식 전환
- Implement: `frontend`
  - 배경 톤 `orange → neutral`(bg-gray-50 카드, 내부 white 박스)
  - `claim` → `"내가 한 말"` 섹션, `text-gray-400 line-through decoration-gray-300`
  - `correction` → `"정확한 내용"` 섹션, `text-gray-700`
  - 여러 건이면 여러 박스로 나열 (프리뷰는 1건이지만 배열 대응 필요)
  - 0건이면 기존처럼 `null` return
- Review: `code-reviewer`

### Task 4: `coaching-card.tsx` 라벨/레이아웃 변경
- Implement: `frontend`
  - 외곽 라벨 `💡 코칭` 제거 (hero copy 는 `content-tab.tsx` 에서 담당)
  - `structure` 박스: 라벨 `답변 구조`, 내부 본문
  - `improvement` 박스: 라벨 `설득력 높이기`, 내부 본문
  - 각 박스 `rounded-xl bg-gray-50 p-4`
  - 둘 다 null 이면 기존처럼 `null` return
- Review: `code-reviewer`

### Task 5: `level-badge.tsx` — 메트릭 셀 variant 추가
- Implement: `frontend`
  - Props 확장: `{ label, level, variant?: 'pill' | 'metric' }` — 기본값 `'pill'`
  - `metric` variant: `bg-white rounded-xl p-3 text-center` 형태 (프리뷰 표정/자세/시선 카드 스타일). 내부는 라벨(`text-[12px] text-gray-400`) + 값(`text-[15px] font-bold text-gray-900`)
  - `pill` variant: 기존 동작 유지
  - `level === null` 이면 `variant==='metric'` 일 때도 카드 자리는 유지하고 값은 `—` 표시 (그리드 정렬 유지를 위해). pill 은 기존처럼 숨김.
- Review: `code-reviewer`

### Task 6: `delivery-tab.tsx` 리디자인
- Implement: `frontend`
  - 섹션 순서: 태도 인상 → 비언어(표정·자세) → 음성
  - 각 섹션 상단 hero copy(`<p className="text-[15px] font-bold text-gray-900 mb-1">면접관에게 이런 인상을 줬어요</p>` + 보조 `<p className="text-[13px] text-gray-400 mb-4">`)
  - 배경 교차: 태도 인상(white) → 비언어(bg-gray-50) → 음성(white)
  - 비언어: 3컬럼 `grid grid-cols-3 gap-3 mb-4` 메트릭. 각 셀은 `LevelBadge variant='metric'` (시선/자세) 및 `expressionLabel`(단순 카드)
  - 음성: 3컬럼 메트릭(속도/자신감/감정) + 습관어 박스(`rounded-xl bg-gray-50 p-4`, "습관어가 N회 감지됐어요" + 태그 리스트)
  - 코멘트는 `StructuredComment` 에 라벨 세트:
    - 태도 인상: `좋은 인상 / 신경 쓰면 좋을 부분 / 이렇게 바꿔보세요`
    - 비언어: `잘한 점 / 아쉬운 점 / 이렇게 해보세요`
    - 음성: `잘한 점 / 아쉬운 점 / 이렇게 해보세요`
  - 모든 서브 블록은 `null` 가드 유지. 3섹션 모두 null 이면 기존 "자세·말투 분석 정보가 없습니다" placeholder 유지
- Review: `code-reviewer` — 기존 `DeliveryFeedback` 데이터 호환 여부, null safety
- Review: `designer` — 프리뷰와 실제 렌더 결과 픽셀 정합성 (스크린샷 비교)
- [parallel] Task 2~5 와 병렬 가능

### Task 7: `content-tab.tsx` 리디자인
- Implement: `frontend`
  - 섹션 순서: 답변 내용 분석 → 틀린 내용 → 다음엔 이렇게 해보세요
  - 각 섹션 hero copy (`답변 내용을 분석했어요` / `틀린 내용이 있었어요` / `다음엔 이렇게 해보세요`) + 보조 문구
  - 배경 교차: 답변 분석(white) → 틀린 내용(bg-gray-50) → 코칭(white)
  - 답변 분석: `<StructuredComment comment={content.verbalComment} />` (기본 라벨 사용)
  - 틀린 내용: `<AccuracyIssues issues={content.accuracyIssues} />` — 0건이면 섹션 전체 숨김
  - 코칭: `<CoachingCard coaching={content.coaching} />` — null 이면 섹션 전체 숨김
  - 3섹션 모두 비면 기존 fallback 유지
- Review: `code-reviewer`
- [parallel] Task 6 과 병렬 가능

### Task 8: `feedback-panel.tsx` (FeedbackCard) 껍데기 리디자인
- Implement: `frontend`
  - 카드: `rounded-2xl bg-white overflow-hidden` + 커스텀 섀도 `style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.06)' }}`
  - 헤더: 타임스탬프(`text-[13px] font-bold tabular-nums`) + 유형 라벨(`text-[13px] text-gray-400`) + 미분석 배지(오른쪽)
  - 질문 제목: `text-[17px] font-bold text-gray-900 leading-snug`
  - 탭 라벨 교체: `기술 분석` → `내 답변은 어땠을까`, `자세·말투` → `어떤 인상을 줬을까`
  - 탭 비활성 상태(`isDeliveryAvailable === false`) 는 프리뷰의 `cursor-not-allowed` 스타일로 표현
  - 탭 하단 경계선 `border-b border-gray-100`
  - 카드 푸터 버튼 2개(`답변 텍스트` / `모범답변 비교`) — 프리뷰의 border-top 구분선 + `text-[13px] font-bold text-gray-400` 스타일
  - 기존 상태 훅(`activeTab`, `showTranscript`, `showModelAnswer`, `cardRef.scrollIntoView` 로직) 모두 그대로 유지
  - `onSeek` / `isActive` / active 카드 강조(border+bg) 로직 유지
- Review: `code-reviewer` — 상태/이벤트/접근성, 탭 비활성 조건
- Review: `designer` — 프리뷰 vs 실제 렌더 픽셀 비교

### Task 9: 수동 QA + PR 준비
- Implement: `qa` — 로컬 `npm run dev` 로 피드백 페이지를 열고 프리뷰 HTML 과 나란히 비교
  - 카드 1(v2 데이터 있음): 모든 섹션 렌더 확인
  - 카드 2(`delivery === null`): 탭 비활성 확인
  - 카드 3(미분석): placeholder 렌더 확인
  - 후속질문 카드(`questionType=FOLLOWUP`): 배지 확인
- Review: `code-reviewer` — 전체 PR 최종 스윕

---

## Critical files (수정 대상)
- `frontend/src/components/feedback/feedback-panel.tsx`
- `frontend/src/components/feedback/content-tab.tsx`
- `frontend/src/components/feedback/delivery-tab.tsx`
- `frontend/src/components/feedback/structured-comment.tsx`
- `frontend/src/components/feedback/accuracy-issues.tsx`
- `frontend/src/components/feedback/coaching-card.tsx`
- `frontend/src/components/feedback/level-badge.tsx`
- (선택) `docs/prototypes/feedback-panel-v3-preview.html` — `frontend/feedback-preview.html` 이동

## 건드리지 않음
- `frontend/src/types/interview.ts`
- `frontend/src/pages/**` (피드백 페이지 라우팅·데이터 패칭)
- `backend/**`
- `lambda/**`

---

## Verification
1. **정적 검증**
   - `cd frontend && npm run lint` → 통과
   - `cd frontend && npm run build` → 통과 (Vite 빌드 에러 0)
   - TypeScript `tsc --noEmit` (`npm run build` 에 포함)
2. **로컬 수동 QA (Task 9)**
   - `cd frontend && npm run dev`
   - 브라우저에서 `frontend/feedback-preview.html` 을 `file://` 로 열어 프리뷰 확인
   - 실제 앱의 `/interview/c3d47e4d-d337-42ca-96e5-57e2a46e860f/feedback` 페이지를 열어 프리뷰와 나란히 비교
   - 체크리스트:
     - [ ] 탭 라벨이 `내 답변은 어땠을까 / 어떤 인상을 줬을까`
     - [ ] 기술분석 탭: 3섹션 hero copy + 교차 배경
     - [ ] 틀린 내용: "내가 한 말"(취소선) → "정확한 내용"
     - [ ] 코칭: "답변 구조" / "설득력 높이기" 라벨
     - [ ] 자세말투 탭: 태도 인상 → 비언어(메트릭 3컬럼) → 음성(메트릭 3컬럼 + 습관어 박스)
     - [ ] 습관어 0회면 블록 숨김, N회면 "N회 감지됐어요" + 태그
     - [ ] 미분석 카드는 "AI가 분석하고 있어요"
     - [ ] 후속질문 카드 배지
     - [ ] 답변 텍스트 / 모범답변 토글 동작
3. **회귀**
   - 기존 피드백 페이지의 다른 영역(비디오 플레이어, 타임라인) 영향 없음
   - `delivery === null` / `content === null` 케이스에서 런타임 에러 없음 (기존 interviewId=94 데이터로 확인)
4. **배포 후**
   - PR 머지 → `deploy-dev.yml` 자동 실행 → `frontend-build` + `Deploy` 성공 확인
   - S3 `rehearse-frontend-dev` 의 `assets/index-*.js` 파일 타임스탬프 갱신 확인
   - CloudFront invalidate 완료 후 `dev.rehearse.co.kr` 에서 강제 새로고침 → 새 UI 확인

---

## Branch / PR
- Branch: `feat/feedback-panel-redesign` (base: `develop`)
- PR title: `[FE] feat: 피드백 패널 리디자인 — 프리뷰 HTML 마크업 반영`
- 커밋을 **Task 단위로 분할**(8~9 커밋) 하여 리뷰 편의성 확보. 각 커밋 메시지에 대응 Task 번호 명시.
