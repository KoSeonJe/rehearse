# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **먼저 `backend/AGENTS.md` 를 Read 후 본 파일로 돌아온다.** 기술 스택 / 핵심 룰 / 도메인 맵 / 테스트 정책 등 모든 보편 컨텍스트는 AGENTS.md 단일 소스.
> 본 파일은 `backend/` 자동 로드되는 Claude 세션 전용 추가 컨텍스트만 보유.

## Claude 에이전트 호출 매핑 (BE)

| 작업 유형 | 에이전트 |
|----------|---------|
| 신규 구현 | `backend` (구현 설계 + 코드) |
| 디버깅 | `debugger-backend` (재현 + 원인 분석 + minimal fix. 큰 변경 = `backend` 위임) |
| 코드 리뷰 | `code-reviewer-backend` (룰 위배 + 성능 / 확장성 / 클린코드 / 쿼리 효율성) |
| 테스트 작성 | `backend` (구현과 동시 작성. testing.md 카테고리 분류 강제) |

## 서브에이전트 주의

서브에이전트는 격리 컨텍스트. 호출 시 다음 문서를 Read 하도록 프롬프트에 명시:

- `backend/AGENTS.md`
- `backend/.claude/rules/conventions.md`
- `backend/.claude/rules/testing.md`

루트 `AGENTS.md` `@import` (commit / comments / security / plan-mode) 는 부모 세션 자동 로드 → 서브에이전트 별도 로드 필요 시 명시.
