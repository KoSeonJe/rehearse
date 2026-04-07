# Plan 01: QuestionList 컴포넌트 + 페이지 통합

> 상태: Draft
> 작성일: 2026-04-07

## Why

`requirements.md`의 결정 실행. 메인+후속질문 계층 목록을 좌측 영상/타임라인 아래에 추가해 면접 흐름을 한눈에 보이게 한다. 데이터/API/훅이 모두 준비돼 있어 순수 FE 작업.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/feedback/question-list.tsx` | **신규** — QuestionList 컴포넌트 |
| `frontend/src/pages/interview-feedback-page.tsx` | 좌측 column에 `<QuestionList>` 추가, `questions`/`feedbacks`/`activeFeedbackId`/`seekTo` 전달 |

`feedback-panel.tsx` / `timeline-bar.tsx` / `interview.ts` / 백엔드 / Lambda 모두 **변경 없음**.

## 상세

### 1. `question-list.tsx` (신규)

```tsx
import type { QuestionWithAnswer, TimestampFeedback } from '@/types/interview'

interface QuestionListProps {
  questions: QuestionWithAnswer[]
  feedbacks: TimestampFeedback[]
  activeFeedbackId: number | null
  onSeek: (ms: number) => void
}

const formatTime = (ms: number): string => {
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  return `${m}:${(s % 60).toString().padStart(2, '0')}`
}

export const QuestionList = ({
  questions,
  feedbacks,
  activeFeedbackId,
  onSeek,
}: QuestionListProps) => {
  // 시간이 없는 질문(미답변/미분석)은 제외
  const playable = questions.filter(
    (q): q is QuestionWithAnswer & { startMs: number; endMs: number } =>
      q.startMs !== null && q.endMs !== null,
  )
  if (playable.length === 0) return null

  // startMs 기준 정렬 (BE가 이미 정렬했지만 안전 차원)
  const sorted = [...playable].sort((a, b) => a.startMs - b.startMs)

  const findFeedback = (q: { startMs: number; endMs: number }) =>
    feedbacks.find((fb) => fb.startMs >= q.startMs && fb.startMs < q.endMs)

  return (
    <div className="rounded-2xl bg-white border border-gray-100 p-5">
      <p className="text-[13px] font-bold text-gray-400 mb-4">질문 흐름</p>
      <ol className="space-y-1">
        {sorted.map((q, idx) => {
          const isFollowup = q.questionType === 'FOLLOWUP'
          const fb = findFeedback(q)
          const isActive = fb !== undefined && fb.id === activeFeedbackId

          return (
            <li key={`${q.questionId}-${idx}`}>
              <button
                type="button"
                onClick={() => onSeek(q.startMs)}
                className={`w-full text-left rounded-xl px-3 py-3 transition-colors ${
                  isActive ? 'bg-[#FFF1EE]' : 'hover:bg-gray-50'
                } ${isFollowup ? 'pl-8 relative' : ''}`}
              >
                {isFollowup && (
                  <span
                    className={`absolute left-4 top-0 bottom-0 w-px ${
                      isActive ? 'bg-[#FF6B5B]' : 'bg-gray-200'
                    }`}
                    aria-hidden
                  />
                )}
                <div className="flex items-start gap-3">
                  {!isFollowup && (
                    <span
                      className={`mt-1.5 inline-block h-2 w-2 rounded-full flex-shrink-0 ${
                        isActive ? 'bg-[#FF6B5B]' : 'bg-gray-300'
                      }`}
                      aria-hidden
                    />
                  )}
                  {isFollowup && (
                    <span
                      className={`text-[13px] flex-shrink-0 ${
                        isActive ? 'text-[#FF6B5B]' : 'text-gray-400'
                      }`}
                      aria-hidden
                    >
                      ↳
                    </span>
                  )}
                  <div className="flex-1 min-w-0">
                    <p
                      className={`leading-snug ${
                        isFollowup
                          ? 'text-[14px] text-gray-700'
                          : 'text-[15px] font-bold text-gray-900'
                      } ${isActive ? 'text-gray-900' : ''}`}
                    >
                      {q.questionText}
                    </p>
                    <p className="mt-1 text-[12px] text-gray-400 tabular-nums">
                      {formatTime(q.startMs)} ~ {formatTime(q.endMs)}
                    </p>
                  </div>
                </div>
              </button>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

export default QuestionList
```

### 2. `interview-feedback-page.tsx` 수정

`QuestionSetSection` 좌측 column 끝에 `<QuestionList>` 추가:

```tsx
// import 추가
import { QuestionList } from '@/components/feedback/question-list'

// 좌측 column 내부 (line ~253 영역)
<div className="lg:w-[60%] space-y-4 lg:sticky lg:top-20 lg:self-start">
  <VideoPlayer ... />
  <TimelineBar ... />
  <QuestionList
    questions={questions}
    feedbacks={feedbacks}
    activeFeedbackId={activeFeedbackId}
    onSeek={seekTo}
  />
</div>
```

> ⚠️ `lg:sticky lg:top-20`이 좌측 컬럼 전체에 걸려 있다. 영상+타임라인+질문목록이 함께 sticky가 되면 길어질 때 화면을 넘어갈 수 있음. **검증 필요**: 질문 목록이 5개 넘을 때 sticky 영역이 viewport 높이를 초과하면 sticky 해제 또는 max-height + 내부 스크롤 처리.
>
> 1차 구현은 그대로 두고, 검증 단계에서 viewport overflow 발생하면 `<QuestionList>`의 컨테이너에 `max-h-[calc(100vh-...)] overflow-y-auto` 추가.

## 담당 에이전트

- Implement: `frontend` — 컴포넌트 + 페이지 통합
- Review: `code-reviewer` — 타입 안전성, null 처리, 활성 매칭 로직 정확성
- Review: `designer` — 토스풍 모노톤+coral 일관성, 메인/후속 시각적 위계, 모바일 적층

## 검증

- `npm --prefix frontend run build` 통과 (tsc + vite)
- `npm --prefix frontend run lint` 통과
- 로컬 dev (`npm run dev`)에서 mock 데이터로 확인:
  - [ ] 메인 1개 + 후속 3개 질문세트에서 4항목 계층 렌더
  - [ ] 항목 클릭 → 영상 시킹 (`onSeek` 호출)
  - [ ] 영상 재생 위치가 후속 2번 구간 진입 → 해당 항목 활성 강조
  - [ ] 메인만 있는 질문세트(후속 0개)에서도 메인 1줄 정상 렌더
  - [ ] 빈 questions(분석 미완료) → 컴포넌트 미렌더 (null)
  - [ ] 모바일 (375px) 레이아웃에서 영상 → 질문목록 → 패널 적층
  - [ ] sticky 영역 viewport overflow 검증 (후속 5개 케이스)
- `progress.md` 상태 업데이트 (Task 1 → Completed)

## 잠재 리스크

| 리스크 | 대응 |
|---|---|
| `lg:sticky` 좌측 컬럼이 viewport보다 길어짐 | 검증 단계에서 발견 시 `max-h` + 내부 스크롤. 또는 질문 목록만 sticky 해제 (`lg:relative`로 별도 분리) |
| `findFeedback` 매칭 실패 (`startMs` 경계 케이스) | feedback이 없어도 클릭은 동작 (seek만 함). 활성 표시만 안 됨. UX 손상 작음 |
| 질문 텍스트가 매우 길 때 줄바꿈 폭주 | `line-clamp-2` 적용 검토 (1차에선 자연 줄바꿈) |
| `questions[]`가 시간 순 정렬 안 돼 있음 | 컴포넌트 내부에서 `[...].sort((a,b) => a.startMs - b.startMs)` 방어 (이미 포함) |
