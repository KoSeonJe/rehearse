---
name: implement-fe
description: "Frontend 구현 진입 정형화 스킬. product-spec + tech-spec + implement.md 검증 → git worktree 생성 → spec 3종 1커밋 → frontend agent 위임까지 자동. 메인 세션이 spec 누락한 채 구현 진입하는 사고 방지. '/implement-fe {slug}', 'FE 구현 시작', 'frontend implement 진입', '이 plan 프론트 구현' 등 트리거. slug 인자 필수. 호출 시 docs/plans/{slug}/ 안의 spec 3종 검증 후 ../devlens-{slug} worktree + feat/{slug} 브랜치 생성. 코드 변경은 frontend agent 가 implement.md 따라 자율 진행."
---

# Implement FE

Frontend 구현 진입 정형 절차. spec 3종 검증 → worktree → spec 커밋 → `frontend` agent 위임.

## 입력 검증 (Blocking)

1. `{slug}` 인자 누락 → `AskUserQuestion` 으로 plan 폴더 선택지 제시 (`ls docs/plans/` 결과 최근 5개). 자율 추정 금지.
2. `docs/plans/{slug}/` 디렉토리 존재 확인. 없으면 종료 + 사용자 보고.
3. spec 3종 존재 확인 — 다음 패턴 중 **각 카테고리 하나씩** 충족:
   - product: `product-spec.md` 또는 `product_spec/requirements.md`
   - tech: `tech-spec.md` 또는 `tech_spec/design.md`
   - implement (FE): `implement.md` 또는 `implement-fe.md` 또는 `tasks/fe-*.md`

   누락 시 무엇이 비었는지 명시하고 종료.

## Worktree 생성

```bash
cd /Users/koseonje/dev/devlens
git fetch origin develop
```

위치 / 브랜치:
- 디렉토리: `../devlens-{slug}`
- 브랜치: `feat/{slug}` (base: `origin/develop` 최신)

분기 처리:
- 양쪽 다 없음 → `git worktree add -b feat/{slug} ../devlens-{slug} origin/develop`
- 브랜치만 존재 → `git worktree add ../devlens-{slug} feat/{slug}`
- 디렉토리 존재 (worktree list 확인) → 재사용 + "기존 worktree 사용" 보고
- 디렉토리는 있는데 worktree 등록 안 됨 → 사용자에게 처리 방향 문의 (자율 삭제 금지)

검증: `git worktree list` 로 등록 확인.

BE+FE 같은 plan 인 경우 — 기존 worktree (BE 가 먼저 생성) 재사용. 이 경우 `feat/{slug}` 브랜치 그대로 이어서 작업.

## Spec 3종 1커밋

worktree 진입 후 (`cd ../devlens-{slug}`):

```bash
git status --short docs/plans/{slug}/
```

`docs/plans/{slug}/` 하위 untracked / modified 파일이 spec 3종 (product / tech / implement 계열) 만 포함하는지 확인. 다른 plan 폴더 / 소스 코드 함께 변경됨이 감지되면 alert 후 사용자 결정 대기.

변경 있으면:

```bash
git add docs/plans/{slug}/
git commit -m "docs(plans): {slug} spec 작성"
```

커밋 룰 (`/Users/koseonje/dev/devlens/.claude/rules/commit.md`):
- type=docs, scope=plans, 한국어 명령형, body 없음
- `--no-verify` 금지

이미 develop 에 머지됐거나 BE 스킬이 먼저 spec 커밋 완료 = skip + "spec 이미 커밋됨" 보고.

## Frontend Agent 위임

`Read` 로 다음 파일 본문 흡수:
- `docs/plans/{slug}/implement.md` (또는 `implement-fe.md`)
- `docs/plans/{slug}/tech-spec.md` (또는 `tech_spec/design.md`)
- `docs/plans/{slug}/product-spec.md` (또는 `product_spec/requirements.md`)
- `tasks/fe-*.md` 존재 시 모두 (분리 임계 초과 케이스)

`Agent(subagent_type=frontend, prompt=...)` 1회 호출. prompt 본문에 다음 인라인 포함:

```
## 작업 디렉토리

`../devlens-{slug}` (이미 worktree 진입 완료. 모든 파일 작업 / 커밋 이 디렉토리에서 수행).
브랜치: `feat/{slug}`. base: `develop`.

## 컨벤션 로드 (Blocking — 시작 전 Read 직접)

1. `frontend/.claude/rules/conventions.md`
2. `frontend/.claude/rules/architecture.md`
3. `frontend/.claude/rules/testing.md`
4. `frontend/AGENTS.md`
5. `.claude/rules/commit.md` (커밋 메시지 룰)
6. `.claude/rules/security.md` (OWASP Top 10 self-check)

디자인 / UI 작업 포함 시 추가: `frontend/DESIGN.md`.

미로드 상태로 구현 진행 시 리뷰 단계 재작업 사유.

## Plan 문서

다음은 본 task 의 product-spec / tech-spec / implement plan 전문. 진행 중 의문점 = 우선 spec 재확인.

### product-spec

{product-spec.md 본문 그대로}

### tech-spec

{tech-spec.md 본문 그대로}

### implement plan (FE)

{implement.md 또는 implement-fe.md 본문 그대로 + tasks/fe-*.md 있으면 함께}

## 진행 방식

- implement plan 의 Phase 순서대로 끝까지 자율 진행. 사용자 사전 승인 = 본 스킬 호출.
- 각 Phase 완료마다 별도 커밋 (commit.md 룰: `feat(FE):`, `fix(FE):`, `refactor(FE):`, `test(FE):` 등).
- 빌드 / 테스트 / 타입체크 통과 상태로만 커밋. `console.log` 커밋 금지. `any` 금지.
- progress.md 존재 시 Phase 완료마다 행 상태 갱신.

## 중단 조건 (즉시 main 복귀)

다음 발견 시 자율 진행 금지 — main 세션에 보고 후 사용자 결정:

- 보안 위반 / 가능성 (`.claude/rules/security.md` OWASP Top 10)
- spec 미커버 케이스 / 모호 요구사항
- tech-spec 과 도메인 코드 충돌 (spec 갱신 필요)
- implement plan 의 trade-off 결정 항목 발견
- BE API contract 부재 / 불일치 발견

보고 형식 = `.claude/rules/reporting.md` 의 `AskUserQuestion` 질문 친절도 룰 준수.

## 완료 보고

- 구현된 Phase 목록 + 커밋 SHA 목록
- 테스트 / 타입체크 / 린트 통과 여부 / 미완 영역
- 다음 단계 안내 (리뷰 / PR)
```

## 메인 세션 보고 (스킬 종료 시)

```
**implement-fe 완료**

- worktree: ../devlens-{slug}
- 브랜치: feat/{slug}
- spec 커밋: {SHA 또는 "이미 커밋됨"}
- frontend agent 결과: {요약}

**다음 단계** (사용자 결정):
1. 통합 리뷰 — `code-reviewer-frontend` 호출
2. PR 생성 — `/create-pr` (git-manager 위임)
3. 추가 작업 / 미완 처리
```

## 룰 정합성

- `.claude/rules/branch-pr.md`: `feat/{slug}` 네이밍 / 직접 push 금지 / BE PR 머지 후 FE PR 생성 룰 / PR 머지 사용자 승인 필요
- `.claude/rules/commit.md`: `docs(plans):` scope, 한국어 명령형, body 없음
- `.claude/rules/plan-mode.md`: 스킬 호출 시점 = 사용자 명시 승인. spec 누락 시 차단 게이트 동작
- `AGENTS.md` Mandatory Delegation: PR 생성 / 머지는 본 스킬 범위 외 — git-manager 후속 위임

## 안티 패턴

- slug 인자 없는데 자율 추정해 진행
- spec 3종 누락 검출했는데 진행
- worktree 디렉토리 존재 시 자율 삭제
- 소스 코드 변경이 spec 커밋에 섞여 들어감
- frontend agent prompt 에 컨벤션 로드 지시 누락
- 보안 위반 / 모호함 / API contract 불일치 발견했는데 agent 가 자율 진행
- main 세션이 worktree 안 들어가고 메인 디렉토리에서 코드 변경
- BE+FE plan 에서 BE 머지 전 FE 단독 PR 생성 (branch-pr.md 위반)
