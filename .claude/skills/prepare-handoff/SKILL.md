---
name: prepare-handoff
description: "현재 plan 의 handoff.md 갱신 + 다음 세션 시작 프롬프트 (복사 붙여넣기용) 생성. '/prepare-handoff', '핸드오프 준비', '다음 세션 인계', '세션 정리 후 인계' 등 명시 호출 시. 출력: docs/plans/{N}-{slug}/handoff.md (없으면 신규) + 코드블록 시작 프롬프트. 컨텍스트 30-40% 잔여 시 사용자 권장."
---

# Prepare Handoff

현재 plan 의 `handoff.md` 갱신 + 다음 세션 시작용 복사 가능 프롬프트 생성.

## 실행 에이전트

`docs-manager` 서브에이전트로 실행 (CLAUDE.md "Mandatory Delegation — handoff = docs-manager"). 메인 세션 직접 수행 금지.

```
Agent(subagent_type=docs-manager, prompt="<이 SKILL 절차 수행 + 현재 세션 컨텍스트 인계>")
```

메인 세션은 **현재 세션의 진행 상황 / 미해결 결정 / 다음 작업 후보**를 docs-manager 에 전달한 뒤 결과만 사용자에게 노출.

## 전제 (Read 필수, Blocking)

스킬 실행 직후 다음 문서 Read:

- `docs/plans/AGENTS.md` — handoff 파일 역할 / 수명 / 작성 시점
- `docs/plans/_templates/handoff.md` — 출력 템플릿 구조

## 핵심 원칙

- **자율 분석 + 컨텍스트 흡수**. 메인 세션 → docs-manager 전달 정보 + git log + plan 폴더 직접 분석.
- **handoff.md 부재 = 자동 생성** (사용자 추가 confirm X). plan 폴더만 존재하면 진행.
- **다음 세션 프롬프트 = 간결**. 경로 + 다음 작업 1줄 + 첫 명령. 모든 컨텍스트는 handoff.md 본문에 위임 (중복 X).
- **승인 게이트 X**. handoff 는 단명 문서 — preview → 즉시 Write.

## Phase A — Investigation (자율)

### A-1. 현재 plan 폴더 감지

```bash
# handoff.md 존재 = 진행 중 plan (최우선)
find docs/plans -maxdepth 2 -name handoff.md -exec stat -f "%m %N" {} \; | sort -rn | head -3

# implement*.md 최근 수정 (handoff 없는 신규 plan 후보)
find docs/plans -maxdepth 2 \( -name "implement.md" -o -name "implement-be.md" -o -name "implement-fe.md" \) -exec stat -f "%m %N" {} \; | sort -rn | head -3
```

후보 1개 = 자동 선택. 2개+ → `AskUserQuestion` (handoff 진행중 우선 표시).

후보 0개 = STOP. "plan 폴더 없음. `/create-product-spec` 부터 시작 권장." 종료.

### A-2. 현재 상태 수집

```bash
git status -sb
git log --oneline -10
git rev-parse --abbrev-ref HEAD
git log -1 --format="%H %s"
```

**메인 세션 → docs-manager 전달 정보 활용**:
- 이번 세션 완료된 작업 / 결정
- 미해결 blocker / 사용자 결정 대기 항목
- 다음 작업 후보 (Phase 번호 / 제목)

### A-3. implement.md 진행도 추정

선택 plan 의 `implement*.md` Read:

- 완료 표시 (체크박스 / 커밋 메시지 매칭) — `git log` 와 대조
- 다음 미완료 Phase = "다음 시작점"
- Phase 의 첫 Verification 명령 = "첫 명령" 후보

## Phase B — Synthesis (handoff 작성)

`docs/plans/_templates/handoff.md` 섹션 그대로:

### 현재 상태
- 진행: `implement-be.md` Phase X 완료 (커밋 `abc1234`)
- 브랜치: `{현재 브랜치}`
- 관련 PR: #N (상태)
- 빌드 / 테스트: 통과 / 실패 + 사유

### 다음 세션 시작점
- 다음 작업: `implement.md` Phase X+1 — {제목}
- 참조: `tech-spec.md#{anchor}`
- 첫 명령: `./gradlew test --tests XxxTest` (또는 npm)
- 예상 변경 파일: `path/to/Xxx.java`

### 미해결 질문 / Blocker
사용자 결정 대기 항목. 없으면 "없음".

### 컨텍스트 메모
함정 / 결정 / 환경 / 임시 우회. 다음 세션이 놓치면 안 되는 것.

### 참고 명령
자주 쓰는 명령 모음.

마지막 줄: `업데이트: YYYY-MM-DD (세션 종료 / 컨텍스트 잔여 X%)`

## Phase C — 다음 세션 프롬프트 생성

handoff.md 갱신 후 **사용자 복사용 프롬프트** 출력. 형식 고정:

````
다음 세션 시작 프롬프트 (복사):

```
docs/plans/{NNN}-{slug}/handoff.md 읽고 이어서 진행.
다음 작업: {Phase 제목 1줄}.
첫 명령: {명령}
```
````

**원칙**:
- 3-4줄 이내. 경로 + 다음 작업 + 첫 명령만.
- 모든 컨텍스트 = handoff.md 본문 위임 (중복 X).
- 코드블록 안에 넣어 사용자 한 번에 복사 가능.

## Phase D — 출력

1. handoff.md diff 요약 (어느 섹션 갱신).
2. 다음 세션 프롬프트 코드블록.
3. 추가 안내 1줄: "다음 세션 시작 시 위 프롬프트 복사 → handoff.md 자동 로드."

## 안티 패턴

- 메인 세션 직접 수행 (docs-manager 위임 룰 위반).
- handoff.md 부재 시 신규 생성 거부 / 추가 confirm (자동 생성 룰 위반).
- 다음 세션 프롬프트에 handoff 본문 복붙 (중복).
- plan 폴더 자동 감지 생략하고 사용자에게 경로 입력 요구.
- 템플릿 섹션 자의적 변형 / 추가.
- 승인 게이트 추가 (handoff = 단명 문서, 즉시 Write).
- handoff 본문에 일반 코딩 컨벤션 / 룰 복사 (다른 문서 책임).
