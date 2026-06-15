# docs/plans — Spec-Driven Work 운영 룰

이 폴더는 GitHub Issue 기반 작업의 **영구 spec 저장소**. 모든 작업의 "왜 / 어떻게 / 어떤 순서" 가 여기에 산다.

> 루트 `AGENTS.md`, `CLAUDE.md` 와 함께 적용. 충돌 시 이 파일 우선 (이 폴더 한정).

---

## 1. 폴더 생성 트리거

| 작업 규모 | spec 폴더 | 설명 |
|----------|----------|------|
| 1줄 bug / chore | ❌ | Issue body 만으로 충분 |
| spec 1줄 이상 필요 | ✅ | product-spec.md 부터 작성 |

**판단 기준**: "이 작업 product-spec 1줄 이상 적을 게 있나?" Yes → 폴더 생성.

---

## 2. 명명 규약

```
docs/plans/{Issue번호}-{slug}/
```

- `{Issue번호}` = Epic Issue 번호 (3자리 권장: `042`)
- `{slug}` = kebab-case 영문, 30자 이하, 의미 압축
- 예: `042-interview-quality`, `087-resume-section-rewrite`

Issue 없는 spec 작업 (탐색/리서치) = 번호 자리에 `00x` 임시 번호 → Issue 만든 직후 rename.

---

## 3. 파일 역할

| 파일 | 답하는 질문 | 필수 | 작성자 |
|------|----------|------|-------|
| `product-spec.md` | 왜? 무엇? 수용기준? | ✅ | 사용자 |
| `tech-spec.md` | 구조 / API / 데이터 / Trade-off / 검증 기준 | ✅ | 구현 agent |
| `implement.md` | 단일 영역 실행 순서 | △ (BE+FE 분리 시 X) | 구현 agent |
| `implement-be.md` | BE 실행 순서 | BE+FE 작업 시 ✅ | backend agent |
| `implement-fe.md` | FE 실행 순서 | BE+FE 작업 시 ✅ | frontend agent |
| `tasks/{NN}-{slug}.md` | 단일 Task 상세 | 임계 초과 시 | 구현 agent |
| `handoff.md` | 다음 세션 어디부터? | 진행 중에만 | Claude (세션 종료 시) |
| `progress.md` | 진행 narrative | 옵션 | docs-manager |

---

## 4. 작성 워크플로우

```
[사용자] product-spec.md 작성
   ↓
[구현 agent] tech-spec.md 작성 (API contract 포함)
   ↓ ★ 사용자 명시 승인 ★
[구현 agent] implement.md (또는 implement-be/-fe.md)
   ↓ ★ 사용자 명시 승인 ★
구현 → PR → Sub-Issue close
   ↓ (Epic 모든 Sub-Issue close 시)
Epic Issue close → handoff.md 제거
```

**승인 게이트**:
- tech-spec → implement 사이 1차 승인
- implement → 코드 작성 사이 2차 승인
- 자율 진행 금지. 모호하면 즉시 사용자 질문.

---

## 5. BE/FE 분리 룰

### 단일 영역 작업
`implement.md` 1개. 끝.

### BE+FE 동시 작업
- `tech-spec.md` 안에 **API contract** 명시 (요청/응답 schema, 엔드포인트, 상태코드)
- `implement-be.md` / `implement-fe.md` 분리
- ★ contract 합의 = 사용자 승인 게이트 ★ → 그 후 BE/FE **병렬 시작 가능**
- FE 는 mock (MSW / hardcoded fixture) 로 진행. BE 머지 후 mock 제거 → 통합

### 강결합 (BE 선행 강제)
- DB 마이그레이션 backfill / 이벤트 페이로드 변경 등
- BE 머지 완료 → FE 시작
- tech-spec.md 에 "BE 선행 필수" 명시.

---

## 6. 분리 임계 (하이브리드)

`implement*.md` 단일 파일 vs `tasks/` 폴더 분리:

**분리 트리거 (둘 중 하나)**:
- Task 8개+
- 단일 Task 본문 50줄+ (코드블록 / 시퀀스 다이어그램 포함 시)

**분리 시**:
- `implement-be.md` = Task 목록 (제목 + 1줄 요약 + `tasks/be-NN-x.md` 링크) + 의존관계 + Verification 요약
- `tasks/be-{NN}-{slug}.md` = Task 상세 (변경 파일 / 핵심 로직 / 테스트 / 완료기준)
- `tasks/fe-{NN}-{slug}.md` 동일

작은 작업은 단일 파일 유지. 미리 분리 X.

---

## 7. handoff.md 룰

### 작성 시점
- 세션 종료 직전 (사용자 "끝" / "종료" / 명시 요청)
- 컨텍스트 30-40% 남은 시점 (자율 작성)

### 다음 세션
- plan 폴더 진입 시 **handoff.md 먼저 읽음**. 무조건.
- 없으면 product-spec → tech-spec → implement 순.

### 종료
- Epic Issue close = plan 폴더 freeze
- handoff.md 만 삭제. 다른 파일 보존 (히스토리).

### 금지
- 진행 중 매 step 업데이트 X. 세션 경계만 갱신.
- progress.md 와 혼동 금지: progress = 영구 narrative, handoff = 단명 컨텍스트 덤프.

---

## 8. 종료 / 보존

- Epic close → 폴더 freeze (수정 X). 히스토리 보존.
- `handoff.md` 만 제거.
- 폴더 자체 삭제 금지. 후속 작업 참조 자료.
- 큰 변경으로 spec 무효화 시 = 새 Issue + 새 폴더. 기존은 archive.

---

## 9. 템플릿

`_templates/` 하위 7종. 새 plan 생성 시 복사 → 채우기.

```bash
PLAN_DIR=docs/plans/042-interview-quality
mkdir -p "$PLAN_DIR"
cp docs/plans/_templates/product-spec.md "$PLAN_DIR/"
cp docs/plans/_templates/tech-spec.md "$PLAN_DIR/"
# 이하 필요 파일만
```

---

## 10. 안티 패턴

- 작은 bug/chore 인데 폴더 생성 (Issue body 만으로 충분)
- product-spec 없이 tech-spec 시작 (WHY 누락)
- tech-spec 승인 없이 implement 시작
- implement 승인 없이 코드 작성
- handoff.md 매 step 업데이트 (세션 경계만)
- BE/FE contract 합의 없이 병렬 시작
- spec 폴더에 단명 파일 (스크래치 / 메모) — handoff.md 외 단명 파일 X
