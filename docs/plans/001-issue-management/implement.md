# Implement — Issue 기반 Spec-Driven 작업 시스템

> 단일 영역 (docs / 룰 정의). BE/FE 코드 변경 없음.

---

## Phase / Step 개요

| Phase | 제목 | 의존 |
|-------|------|------|
| 1 | docs/plans/AGENTS.md 작성 | - |
| 2 | _templates/ 7종 작성 | Phase 1 |
| 3 | 001-issue-management dogfood | Phase 2 |
| 4 | 루트 AGENTS.md 갱신 | Phase 3 |
| 5 | docs/todo 제거 + .gitignore 정리 | Phase 4 |
| 6 | (선택) GitHub Label 셋업 명령 문서화 | Phase 5 |

---

## Phase 1: docs/plans/AGENTS.md 작성

### 변경 파일
- `docs/plans/AGENTS.md` (신규)

### 핵심 로직
8 섹션:
1. 폴더 생성 트리거 (spec 1줄+ 만 폴더)
2. 명명 규약 (`{N}-{slug}`)
3. 파일 역할 표
4. 작성 워크플로우 + 승인 게이트
5. BE/FE 분리 룰 (contract-first 병렬)
6. 분리 임계 (Task 8개+ / 단일 50줄+)
7. handoff.md 룰
8. 종료 / 보존

### Verification
- [ ] 모든 섹션 존재
- [ ] 안티 패턴 명시

### 커밋 메시지
```
docs(infra): docs/plans 운영 룰 (AGENTS.md) 추가
```

---

## Phase 2: _templates/ 7종 작성

### 변경 파일
- `docs/plans/_templates/product-spec.md`
- `docs/plans/_templates/tech-spec.md`
- `docs/plans/_templates/implement.md`
- `docs/plans/_templates/implement-be.md`
- `docs/plans/_templates/implement-fe.md`
- `docs/plans/_templates/handoff.md`
- `docs/plans/_templates/task.md`

### 핵심 로직
각 템플릿 = 필수 섹션 + 채우기 가이드. 사용 시점 명시 (frontmatter 형태).

### Verification
- [ ] 7개 파일 존재
- [ ] 각 파일 사용 시점 명시
- [ ] tech-spec → implement 승인 게이트 명시

### 커밋 메시지
```
docs(infra): docs/plans/_templates 7종 추가
```

---

## Phase 3: 001-issue-management dogfood

### 변경 파일
- `docs/plans/001-issue-management/product-spec.md`
- `docs/plans/001-issue-management/tech-spec.md`
- `docs/plans/001-issue-management/implement.md` (이 파일)

### 핵심 로직
이번 결정 자체를 새 룰 따라 spec 화. 향후 reference.

### Verification
- [ ] 3개 파일 존재
- [ ] 새 템플릿 구조 따름
- [ ] Issue 생성 후 폴더명 rename (`001-` → 실제 번호)

### 커밋 메시지
```
docs(infra): issue 기반 spec-driven 시스템 dogfood plan 추가
```

---

## Phase 4: 루트 AGENTS.md 갱신

### 변경 파일
- `AGENTS.md` (Spec-Driven Work 섹션)

### 핵심 로직
- 기존 폴더 구조 (`{date}-{topic}/{product_spec,tech_spec}/`) → 새 구조 (`{N}-{slug}/`)
- 새 파일 (handoff, implement-be/fe, tasks/) 명시
- "docs/plans/AGENTS.md 참조" 1줄로 위임

### Verification
- [ ] 새 구조 명시
- [ ] handoff 룰 명시
- [ ] 상세는 docs/plans/AGENTS.md 참조 1줄

### 커밋 메시지
```
docs(infra): 루트 AGENTS.md spec-driven 섹션 새 룰 반영
```

---

## Phase 5: docs/todo 제거 + .gitignore 정리

### 변경 파일
- `docs/todo/` 전체 삭제 (`AGENTS.md` + `2026-05-04/`)
- `.gitignore` — `docs/todo/**` 관련 룰 3줄 제거

### 핵심 로직
- 핸드오프 역할 = `docs/plans/{N}-{slug}/handoff.md` 가 흡수
- AGENTS.md = `docs/plans/AGENTS.md` 가 흡수

### Verification
- [ ] `docs/todo/` 폴더 부재
- [ ] `.gitignore` 의 todo 관련 룰 제거됨
- [ ] 누구도 `docs/todo` 참조 안 함 (`grep -r "docs/todo"` 결과 0)

### 커밋 메시지
```
chore(infra): docs/todo 제거 (handoff.md 로 흡수)
```

---

## Phase 6: (선택) GitHub Label 셋업

### 변경 파일
- `.github/labels.yml` 또는 `scripts/setup-labels.sh` (선택)

### 핵심 로직
```bash
gh label create "type:bug" --color "d73a4a"
gh label create "type:feat" --color "0e8a16"
# ...
```

### Verification
- [ ] 16개 label 정의 (type×6 + area×4 + priority×3 + epic 메타×1)

### 커밋 메시지
```
chore(infra): GitHub Issue label 셋업 스크립트 추가
```

> Phase 6 = 선택. 첫 Epic Issue 생성 시점에 같이 진행도 가능.

---

## 통합 Verification

- [ ] tech-spec.md Verification 항목 모두 통과
- [ ] 향후 신규 plan 작성 시 새 룰 따름 확인
- [ ] Claude 세션 종료 시 handoff.md 작성됨

## 리뷰 게이트

- 코드 변경 없음 = 자동화 리뷰 X
- docs 일관성 / 룰 모호성 = 사용자 직접 검토
