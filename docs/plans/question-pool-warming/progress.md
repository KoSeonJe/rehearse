# Question Pool 데이터 워밍 — 진행 상황

## 태스크 상태

| # | 태스크 | 플랜 | 상태 | 태그 | 비고 |
|---|--------|------|------|------|------|
| 1 | QuestionPool 불필요 필드 삭제 | plan-06-cleanup-unused-fields.md | Draft | [blocking] | V20 마이그레이션, 선행 필수 |
| 2 | 사용자별 중복 방지 로직 | plan-01-dedup-logic.md | Draft | [blocking] | Task 1 완료 후 |
| 3 | CS 기본 시드 데이터 (360개) | plan-02-cs-seed.md | Draft | [parallel] | Task 1 완료 후, Task 2와 병렬 |
| 4 | Behavioral + System Design 시드 (180개) | plan-03-behavioral-systemdesign-seed.md | Draft | [parallel] | Task 3과 병렬 |
| 5 | 프레임워크/기술 스택 시드 (810개) | plan-04-framework-seed.md | Draft | [parallel] | Task 3, 4와 병렬 |
| 6 | 기존 시드 파일 정리 + README | plan-05-seed-cleanup.md | Draft | | Task 3, 4 완료 후 |

## 실행 순서

```
[Phase 0 - 선행] Task 1: QuestionPool 필드 정리 (V19 마이그레이션)

[Phase A - 병렬]
  Task 2: 중복 방지 로직 구현 (backend 에이전트)
  Task 3: CS 시드 SQL 작성 (backend 에이전트)
  Task 4: Behavioral/System Design 시드 SQL (backend 에이전트)
  Task 5: 프레임워크 시드 SQL (backend 에이전트)

[Phase B]
  Task 6: 기존 파일 정리 (Task 3, 4 완료 후)
```

## 총 질문 수 요약

| 구분 | 질문 수 | cacheKey 수 |
|------|---------|------------|
| CS 기본 (3레벨 × 120개) | 360 | 3 |
| Behavioral (3레벨 × 30개) | 90 | 3 |
| System Design (3레벨 × 30개) | 90 | 3 |
| 프레임워크 (10타입 × 3레벨 × 30개) | 810 | 30 |
| **총합** | **~1,350** | **39** |

---

## 품질 검증 태스크

| # | Phase | 플랜 | 대상 | 상태 | 세션 수 |
|---|-------|------|------|------|---------|
| 7 | Phase 0: P0 Hotfix | plan-07-quality-hotfix.md | 오류/오타/잘린 답변 3건 수정 | **Done** | 1 |
| 8 | Phase 1: CS Fundamental | plan-08-quality-cs.md | cs-fundamental-junior/mid/senior.sql (358문항) | Pending | 3 |
| 9 | Phase 2: Frontend | plan-09-quality-frontend.md | frontend-react-ts/vue-ts.sql (270문항) | Pending | 2 |
| 10 | Phase 3: DevOps | plan-10-quality-devops.md | devops-aws-k8s.sql (180문항) | Pending | 2 |
| 11 | Phase 4: Backend | plan-11-quality-backend.md | backend-java/kotlin/node/python.sql (360문항) | Pending | 2 |
| 12 | Phase 5: 기타 | plan-12-quality-others.md | fullstack/behavioral/system-design (270문항) | Pending | 2 |

### 실행 순서

```
Phase 0 (Task 7) ← 반드시 먼저
    ├──→ Phase 1 (Task 8)   [3 세션, 순차]
    ├──→ Phase 2 (Task 9)   [2 세션]
    ├──→ Phase 3 (Task 10)  [2 세션]
    ├──→ Phase 4 (Task 11)  [2 세션]
    └──→ Phase 5 (Task 12)  [2 세션]
```

Phase 0만 선행 필수. Phase 1~5는 독립 실행 가능. 각 Phase 후 컨텍스트 초기화.

---

## 진행 로그

### 2026-04-10
- 요구사항 정의 완료 (requirements.md)
- 웹 리서치 완료: CS 4분야 + 프레임워크 9스택 + Behavioral 5카테고리
- Plan 01~05 작성 완료
- 출처: InterviewBit, GeeksforGeeks, GitHub(gyoogle, JaeYeopHan), Velog, Guru99, Tech Interview Handbook, Amazon LP 등
