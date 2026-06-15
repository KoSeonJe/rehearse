# Branch and PR Rule

## Human Approval Required (Blocking)

- Never merge a Pull Request without explicit approval from the user in the current conversation.
- Applies even when plan says merge, CI passing, GitHub reports CLEAN, or sandbox escalation approved.
- Before `gh pr merge`: state PR number, title, base/head, merge method, CI/merge state. Wait for approval.

## Branch

- Default dev branch: `develop`. Production: `main`.
- Feature PR base: `develop`.
- Production release: `develop → main` (`/prod-release` 스킬, `gh release create --target main`).
- Branch naming: `feat/{name}`, `fix/{name}`, `refactor/{name}`.

## PR

- BE/FE PR 분리. BE 머지 → FE PR 생성.
- Commit 규칙: `.claude/rules/commit.md` 참조.
- PR title: `[Scope] type: short description` (Korean). Scope: `[FE]`, `[BE]`, `[FE/BE]`. 문서/chore 는 omit.
- CI 통과 필수: `Frontend CI` (lint+build), `Backend CI` (test).
- PR 생성은 `/create-pr` 스킬 (Claude). 직접 `gh pr create` 금지.

## 직접 push 금지

- `main`, `develop` 브랜치 보호로 직접 push 차단. 모든 변경 PR 경유.
- `main` 머지: 릴리즈 PR (`develop → main`) 만. feature/fix 에서 main 직접 PR 금지.
- `develop` 머지: `feat/*`, `fix/*`, `refactor/*` PR 만.
- 릴리즈 직후 develop 은 main 머지 커밋 누락 → 다음 릴리즈 PR 생성 전 `git merge origin/main` back-merge 필수.
