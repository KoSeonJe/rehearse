# 피드백 페이지 질문 목록 (feedback-question-list) — 요구사항

> 상태: Draft
> 작성일: 2026-04-07

## Why

현재 피드백 페이지(`interview-feedback-page.tsx`)는 **좌측 60% 영상 + 타임라인 막대 / 우측 40% FeedbackCard 세로 리스트** 구조다. 한 질문세트가 메인 질문 1개 + 후속질문 N개를 가질 수 있는데도 사용자는:

1. **질문 전체 구조를 한눈에 못 본다** — 후속이 몇 개 붙었는지, 어떤 흐름이었는지 알려면 우측 카드를 끝까지 스크롤해야 함
2. **메인 vs 후속 구분이 약하다** — `FeedbackCard`가 `원본 답변`/`후속 질문` 라벨을 작은 회색 텍스트로 표시할 뿐, 트리/계층 구조가 시각화되지 않음
3. **점프 네비게이션이 없다** — 5번째 후속질문으로 가려면 카드 4개를 스크롤하거나 타임라인 막대에서 정확히 그 구간을 클릭해야 함

### Decision Framework

- **Why?** 메인+후속질문 구조가 데이터에 이미 있는데(질문세트당 1+N) UI가 평면 리스트라 정보 손실 발생. 사용자 보고는 없지만 prototype HTML(`feedback-v3-preview.html`) 검토 중 사용자가 직접 "질문 목록 좋다"고 언급 → UX 개선 기회.
- **Goal** 영상 아래(또는 우측 패널 상단)에 **메인 질문 1개 + 후속질문 N개를 계층적으로 보여주는 질문 목록 컴포넌트**를 추가. 각 항목 클릭 시 해당 답변 시작점으로 영상 시킹 + 우측 카드 활성화.
- **Evidence**
  - 데이터 준비 완료: `QuestionWithAnswer { questionType: 'MAIN' \| 'FOLLOWUP', startMs, endMs, questionText }` (`interview.ts:113`)
  - API 준비 완료: `/api/v1/interviews/{id}/question-sets/{qsId}/questions-with-answers` (`QuestionSetController:70`)
  - 훅 준비 완료: `useQuestionsWithAnswers` (`use-question-sets.ts:105`)
  - 활성 답변 동기화 준비 완료: `useFeedbackSync` → `activeFeedbackId`
- **Trade-offs**
  - **포기**: 좌측 60% / 우측 40% 단순 2열 레이아웃의 깔끔함
  - **얻음**: 한눈에 보이는 면접 흐름, 빠른 점프, 메인/후속 시각적 위계
  - **고려한 대안 1 (기각)**: 우측 FeedbackCard 위에 작은 헤더로 추가 — sticky 영역이 좁아 5개 이상이면 잘림
  - **고려한 대안 2 (기각)**: 좌측 영상과 우측 패널 사이에 새 컬럼 — 화면이 좁아져 영상 시인성 저하
  - **선택**: **좌측 영상+타임라인 아래에 질문 목록 카드를 추가**. 영상이 sticky지만 질문 목록은 함께 sticky 될 필요 없음 (스크롤 시 우측 카드와 같이 흐름)

## 목표

1. 질문세트 단위로 메인 질문 1개 + 후속질문 N개를 계층적으로 시각화
2. 각 항목 클릭 → 영상 시킹 + 우측 FeedbackCard 활성화 (`activeFeedbackId` 동기화)
3. 현재 재생 중인 답변 항목을 시각적으로 강조 (우측 FeedbackCard와 동일한 활성 표시)
4. 빈 상태 (질문 목록이 1개뿐)에서도 깨지지 않음
5. 모바일 (lg 미만) 레이아웃에서도 정상 표시

## 아키텍처 / 설계

### 데이터 흐름

```
QuestionSetSection
  ├─ useQuestionsWithAnswers → QuestionWithAnswer[]  (이미 있음)
  ├─ useFeedbackSync → activeFeedbackId, seekTo      (이미 있음)
  │
  ├─ 좌측 column
  │    ├─ VideoPlayer
  │    ├─ TimelineBar
  │    └─ ★ QuestionList (신규)         ← questions, activeFeedbackId, feedbacks, onSeek
  │
  └─ 우측 column
       └─ FeedbackPanel (변경 없음)
```

### 컴포넌트 책임

`QuestionList`:
- props: `{ questions: QuestionWithAnswer[], feedbacks: TimestampFeedback[], activeFeedbackId: number | null, onSeek: (ms: number) => void }`
- 메인 질문(`questionType === 'MAIN'`)과 후속질문(`questionType === 'FOLLOWUP'`)을 시간 순으로 정렬
- 메인 질문은 좌측 정렬 + 큰 폰트, 후속질문은 들여쓰기(좌측 인디케이터 라인)
- 각 항목에 시간 범위 (`mm:ss ~ mm:ss`) 표시
- 활성 항목 (`activeFeedbackId`와 매칭) 강조 — coral accent
- 항목 클릭 시 `onSeek(question.startMs)` 호출

### 활성 매칭 로직

```ts
// QuestionList 내부
const findFeedbackForQuestion = (q: QuestionWithAnswer): TimestampFeedback | undefined => {
  if (q.startMs === null || q.endMs === null) return undefined
  return feedbacks.find(
    (fb) => fb.startMs >= q.startMs! && fb.startMs < q.endMs!,
  )
}

const isActive = (q: QuestionWithAnswer): boolean => {
  const fb = findFeedbackForQuestion(q)
  return fb !== undefined && fb.id === activeFeedbackId
}
```

### 시각 디자인 (토스풍 모노톤 + coral accent)

```
┌──────────────────────────────────┐
│ 질문 흐름                            │  ← 헤더 (text-[13px] font-bold gray-400)
├──────────────────────────────────┤
│ ●  Q. JVM의 GC 동작 원리…       │  ← 메인 (text-[15px] font-bold gray-900)
│    00:12 ~ 02:48                  │     활성 시: bg-[#FFF1EE], 좌측 ● coral
│                                   │
│  ┃ ↳ 그럼 G1 GC와 ZGC는?          │  ← 후속 1 (text-[14px] gray-700, 좌측 ┃)
│  ┃   02:55 ~ 04:10                │
│                                   │
│  ┃ ↳ 실무에서 GC 튜닝 사례는?    │  ← 후속 2
│  ┃   04:15 ~ 06:20                │
└──────────────────────────────────┘
```

- 메인: 원형 dot (●) + 큰 텍스트
- 후속: 좌측 vertical line (┃) + 들여쓰기 + ↳ 화살표
- 활성: 배경 `#FFF1EE`, dot/line이 coral `#FF6B5B`
- 비활성: gray-50 hover, dot 회색
- 시간 텍스트: tabular-nums

## Scope

- **In**:
  - 신규 컴포넌트 `frontend/src/components/feedback/question-list.tsx`
  - `interview-feedback-page.tsx`의 좌측 column에 `<QuestionList>` 추가
  - 활성 동기화 (클릭 → seek, 재생 위치 → 활성 표시)
  - 메인/후속 시각적 위계
- **Out**:
  - BE/Lambda 변경 (데이터/API 모두 준비됨)
  - `FeedbackPanel`/`FeedbackCard` 내부 변경 (하위호환 유지)
  - `TimelineBar` 변경
  - 질문 목록에서 직접 펼쳐서 피드백 미리보기 (스코프 폭증)
  - 키보드 단축키 (j/k 네비게이션 등)
  - 후속질문 펼침/접힘 토글 (메인이 1개라 가치 낮음)

## 제약조건 / 환경

- **dev 단일 환경**, prod 운영 아님
- 한 질문세트의 후속질문 최대치는 ~5개 (Sprint 0 기준)
- 1 질문세트 = 1 영상 = 1 답변 시퀀스. `questions[]`는 시간 순 정렬 가정 가능 (BE `from()`에서 orderIndex 정렬)
- `questions`가 빈 배열인 경우 (분석 미완료) → 컴포넌트 자체 미렌더
- 메인 질문이 아예 없는 케이스는 데이터 무결성 상 발생 안 함, 발생 시 안전 폴백 (전체 평면 렌더)

## 성공 기준

- [ ] 질문세트에 메인 1 + 후속 3개가 있을 때 4개 항목이 계층 구조로 렌더
- [ ] 영상 재생 중 후속질문 2번 구간 진입 시 해당 항목이 자동으로 coral 강조
- [ ] 질문 목록의 후속 3번 항목 클릭 → 영상이 해당 시점으로 시킹 + 우측 FeedbackCard 활성화
- [ ] 메인만 있는 질문세트(후속 0개)에서도 깨지지 않음
- [ ] 모바일 (lg 미만) 1열 레이아웃에서도 영상 → 질문목록 → 우측패널 순으로 정상 적층
- [ ] `npm run build` 통과 (타입/린트)
