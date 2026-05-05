---
name: create-issue
description: "GitHub Issue 생성 — 사용자에게 단계별 질문하여 type / area / priority / body 채운 후 gh issue create. Plan 폴더 생성은 별도 (이 스킬 = Issue 생성만)."
---

# Create Issue

GitHub Issue 를 대화형으로 생성. 한 번에 한 질문씩 묻고, 답변 누적해서 최종 `gh issue create` 호출.

## 핵심 원칙

- **한 번에 한 질문**. 다중 질문 금지.
- **`AskUserQuestion` 도구 우선** (선택지 명확한 경우). 자유서술은 필드 텍스트 입력 시만.
- **모호 답변 = 재질문**. 자율 추측 금지.
- **사용자 명시 confirm 후 `gh issue create` 호출** (Blocking).
- 라벨 / 템플릿 = 이 repo `.github/ISSUE_TEMPLATE/` + 라벨 스킴 (type / area / priority) 따름.

## Step 1 — type 분류

`AskUserQuestion`:

```
question: "어떤 종류의 이슈인가요?"
options:
  - "Epic — 큰 작업, plan 폴더 1:1 매핑 (product-spec / tech-spec 작성 예정)"
  - "Feat — 작은 기능 추가 (plan 폴더 없이 진행)"
  - "Fix — 버그 fix (재현 명확, 작은 변경)"
  - "Bug — 버그 리포트 (재현 / 기대 / 실제 분석 필요)"
  - "Chore — 인프라 / 문서 / 의존성 정리"
  - "Refactor — 동작 변경 없는 구조 개선"
```

→ type 라벨 결정: `type:epic` / `type:feat` / `type:fix` / `type:bug` / `type:chore` / `type:refactor`.

## Step 2 — area

`AskUserQuestion`:

```
question: "영향 영역?"
options:
  - "BE — backend"
  - "FE — frontend"
  - "lambda — analysis / convert"
  - "infra — CI/CD / AWS / 도커"
  - "docs — 문서 전용"
  - "BE+FE — 동시 작업 (Epic 한정)"
```

→ 라벨: `BE` / `FE` / `lambda` / `infra` / `docs` (BE+FE 선택 시 두 라벨 동시).

## Step 3 — priority

`AskUserQuestion`:

```
question: "우선순위?"
options:
  - "P0 — 즉시 (서비스 장애 / 데이터 손실 / 보안)"
  - "P1 — 이번 주 (현재 sprint 핵심)"
  - "P2 — 보통 (다음 sprint 까지)"
  - "P3 — backlog (시간 날 때)"
```

→ 라벨: `priority:P0` / `priority:P1` / `priority:P2` / `priority:P3`.

## Step 4 — title

자유서술 입력 받기. 한 줄 (50자 권장, 80자 max). 한국어.

```
"이슈 한 줄 제목을 알려주세요. (type prefix 자동 추가됨)"
```

→ 최종 title = `[Epic] {입력}` / `[Feat] {입력}` / `[Bug] {입력}` 등. type 별로 prefix 변동.

## Step 5 — type 별 body 질문 분기

### Epic

순서대로 (한 번에 1개):

1. **Why** — "왜 필요한가요? 배경 / 문제 / 기회를 알려주세요." (자유서술)
2. **Goal** — "측정 가능한 성공 기준은? 'X가 Y할 수 있다' 형태." (자유서술)
3. **수용기준** — "완료 판정 체크리스트 (3-5개)." (자유서술 → bullet 으로 정리)
4. **비스코프** — "이번 Epic 에서 다루지 않을 항목? (없으면 '없음')" (자유서술)
5. **plan 폴더 slug** — "plan 폴더용 slug? (kebab-case, 예: `interview-quality-sprint`)"

→ body = `.github/ISSUE_TEMPLATE/epic.md` 구조 채우기. plan 폴더 경로 = `docs/plans/{Issue번호}-{slug}/` (Issue 번호는 생성 후 확정).

### Feat

1. **목적** — "이 기능 무엇을 / 왜?"
2. **변경 영역** — "어떤 파일 / 모듈 / 도메인 영향?"
3. **완료 조건** — "DoD 체크리스트 2-4개."

### Fix

1. **현상** — "무엇이 잘못 동작?"
2. **원인 (알면)** — "원인 추정?"
3. **수정 범위** — "어떤 파일 / 함수 영향?"

### Bug

1. **재현 절차** — "1, 2, 3 단계로."
2. **기대 동작**
3. **실제 동작**
4. **환경** — "브라우저/OS, 백엔드 환경 (local/dev/prod), 발생 시각, userId/interviewId (있으면)."
5. **로그 / 스크린샷** — "첨부할 것 있으면 경로 / 링크." (옵션)

### Chore / Refactor

1. **무엇을** — "구체적으로 어떤 정리 / 개선?"
2. **왜** — "현재 어떤 문제 / 갭?"
3. **완료 기준** — "어떻게 끝났다고 판단?"

## Step 6 — preview + confirm

채워진 body markdown + title + label 을 사용자에게 제시. `AskUserQuestion`:

```
question: "이 내용으로 Issue 생성할까요?"
options:
  - "생성 — 그대로 진행"
  - "수정 — 특정 필드 다시 작성"
  - "취소 — 중단"
```

수정 선택 시 → "어떤 필드?" 재질문 → 해당 step 만 재실행 → 다시 preview.

## Step 7 — `gh issue create`

승인 후:

```bash
gh issue create \
  --title "{title}" \
  --label "type:{type},{area},priority:{P}" \
  --body "{body}"
```

multi-area (BE+FE) = label 컴마 추가. 결과 = Issue URL 출력.

## Step 8 — 후속 안내

- Epic 생성 시: "Issue #N 생성 완료. plan 폴더 만들려면: `mkdir -p docs/plans/{N}-{slug}` + 템플릿 복사 (`docs/plans/_templates/`)." — 자동 생성 X (이 스킬 범위 외).
- 기타 type: URL 만 보고.

## 라벨 부재 대응

`gh issue create` 가 라벨 없음 에러 시:

1. 사용자에게 "라벨 `{name}` 가 repo 에 없음. 생성할까요?" 질문 (`AskUserQuestion`).
2. 승인 시 `gh label create "{name}" --color "{hex}" --description "{desc}"`.
3. 라벨 스킴 (참고):
   - `type:epic` (#5319e7) / `type:feat` (#a2eeef) / `type:fix` (#fbca04) / `type:bug` (#d73a4a) / `type:chore` (#cfd3d7) / `type:refactor` (#bfdadc)
   - `priority:P0` (#b60205) / `priority:P1` (#d93f0b) / `priority:P2` (#fbca04) / `priority:P3` (#0e8a16)
   - area 라벨 (BE / FE / infra / lambda) = 기존 사용. `docs` 신규 시 동일 패턴 (#0075ca).

## 안티 패턴

- 한 메시지에 질문 여러 개.
- 사용자 답변 추측 / 자율 채움.
- preview 생략하고 바로 `gh issue create`.
- type / area / priority 라벨 누락.
- title 에 type prefix (`[Epic]` 등) 자동 추가 안 함.
- Epic 인데 plan 폴더 자동 생성 (스킬 범위 외).
