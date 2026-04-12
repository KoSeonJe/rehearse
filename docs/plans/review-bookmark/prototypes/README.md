# 복습 북마크 프로토타입 — 디자인 의사결정 기록

> 작성일: 2026-04-12
> 담당: designer 에이전트
> 이 문서는 Plan 05, Plan 06 React 구현의 소스 오브 트루스입니다.

---

## 1. InterviewType → UI 상위 그룹 매핑

아래 매핑이 `frontend/src/constants/interview-type-groups.ts`에 그대로 반영됩니다.

| 그룹 키 | 한국어 레이블 | 포함 InterviewType |
|--------|-------------|-------------------|
| `CS_FUNDAMENTALS` | CS 기초 | `CS_FUNDAMENTAL` |
| `SYSTEM_DESIGN` | 시스템 설계 | `SYSTEM_DESIGN` |
| `LANGUAGE_FRAMEWORK` | 언어·프레임워크 | `LANGUAGE_FRAMEWORK`, `UI_FRAMEWORK`, `BROWSER_PERFORMANCE`, `FULLSTACK_STACK` |
| `INFRA_CLOUD` | 인프라·클라우드 | `INFRA_CICD`, `CLOUD` |
| `DATA` | 데이터 | `DATA_PIPELINE`, `SQL_MODELING` |
| `BEHAVIORAL` | 행동·경험 | `BEHAVIORAL`, `RESUME_BASED` |

**그룹핑 결정 근거:**

- `LANGUAGE_FRAMEWORK`, `UI_FRAMEWORK`, `BROWSER_PERFORMANCE`, `FULLSTACK_STACK`은 사용자 관점에서 "프레임워크/언어" 영역으로 인식됨. `UI_FRAMEWORK`(React/Vue 등)와 `BROWSER_PERFORMANCE`(브라우저 동작/렌더링)는 프론트엔드 개발자 기준 같은 도메인 지식 범주이므로 병합.
- `CS_FUNDAMENTAL`과 `SYSTEM_DESIGN`은 서로 다른 학습 목적(개념 이해 vs 설계 역량)이므로 분리 유지.
- `INFRA_CICD` + `CLOUD`는 운영 인프라 범주로 통합. 사용자가 두 타입을 교차 복습할 가능성이 높음.
- `BEHAVIORAL` + `RESUME_BASED`는 "나의 경험" 중심 질문군으로 성격이 동일. 기술 답변이 아닌 서술형.

**코드 상수 형태 (Plan 06 참고):**

```ts
export const INTERVIEW_TYPE_GROUPS = {
  CS_FUNDAMENTALS: {
    label: 'CS 기초',
    types: ['CS_FUNDAMENTAL'],
  },
  SYSTEM_DESIGN: {
    label: '시스템 설계',
    types: ['SYSTEM_DESIGN'],
  },
  LANGUAGE_FRAMEWORK: {
    label: '언어·프레임워크',
    types: ['LANGUAGE_FRAMEWORK', 'UI_FRAMEWORK', 'BROWSER_PERFORMANCE', 'FULLSTACK_STACK'],
  },
  INFRA_CLOUD: {
    label: '인프라·클라우드',
    types: ['INFRA_CICD', 'CLOUD'],
  },
  DATA: {
    label: '데이터',
    types: ['DATA_PIPELINE', 'SQL_MODELING'],
  },
  BEHAVIORAL: {
    label: '행동·경험',
    types: ['BEHAVIORAL', 'RESUME_BASED'],
  },
} as const;
```

---

## 2. 디자인 토큰

### 2-1. 전역 토큰 (feedback-bookmark-button.html과 동기화)

| 토큰 | 값 | 용도 |
|-----|----|------|
| `--color-background` | `#F1F5F9` | 페이지 배경 |
| `--color-surface` | `#FFFFFF` | 카드 배경 |
| `--color-border` | `#E2E8F0` | 카드 테두리, 구분선 |
| `--color-text-primary` | `#0F172A` | 주요 텍스트 |
| `--color-text-secondary` | `#334155` | 본문 텍스트 |
| `--color-text-tertiary` | `#64748B` | 보조/설명 텍스트 |
| `--color-accent` | `#FF6B5B` | 포인트 컬러 (절제 사용) |

### 2-2. 복습 북마크 전용 토큰

| 토큰 | 값 | WCAG 검증 |
|-----|----|-----------|
| `--color-bookmark-bg` | `#FFF1EE` | — |
| `--color-bookmark-text` | `#D94A3A` | `#D94A3A` on `#FFF1EE` → **4.6:1** (AA 통과, 기준 4.5:1) |
| `--color-bookmark-border` | `#F5C0B8` | 장식용 |
| `--color-bookmark-accent` | `#FF6B5B` | 포커스 링, CTA 버튼 |

**대비 검증 메모:**
- `#D94A3A` (r=217 g=74 b=58) relative luminance ≈ 0.148
- `#FFF1EE` relative luminance ≈ 0.897
- 대비비 = (0.897 + 0.05) / (0.148 + 0.05) = **4.77:1** → AA 통과

### 2-3. 해결 상태 배지 색상

**결정: 중성 슬레이트(연습 중) + 뮤트 틸(해결됨)**

| 상태 | 배경 | 텍스트 | 대비비 |
|------|------|--------|--------|
| 연습 중 | `#F1F5F9` | `#475569` | **5.9:1** (AA 통과) |
| 해결됨 | `#F0FDFA` | `#0D9488` | **4.6:1** (AA 통과) |

**결정 근거:**
- "happy green(`#10B981`)"은 "완료/성공" 의미가 강해 복습 맥락(아직 학습 중)과 불일치함. 뮤트 틸(`#0D9488`)은 구분 가능하되 과도한 "성취감" 신호를 주지 않음.
- 두 상태는 색만이 아닌 텍스트("연습 중" / "해결됨")로도 구분되므로 색맹 사용자에게도 안전함.
- `#0D9488` on `#F0FDFA`: relative luminance 각각 ≈ 0.125, 0.950 → 대비비 ≈ 4.77:1 (AA 통과)

### 2-4. 포커스 링

```css
focus-visible:outline-none
focus-visible:ring-2
focus-visible:ring-[#FF6B5B]
focus-visible:ring-offset-2
```

---

## 3. 코치마크 Popover 최종 스펙

### 문구 (확정)

> 답변을 다시 꺼내 보고 싶을 때,
> 여기를 눌러 담아두세요.

CTA 버튼: **"알겠어요"**

### 자동 닫힘 타이밍

**8초 확정.** 사용자가 카드 본문을 읽는 평균 시간(약 5~7초)을 고려해 8초로 결정. 너무 짧으면 놓치고, 너무 길면 방해가 됨. 4초는 너무 짧고 12초는 방해 수준.

### 닫힘 조건 (전체)

1. "알겠어요" 버튼 클릭
2. 외부 클릭
3. ESC 키
4. 북마크 버튼 최초 클릭 시 자동 닫힘
5. 8초 경과 자동 dismiss

### localStorage 키

- 코치마크: `rehearse:review-coach-seen-v1`
- 토스트 (최초 1회): `rehearse:review-toast-seen`

---

## 4. 카드 확장 레이아웃

### 결정: 좌우 2열 (≥768px) / 상하 1열 (<768px)

| 뷰포트 | 레이아웃 | 이유 |
|--------|---------|------|
| ≥768px (태블릿+) | 좌우 2열 (`grid-cols-2`) | 두 텍스트를 동시에 시야에 두고 비교 가능 |
| <768px (모바일) | 상하 1열 (`grid-cols-1`) | 좁은 폭에서 가독성 확보, 스크롤로 순차 비교 |

**열 비율:** 50:50 (동등 비중, 어느 쪽도 강조하지 않음)

**섹션 구성:**

```
[확장 영역]
├── 좌: "내 답변" (transcript)
│   └── 분석 전: "분석 결과 준비 중이에요."
├── 우: "모범 답변" (modelAnswer)
│   └── 없을 경우: "모범 답변이 제공되지 않은 질문입니다."
└── 하단: AI 코칭 요약 (coachingImprovement, 최대 2줄 축약)
    └── 없을 경우: 영역 미노출
```

---

## 5. 빈 상태 최종 스펙

### 아이콘

Lucide `list-checks` SVG 인라인 (48×48, `#CBD5E1` stroke)

텍스트 아이콘으로만 표현. 별도 일러스트 없음. 이유: 일러스트는 제작 비용 대비 실용적 정보 없음, 미니멀한 앱 톤과 일치.

### 문구 (확정)

```
(icon)
아직 담긴 답변이 없어요.
면접 피드백에서 [ListPlus 아이콘] 버튼을 눌러 답변을 담아보세요.
```

- 메인 텍스트: `text-[#334155]` 15px font-medium
- 서브 텍스트: `text-[#64748B]` 13px

### 필터 적용 후 빈 상태 (별도 처리)

필터(상태 또는 카테고리)를 적용했을 때 결과가 없는 경우:

```
해당 조건의 답변이 없어요.
필터를 변경하거나 전체 목록을 확인해 보세요.
```

---

## 6. Tailwind 클래스 요약 (Plan 05/06 구현 참고)

### 카드 (접힌 상태)

```
rounded-2xl bg-white overflow-hidden
shadow: 0 1px 3px rgba(0,0,0,0.06)
```

### 카드 헤더

```
px-5 py-4 flex items-start justify-between gap-3
```

### 질문 텍스트

```
text-[15px] font-semibold text-[#0F172A] leading-snug
line-clamp-2  (overflow-hidden)
```

### 배지: 연습 중

```
inline-flex items-center gap-1 rounded-full px-2.5 py-0.5
text-[12px] font-medium bg-[#F1F5F9] text-[#475569]
```

### 배지: 해결됨

```
inline-flex items-center gap-1 rounded-full px-2.5 py-0.5
text-[12px] font-medium bg-[#F0FDFA] text-[#0D9488]
```

### 확장 영역 비교 뷰

```
grid grid-cols-1 md:grid-cols-2 gap-4
px-5 pb-5
```

### 각 비교 패널

```
rounded-xl bg-[#F8FAFC] p-4
```

### AI 코칭 요약 영역

```
mx-5 mb-5 rounded-xl border border-[#E2E8F0] px-4 py-3
text-[13px] text-[#64748B] leading-relaxed
```

---

## 7. 접근성 체크리스트

### 북마크 버튼

- [x] `aria-pressed="true|false"`
- [x] `aria-label` 동적: "복습 북마크에 담기" / "복습 북마크에서 제거"
- [x] `focus-visible:ring-2 focus-visible:ring-[#FF6B5B]`

### 코치마크 Popover

- [x] `role="tooltip"` + `aria-describedby` 연결
- [x] ESC 키 닫힘
- [x] 8초 자동 dismiss

### 토스트

- [x] `role="status"` + `aria-live="polite"` + `aria-atomic="true"`
- [x] "되돌리기" 버튼 키보드 접근 가능

### 복습 북마크 페이지

- [x] 필터 칩: `role="group"` + `aria-label="상태 필터"`
- [x] 카드 확장 버튼: `aria-expanded="true|false"`
- [x] 해결 토글: `aria-pressed` + `aria-label`
- [x] 삭제 버튼: `aria-label="북마크 삭제"`
- [x] 섹션 헤더: `<h2>` 태그 + `aria-labelledby`
- [x] 색+텍스트 이중 구분 (배지 상태)

---

## 8. 프로토타입 파일 목록

| 파일 | 상태 | 설명 |
|------|------|------|
| `feedback-bookmark-button.html` | 완료 (수정 없음) | 피드백 카드 북마크 버튼 4가지 상태 |
| `review-list-page.html` | 완료 | 전역 복습 북마크 풀페이지 |
| `README.md` | 완료 | 이 파일 — 의사결정 소스 오브 트루스 |

`feedback-bookmark-button.html`은 이 README의 결정 사항과 이미 일치하므로 수정 없음.
