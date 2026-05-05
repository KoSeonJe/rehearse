# Tech Spec — Issue 기반 Spec-Driven 작업 시스템

---

## Why → Goal (1줄)

GitHub Issue 일원화 + `docs/plans/{N}-{slug}/` 1:1 매핑 + handoff.md 세션 인계 = 솔로 단계 추적성 / 일관성 / 컨텍스트 보존 모두 달성.

## Evidence

- 현재 구조:
  - `docs/plans/{date-topic}/{product_spec,tech_spec}/` (snake_case 폴더, design.md / progress.md)
  - `docs/todo/{date}/` 일자별 메모 (gitignored, AGENTS.md 만 tracked)
  - `.claude/rules/plan-mode.md` 플랜 작성 룰
  - `.claude/rules/branch-pr.md` PR 룰
- 사용자 발화 (결정 근거):
  - "GitHub Project board 솔로면 굳이 필요없겠지?" → 도입 안 함
  - "큰 주제 아래 type 으로 나누는게 좋을 것 같다" → epic + type 라벨
  - "tech spec = 구현 계획 plan.md 맞나?" → tech-spec 이 plan 본문, 그 안 Tasks 가 Issue
  - "implement.md (구현 순서) 분리, BE/FE 의존시 implement-be / implement-fe" → 채택
  - "BE 머지 후 FE 시작?" → No, contract-first 병렬 채택
  - "handoff.md 추가" → 채택
- 추정:
  - 솔로 → 협업 전환 시점에 Projects board 추가 비용 낮음 (Issue 그대로 흡수). 지금 도입 시 운영 부담만.

## Trade-offs

### Option A — Issue + plans 폴더 1:1 (채택)
- 장점: 도구 1개 (Issue). 추적 단위 명확. spec 영속화. PR 자동 연결.
- 단점: spec 폴더 / Issue / 라벨 동기화 룰 필요.
- 사유: 솔로 단계에서 추적성 + 영속성 모두 확보, 협업 전환 시 그대로 확장.

### Option B — BACKLOG.md 단일 파일 (폐기)
- 장점: 0 도구. 마크다운 1개.
- 단점: 검색 / 필터 / PR 추적 약함. 솔로여도 한달 후 본인이 못 찾음.
- 폐기 사유: 추적성 / 영속성 약함. 도입한 spec-driven 워크플로우와 분리됨.

### Option C — 풀 Epic-Milestone-Task-Subtask 4계층 (폐기)
- 장점: Jira 흉내. 큰 그림 시각화.
- 단점: 솔로 운영 부담 큼. label / 폴더 구조 복잡.
- 폐기 사유: 욕심. 운영 실패 위험.

### 분기 결정 — BE 선행 vs Contract-first 병렬

- BE 선행: 안전하지만 사이클 길어짐 (PR 2회 + 시간 2배)
- **Contract-first 병렬 (채택)**: tech-spec.md 에 API contract 합의 → BE/FE 병렬, FE 는 mock 진행 → BE 머지 후 mock 제거
  - 사유: 솔로여도 작업 사이클 단축. 강결합 (DB 마이그레이션) 만 BE 선행 강제.

## Architecture

```
GitHub Issue (Epic)
    ↓ 1:1
docs/plans/{N}-{slug}/
    ├── product-spec.md    ← 사용자: WHY / WHAT
    ├── tech-spec.md       ← agent: HOW (구조 / contract)
    │       ↓ 승인
    ├── implement.md       ← agent: 실행 순서 (단일 영역)
    │   또는
    ├── implement-be.md    ← BE 실행 (Phase 0 = contract 확인)
    ├── implement-fe.md    ← FE 실행 (Phase 0 = contract + mock)
    │
    ├── tasks/             ← (옵션) Task 8개+ 시 분리
    │   ├── be-01-xxx.md
    │   └── fe-01-xxx.md
    │
    └── handoff.md         ← 단명. 세션 인계 컨텍스트
        ↓ 1:N
GitHub Issue (Sub-Tasks, label: epic:X)
        ↓ 1:1
PR (Closes #N)
```

## Data Model

해당없음 (코드 변경 X). 파일 / 폴더 구조만 변경.

## API Contract

해당없음 (BE+FE 코드 작업 X).

## File Migration

| Before | After | 처리 |
|--------|-------|------|
| `docs/plans/{date}-{topic}/product_spec/requirements.md` | `docs/plans/{N}-{slug}/product-spec.md` | 신규부터 적용. 기존 보존. |
| `docs/plans/{date}-{topic}/tech_spec/design.md` | `docs/plans/{N}-{slug}/tech-spec.md` + `implement*.md` | 분할. 신규부터. |
| `docs/plans/{date}-{topic}/tech_spec/progress.md` | `progress.md` (옵션) 또는 `handoff.md` | 단명 = handoff, 영구 = progress |
| `docs/todo/{date}/*.md` | `docs/plans/{N}-{slug}/handoff.md` | 폴더 자체 제거. 핸드오프는 plan 폴더 안. |
| `docs/todo/AGENTS.md` | 삭제 | docs/plans/AGENTS.md 가 흡수 |

## GitHub Label 정의

| Label | 값 | 색상 (예시) |
|-------|-----|-----------|
| `type:` | bug, feat, refactor, chore, tech-debt, docs | bug=red / feat=green |
| `area:` | BE, FE, lambda, infra | BE=blue / FE=cyan |
| `priority:` | P0, P1, P2 | P0=darkred |
| `epic:` | interview-quality, design-overhaul, ... | purple |

운영: Issue 생성 시 `type` + `area` + `priority` 필수. `epic` 은 큰 작업만.

## Verification

- [ ] `docs/plans/AGENTS.md` 8 섹션 모두 작성됨
- [ ] `_templates/` 7개 파일 존재 + 각 필수 섹션 보유
- [ ] 본 plan (001-issue-management) 이 새 룰 따라 dogfood 됨
- [ ] 루트 `AGENTS.md` Spec-Driven 섹션 갱신됨
- [ ] `docs/todo/` 제거됨
- [ ] `.gitignore` 의 `docs/todo/**` 룰 제거됨
- [ ] (선택) GitHub Label 셋업 명령 문서화 (`gh label create ...`)

## Pre / Post State

### Pre
```
docs/
├── todo/
│   ├── AGENTS.md
│   └── 2026-05-04/
└── plans/
    └── (gitignored, _archived/ 외 비어있음)
```
- 폴더 명명: `{date}-{topic}/{product_spec,tech_spec}/`
- 핸드오프: docs/todo/{date}/

### Post
```
docs/
└── plans/
    ├── AGENTS.md             ← 운영 룰 (신규)
    ├── _templates/           ← 7종 템플릿 (신규)
    │   ├── product-spec.md
    │   ├── tech-spec.md
    │   ├── implement.md
    │   ├── implement-be.md
    │   ├── implement-fe.md
    │   ├── handoff.md
    │   └── task.md
    ├── 001-issue-management/ ← dogfood
    │   ├── product-spec.md
    │   ├── tech-spec.md
    │   ├── implement.md
    │   └── handoff.md (작업 중일 때만)
    └── (이후 신규 plan 들...)
```
- 폴더 명명: `{N}-{slug}/`
- 핸드오프: 각 plan 폴더 내 handoff.md
- todo/ 폴더 제거됨

## 위험 / 마이그레이션 / 롤백

- 위험: 기존 진행중 plan 의 사용자 / agent 가 새 룰과 혼동
  - 완화: 신규 plan 만 새 룰 적용. 기존은 그대로. AGENTS.md 에 명시.
- 위험: handoff.md 작성 습관 미형성 → 컨텍스트 손실
  - 완화: 루트 CLAUDE.md / AGENTS.md 에 "세션 종료 시 handoff 필수" 명시.
- 롤백: 매우 단순. 모든 변경이 docs / 룰 텍스트. 코드 영향 0. 필요시 commit revert.

## 분기 결정

- [x] **단일 영역 → `implement.md` 1개** (이번 작업은 docs / 룰 정의. BE/FE 코드 변경 없음)
- [ ] BE+FE 동시
- [ ] BE 선행 강제
