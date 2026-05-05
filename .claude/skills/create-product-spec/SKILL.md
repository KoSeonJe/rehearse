---
name: create-product-spec
description: "GitHub open Issue 선택 후 메타인지 대화로 product-spec.md 작성. WHY / 누구에게 / 측정기준 / 수용기준 / 비스코프 / YAGNI 점검 강제. docs/plans/{N}-{slug}/product-spec.md 생성."
---

# Create Product Spec

Issue 1개 → `docs/plans/{N}-{slug}/product-spec.md` 1개. 사용자 메타인지를 강제하는 대화형 spec 생성.

## 핵심 원칙

- **한 번에 1 질문** (brainstorming 스킬 패턴 차용).
- **`AskUserQuestion` 우선** — 선택지로 좁힐 수 있는 건 무조건 다중선택. 자유서술은 자연어 답변 필요한 필드만.
- **메타인지 강제** — "왜 / 무엇 / 누구에게 / 어디까지 / 빠지는 건 뭔가" 사용자가 직접 정리하게 한다. 추측 금지, 모호 답 = 재질문.
- **YAGNI 점검 명시** — "꼭 필요한가? 미루면? 더 작은 버전?" 항상 묻는다.
- **승인 후 파일 작성** — preview → confirm → write (Blocking).

## Step 1 — open Issue 나열 + 선택

```bash
gh issue list --state open --json number,title,labels --limit 20
```

결과를 `AskUserQuestion` 옵션 (최대 4개) 으로 제시. Epic 라벨 우선:

```
question: "어떤 Issue 의 product-spec 을 작성할까요?"
options:
  - "#42 [Epic] 인터뷰 품질 개편 (type:epic, BE+FE)"
  - "#48 [Epic] 결제 도입 (type:epic, BE)"
  - "#51 [Feat] resume 미리보기 (type:feat, FE)"
  - "다른 Issue / 새로 검색 — 직접 번호 입력"
```

5개 이상이면 첫 4개 (label `type:epic` 가중) + "다른 Issue" 옵션. 사용자가 "다른 Issue" 선택 시 자유서술로 번호 받기.

선택된 Issue 의 number / title / labels 수집:

```bash
gh issue view {N} --json number,title,body,labels
```

→ Issue body 가 product-spec 초안 가질 수 있음. Read 해서 이후 질문에 컨텍스트로 활용.

## Step 2 — slug 결정

`AskUserQuestion`:

```
question: "plan 폴더 slug? (kebab-case, 30자 이하, 의미 압축)"
options:
  - "{Issue title 자동 추론 slug} (추천)"
  - "직접 입력"
```

폴더 경로 = `docs/plans/{NNN}-{slug}/` (NNN = 3자리 zero-padding).

**아직 폴더 생성 X** — preview 단계에서 사용자 확정 후 생성.

## Step 3 — 메타인지 질문 (순서대로, 한 번에 1개)

각 질문 후 답변 누적. 모호 / 추상 답 = 재질문 ("좀 더 구체적으로 — 예시?" / "측정 가능한 형태로?" 류).

### 3-1. 현재 어떤 문제 / 갭? (자유서술)

> "지금 무엇이 안 되거나 부족한가요? 사용자 / 시스템 / 운영 어느 시점에서 발생?"

목적: 배경 / 문제 명확화. 추상 답 ("UX 안 좋다") = 재질문 ("어떤 화면 / 어떤 행동 / 어느 단계?").

### 3-2. 왜 지금? 미루면 어떻게 되나? (YAGNI 점검)

> "이걸 다음 sprint / 다음 분기로 미루면 어떤 손해? 미루면 안 되는 이유?"

목적: 우선순위 정당화. "미뤄도 무방" 답 = 우선순위 P3 또는 Issue close 권유.

### 3-3. 누구에게 가치가 가는가? (자유서술 또는 다중선택)

`AskUserQuestion`:

```
options:
  - "최종 사용자 (취준생 / 인터뷰 응시자)"
  - "운영진 / 어드민"
  - "개발자 (개발 생산성 / 유지보수)"
  - "시스템 안정성 (성능 / 비용 / 가용성)"
```

복합이면 "직접 입력" 옵션 추가.

### 3-4. 측정 가능한 성공 기준? (자유서술 + 재질문 강제)

> "Goal 을 어떻게 측정할 건가요? 'X가 Y할 수 있다', 'N% 개선', 'Nms 이내' 형태."

추상 답 ("좋아진다") = 재질문. **숫자 / 관찰 가능한 행동 강제**.

### 3-5. 수용기준 체크리스트 (자유서술, 3-7개)

> "완료 판정 체크리스트 3-5개. 각 항목 = 검증 가능한 단언."

빈약 (1-2개) = "더 있나? 엣지케이스 / 실패 케이스?" 재질문.

### 3-6. 비스코프 — 이번에 안 할 것 (자유서술)

> "MVP 에서 의도적으로 빼는 것? 비슷해 보이지만 다른 작업?"

"없음" 답 = 한 번 더 확인 ("정말 없나? 비슷한 기능 중 미루는 것? 미래 확장 영역?").

### 3-7. 의존성 / 선행 (자유서술 또는 "없음")

> "이 작업 시작 전 끝나야 하는 다른 Issue / 작업? 또는 외부 의존 (AWS / 3rd party)?"

### 3-8. 더 작은 버전 가능? (YAGNI 점검 2)

> "지금 정의한 것보다 더 작게 자를 수 있나? phase 1 / phase 2 분리 가능?"

목적: 스코프 압축 기회 탐색. 자르기 가능 시 → 비스코프 / 별도 Issue 분리 권유.

## Step 4 — preview

수집한 답변으로 product-spec.md 초안 작성. 템플릿: `docs/plans/_templates/product-spec.md` 구조 따름.

```markdown
# Product Spec — {title}

> Issue: #{N}
> 작성일: {YYYY-MM-DD}

## Why
{3-1 + 3-2 답변 정리}

## 누구에게
{3-3 답변}

## Goal
{3-4 답변, 측정 가능 형태}

## 수용기준
- [ ] {3-5 항목 1}
- [ ] {3-5 항목 2}
...

## 비스코프
- {3-6 항목}

## 의존 / 선행
- {3-7 항목}
```

`AskUserQuestion`:

```
question: "이 product-spec 으로 파일 생성할까요?"
options:
  - "생성 — 그대로 진행"
  - "수정 — 특정 섹션 다시 답변"
  - "취소"
```

수정 선택 시 → 어느 섹션? → 해당 step 만 재실행 → 다시 preview.

## Step 5 — 폴더 + 파일 생성

승인 후:

```bash
PLAN_DIR=docs/plans/{NNN}-{slug}
mkdir -p "$PLAN_DIR"
```

`Write` 도구로 `$PLAN_DIR/product-spec.md` 작성.

Issue 에 폴더 경로 코멘트 자동 추가 (옵션, 사용자 confirm 후):

```bash
gh issue comment {N} --body "📁 plan: \`docs/plans/{NNN}-{slug}/\`"
```

## Step 6 — 후속 안내

- "product-spec 작성 완료. 다음 = tech-spec. `/create-tech-spec` 호출 시 이 폴더 자동 추천됨."
- 커밋은 별도 (skill 자동 커밋 X). 사용자가 결정.

## 안티 패턴

- 한 메시지에 질문 여러 개.
- 추상 답 그대로 수용 ("좋아진다", "잘 된다", "UX 개선" 등 측정 불가 표현).
- 비스코프 / YAGNI 질문 생략.
- preview 없이 파일 작성.
- product-spec 안에 구현 디테일 (HOW) 침범. tech-spec 영역.
- 폴더 / Issue 번호 / slug 사용자 미확인 자동 결정.
