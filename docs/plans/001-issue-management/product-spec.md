# Product Spec — Issue 기반 Spec-Driven 작업 시스템

> 폴더명 `001-...` 임시. Issue 생성 직후 `{N}-issue-management` 로 rename.

---

## Why / Background

- 현재 상태:
  - `docs/todo/{date}/` 가 일자별 핸드오프 메모로 사용됨. plans/ 와 역할 모호 중복.
  - 백로그 / 버그 / feat 추적 일관 도구 없음. 머릿속 + 산발적 메모.
  - product_spec / tech_spec 폴더 구조는 정의되었으나 **Issue 와 매핑 룰** 부재.
- 문제점:
  - 작은 bug 발견 시 즉시 추적 안 됨 → 잊히거나 다음 PR에 묻어감.
  - 큰 작업 spec 이 plans/ 에만 있고, 진행 상태 추적 단위 (Issue) 와 분리됨.
  - 세션 핸드오프가 docs/todo/{date}/ 라는 단명 위치에 흩어짐.
  - BE/FE 동시 작업 시 어떻게 분리할지 룰 부재.
- 동기:
  - 솔로 개발 단계지만 추적성 / 일관성 도구 필요.
  - 작업 종류별 (bug / feat / refactor / chore) 일관 처리 룰 합의.
  - Claude 세션 간 컨텍스트 손실 최소화.

## Goal

- [ ] 모든 작업이 단일 도구 (GitHub Issue) 로 추적됨
- [ ] spec 필요 작업은 `docs/plans/{N}-{slug}/` 폴더 1개 = Epic Issue 1개 1:1 매핑
- [ ] BE+FE 동시 작업이 contract-first 병렬 가능 (BE 머지 대기 X)
- [ ] Claude 세션 간 컨텍스트 인계 = `handoff.md` 파일 1개 읽으면 즉시 재개 가능
- [ ] 운영 룰 + 템플릿 = `docs/plans/AGENTS.md` + `_templates/` 단일 진실 출처

## 수용 기준

- [ ] `docs/plans/AGENTS.md` 작성 (8 섹션: 폴더 트리거 / 명명 / 파일역할 / 워크플로우 / BE-FE분리 / 분리임계 / handoff / 종료)
- [ ] `docs/plans/_templates/` 7종 템플릿 존재 (product-spec, tech-spec, implement, implement-be, implement-fe, handoff, task)
- [ ] 루트 `AGENTS.md` "Spec-Driven Work" 섹션 새 룰 반영
- [ ] `docs/todo/` 제거 + `.gitignore` 정리
- [ ] dogfood: 본 plan (`001-issue-management`) 자체가 새 룰 따라 product-spec / tech-spec / implement 작성됨
- [ ] GitHub Label 가이드 작성 (`type:`, `area:`, `priority:`, `epic:`)

## 비스코프 (Don't)

- GitHub Projects board 도입 — 솔로 단계 over-engineering. 협업자 합류 시 재검토.
- Issue 자동 라벨러 (.github/workflows/) — 수동 운영으로 시작.
- 기존 docs/plans/_archived/ 마이그레이션 — 과거 plan 은 그대로 보존, 신규부터 적용.

## 참고

- Epic Issue: (Issue 생성 후 번호 채움)
- 합의 대화: 2026-05-05 ~ 2026-05-06 brainstorming 세션
- 관련 룰: `.claude/rules/plan-mode.md`, `.claude/rules/branch-pr.md`
