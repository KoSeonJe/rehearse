---
name: create-pr
description: Create a pull request following project PR rules. Use when the user asks to create a PR, push a PR, or says "/create-pr". Enforces BE/FE separation, Korean commit/PR conventions, and branch naming rules.
agent: git-manager
---

# PR 생성 스킬

이 프로젝트의 PR 규칙을 **반드시** 준수하여 PR을 생성한다.

## 실행 에이전트

이 스킬은 `git-manager` 서브에이전트로 실행한다. 메인 세션 직접 수행 금지.

```
Agent(subagent_type=git-manager, prompt="<이 SKILL 절차 수행 + 컨텍스트>")
```

`git-manager` 가 본 SKILL.md 를 Read 후 절차 수행.

## 실행 절차

### Step 1: 변경 파일 분석

```bash
git status -u
git diff --stat
git diff HEAD~1..HEAD --stat  # 이미 커밋된 경우
```

변경된 파일을 `backend/`, `frontend/`, 기타(docs, config)로 분류한다.

### Step 2: BE/FE 분리 판단 (필수)

**규칙: 백엔드와 프론트엔드 작업은 별도 PR로 분리한다.**

- `backend/` + `frontend/` 모두 변경됨 → **반드시 분리 PR**
  - 사용자에게 "BE/FE 분리 PR 규칙에 따라 2개 PR로 나눠야 합니다. 진행할까요?" 확인
  - BE PR 먼저 생성 → FE PR 생성
- `backend/`만 변경 → `[BE]` 단일 PR
- `frontend/`만 변경 → `[FE]` 단일 PR
- docs/config만 변경 → 영역 생략

**예외 없음.** "변경이 작아서", "필드 하나라서" 등의 이유로 합치지 않는다.

### Step 3: 브랜치 생성

```
feat/{기능명}    # 새 기능
fix/{버그명}     # 버그 수정
refactor/{대상}  # 리팩토링
docs/{문서명}    # 문서
```

BE/FE 분리 시:
```
feat/{기능명}-be   # 백엔드
feat/{기능명}-fe   # 프론트엔드
```

### Step 4: 커밋 메시지 작성

- **한국어**, conventional commits
- 타입: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`
- 끝에 Co-Authored-By 추가

```
feat: 면접 준비 페이지 디바이스 테스트 UX 개선

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

### Step 5: PR 생성

**PR 타이틀 형식:**
```
[영역] 타입: 한줄 설명
```

- 영역: `[FE]`, `[BE]`, `[FE/BE]`(사용하지 않음 — 분리하므로), 생략(docs/chore)
- 70자 이내

**⚠️ 금지 표현 — 타이틀·본문 모두 적용:**

내부 작업 계획의 흔적(플랜 문서 파일명, Phase/Task/Plan 번호, 스프린트 식별자 등)은 **PR 타이틀·본문 어디에도 노출하지 않는다**. 리뷰어 입장에서는 의미 없는 내부 식별자일 뿐이며, 플랜 문서 자체는 `docs/plans/{YYYY-MM-DD-topic}/{product_spec,tech_spec}/`에 따로 링크하면 된다.

금지 예 (타이틀):
- ❌ `[FE] refactor: Plan 10 Phase B.1 레이아웃 primitive`
- ❌ `[FE] feat: Phase D - setup 4+8 · editorial 개편`
- ❌ `[BE] fix: Task 3 — 면접 생성 API 레이어링 정리`
- ❌ `[FE] feat: 스프린트-2 T2.5 페이지 타이틀 고유화`

권장 예 (타이틀):
- ✅ `[FE] refactor: 레이아웃 primitive 6종 도입 (PageGrid/StickyRail 등)`
- ✅ `[FE] feat: 면접 Setup 페이지 editorial 레이아웃 개편`
- ✅ `[BE] fix: 면접 생성 API 레이어링 정리`
- ✅ `[FE] feat: 페이지별 title 고유화 (review-list / admin-feedbacks)`

본문에서도 "Phase B.1", "Plan 10", "T2.5", "Sprint 2" 같은 내부 식별자는 제거하고 **무엇이 달라지는지**로 환원한다. 플랜 문서 참조는 `## 문제 & 동기` 섹션의 `스펙: docs/plans/{YYYY-MM-DD-topic}/{product_spec,tech_spec}/...` 링크로만 남긴다.

**⚠️ 타이틀은 "개발자 관점의 결과"로 작성 (필수 원칙):**

타이틀 단독으로 리뷰어/동료 개발자가 **"이 PR 이 머지되면 내가 뭘 볼 수 있나 / 뭐가 바뀌나"** 를 바로 파악할 수 있어야 한다. 내부 구현 용어(클래스명·라이브러리 객체 타입·의존성 이름·내부 메트릭 규약·"완결/정비/표준" 같은 추상 명사)로 타이틀을 채우지 않는다.

금지 예 (내부 용어·구현 용어 노출):
- ❌ `[BE] feat: AI 호출 관측 완결 — Counter 4종 + prometheus registry + 대시보드 표준`
- ❌ `[BE] refactor: FollowUpService 에 IntentClassifier 주입`
- ❌ `[FE] feat: useInterviewTurn 훅 분리 + zustand store 정리`
- ❌ `[BE] chore: MeterRegistry 바인딩 정비`

권장 예 (관측 가능한 결과·도메인 용어):
- ✅ `[BE] feat: AI 호출 토큰·지연·캐시 히트율을 Prometheus 로 노출`
- ✅ `[BE] feat: 꼬리질문이 사용자 의도에 맞지 않을 때 분기 처리`
- ✅ `[FE] feat: 면접 진행 중 턴 상태를 탭 간 유지`
- ✅ `[BE] chore: AI 호출 비용 대시보드 지표 추가`

변환 가이드:
- `Counter 4종 추가` → "무엇을 계측하게 됐나?" → `토큰·지연·캐시 히트율 노출`
- `X 추상화 / 분리 / 정비` → "사용자 또는 후속 작업자가 뭘 얻게 되나?" → `X 가 Y 할 때 Z 동작`
- 라이브러리·프레임워크 고유명사(`MeterRegistry`, `RestTemplate`, `zustand`) 는 **본문** 에서만 언급

**PR 본문 작성 원칙 (최우선):**

> **핵심: 리뷰하는 개발자가 빠르고 정확하게 이해할 수 있도록 작성한다.**
> PR 본문의 독자는 코드를 처음 보는 리뷰어다. "내가 뭘 했는지"가 아니라 "리뷰어가 뭘 알아야 하는지" 관점으로 쓴다.

- **비즈니스 관점으로 작성** — 사용자/기능 관점의 변경 사항을 구체적으로 기술
- **코드 구현 세부사항 금지** — 파일명, 함수명, 필드명, 타입명 등 코드 레벨 내용을 나열하지 않는다
- 리뷰어가 "이 PR이 제품에 어떤 변화를 주는지" **5분 내 파악**할 수 있어야 한다
- **문제 → 해결 흐름으로 서술** — 왜 이 변경이 필요한지(동기)를 먼저, 무엇을 바꿨는지(변경)를 다음에
- **리뷰 포인트는 구체적으로** — "확인 부탁드립니다"가 아니라 "A 로직에서 B 케이스 처리가 맞는지"처럼 리뷰어가 바로 집중할 수 있게
- **해당 없는 섹션은 삭제** — 빈 섹션을 남기지 않는다

**⚠️ PR 본문은 반드시 `.github/pull_request_template.md` 템플릿 구조를 그대로 따른다.**
- 템플릿에 정의된 섹션 순서와 형식을 임의로 변경하지 않는다
- 섹션을 추가하거나 순서를 바꾸지 않는다 — 해당 없는 섹션만 삭제한다
- 템플릿의 주석(<!-- -->)에 적힌 가이드를 반드시 읽고 그 의도에 맞게 작성한다

**PR 본문 형식:**
```markdown
## 문제 & 동기

<!-- 이 PR이 왜 필요한지 1~3문장. 이슈/스펙 링크 -->

- closes #N
- 스펙: `docs/plans/{YYYY-MM-DD-topic}/{product_spec,tech_spec}/...`

## 변경 내용

<!-- 사용자/기능 관점으로 무엇이 달라지는지 -->

- 변경 사항 1
- 변경 사항 2

## 영향 범위 & 리스크

<!-- 다른 기능에 미치는 영향. 없으면 섹션 삭제 -->

- **영향 범위**: 예) CS 외 면접 타입 10개
- **롤백 방법**: 리버트 커밋

## 테스트

- [ ] CI 통과
- [ ] 수동 테스트: 구체적 시나리오

## 리뷰 포인트

<!-- 리뷰어가 집중해서 봐야 할 부분 1~3개 -->

1. 포인트

## 스크린샷

<!-- UI 변경이 없으면 삭제 -->

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

**좋은 예:**
```
## 문제 & 동기

CS 카테고리 필터가 모든 면접 타입에 적용되어, 시드 데이터가 있는데도 매번 AI가 새로 질문을 생성하는 버그

## 변경 내용

- CS_FUNDAMENTAL일 때만 카테고리 필터 적용하도록 조건 분기 추가
- DB에서 AI 오염 데이터 비활성화

## 영향 범위 & 리스크

- **영향 범위**: CS_FUNDAMENTAL 외 cacheable 면접 타입 10개
- **롤백 방법**: 리버트 커밋

## 테스트

- [x] CI 통과
- [ ] 배포 후 LANGUAGE_FRAMEWORK 면접 생성 시 캐시 히트 확인

## 리뷰 포인트

1. 조건 분기가 CS_FUNDAMENTAL만 정확히 걸러내는지
```

**나쁜 예 (코드 구현 나열 — 금지):**
```
- `device.ts`: DeviceTestStatus 타입 추가
- `use-device-test.ts`: startCameraTest(), startMicTest() 함수 추가
- `Interview.java`: csSubTopics 컬럼 추가
```

**base 브랜치:** `develop`

```bash
gh pr create --title "[BE] feat: 설명" --body "..." --base develop
```

### Step 6: PR URL 반환

생성된 PR URL을 사용자에게 전달한다.

## BE/FE 분리 PR 워크플로우

BE + FE 동시 변경 시:

1. 현재 브랜치에서 BE 파일만 스테이징 → BE 브랜치 생성 → 커밋 → 푸시 → BE PR 생성
2. FE 브랜치 생성 → FE 파일만 스테이징 → 커밋 → 푸시 → FE PR 생성
3. 공통 파일(CLAUDE.md, docs 등)은 관련성 높은 쪽에 포함

## 체크리스트 (PR 생성 전 자체 검증)

- [ ] BE/FE 분리 규칙 준수 여부
- [ ] 브랜치명 규칙 준수 (`feat/`, `fix/` 등)
- [ ] PR 타이틀 형식 `[영역] 타입: 설명`
- [ ] PR 타이틀·본문에 Plan/Phase/Task/Sprint 등 내부 작업 식별자 미포함
- [ ] PR 타이틀이 내부 구현 용어(클래스명·라이브러리 타입·"완결/정비/표준" 추상 명사) 대신 **개발자가 바로 인지하는 관측 가능한 결과**로 작성됐는지
- [ ] 커밋 메시지 한국어 + conventional commits
- [ ] base 브랜치 `develop`
- [ ] 민감 파일(.env 등) 미포함
