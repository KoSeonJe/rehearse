# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

> **먼저 `frontend/AGENTS.md` 를 Read 후 본 파일로 돌아온다.**
> - AGENTS.md = 진입점 / 기술 스택 / 핵심 룰 / 도메인 맵.
> - `frontend/.claude/rules/` = 코드 컨벤션 (`conventions.md`) / 아키텍처 (`architecture.md`) / 테스트 정책 (`testing.md`).
> - `frontend/DESIGN.md` = 디자인 토큰 (디자인 / UI 작업 시에만).
>
> 본 파일은 `frontend/` 자동 로드되는 Claude 세션 전용 추가 컨텍스트만 보유.

## Claude 에이전트 호출 매핑 (FE)

| 작업 유형 | 에이전트 |
|----------|---------|
| 신규 구현 | `frontend` / `frontend-developer` |
| 디자인 / UX | `designer` |
| 디버깅 | `debugger` |
| 코드 리뷰 (보안 / 성능) | `code-reviewer` |
| 코드 리뷰 (레이어 / SOLID) | `architect-reviewer` |
| 테스트 작성 | `test-engineer`, `qa` |

## 서브에이전트 주의

서브에이전트는 격리 컨텍스트. 호출 시 다음 문서를 Read 하도록 프롬프트에 명시:

- `frontend/AGENTS.md`
- `frontend/.claude/rules/` 하위 전체 (`conventions.md` / `architecture.md` / `testing.md`)
- `frontend/DESIGN.md` (디자인 / UI 작업 시에만)

루트 `AGENTS.md` `@import` (commit / comments / security / plan-mode) 는 부모 세션 자동 로드 → 서브에이전트 별도 로드 필요 시 명시.
