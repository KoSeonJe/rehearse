# Sprint 1: 핵심 품질 개선 — 전체 요구사항

## Why

MVP 기능은 완성되었으나 실사용 테스트에서 이슈가 발견됨.
AI 질문 품질 미흡, 면접 UX, 리포트 빈약 등 핵심 가치에 직접 영향을 미치는 문제들이므로 런칭 전 반드시 해결 필요.

> Sprint 0에서 녹화-분석-피드백 파이프라인을 전면 재설계했으므로, 영상 파이프라인(구 Task 1), STT(구 Task 6), 비언어 분석(구 Task 4 일부)은 Sprint 0으로 이관됨.

---

## 이슈 매트릭스

| # | 이슈 | 타입 | 스코프 | 태스크 | 우선순위 |
|---|------|------|--------|--------|----------|
| #49 | AI 면접 질문 품질 개선 | enhancement | BE | Task 1 | P0 |
| #48 | 후속질문이 작동하지 않음 | bug | FE/BE | Task 1 | P0 |
| #50 | 면접 시간 기반 질문 수 동적 생성 | enhancement | BE | Task 1 | P0 |
| #51 | 면접 중간 종료 버튼 UX 개선 | enhancement | FE | Task 2 | P1 |
| #52 | 면접 질문 생성 중 로딩 UX 개선 | enhancement | FE | Task 2 | P1 |
| #58 | 질문별 모범 답변 및 학습 자료 제공 | enhancement | BE/FE | Task 3 | P1 |
| #62 | 종합 리포트 정보 보강 | enhancement | BE | Task 4 | P2 |
| #63 | 점수 세부 기준 및 영역별 점수 체계 | enhancement | FE/BE | Task 4 | P2 |
| #61 | 종합 리포트 페이지 UI/UX 개선 | enhancement | FE | Task 4 | P2 |

### Sprint 0으로 이관된 이슈 (Sprint 1에서 제거)

| # | 이슈 | 이관 사유 |
|---|------|----------|
| #64, #60, #57, #56, #55, #54, #53 | 영상 파이프라인 관련 7건 | Sprint 0 Task 8~10 (질문세트 녹화 + S3 파이프라인)으로 근본 해결 |
| #47 | STT 전환 | Sprint 0 Task 6 (분석 Lambda — Whisper)으로 서버사이드 해결 |
| #59 | 비언어 피드백 미생성 | Sprint 0 Task 6 (분석 Lambda — Vision)으로 서버사이드 해결 |

---

## 태스크 그룹 (4개)

### Task 1: AI 질문 품질 + 후속질문 (P0)
- **Issues**: #49, #48, #50
- **PRs**: BE 1개 (프롬프트 튜닝 + 후속질문 + 동적 질문수) + FE 1개 (후속질문 UI 수정)
- **상세 계획**: `plan-1-ai-quality.md`

### Task 2: 면접 페이지 UX (P1)
- **Issues**: #51, #52
- **PRs**: FE 1개
- **상세 계획**: `plan-2-interview-ux.md`

### Task 3: 모범 답변 제공 (P1)
- **Issues**: #58
- **PRs**: BE 1개 (모범 답변 API) + FE 1개 (모범 답변 UI)
- **상세 계획**: `plan-3-model-answer.md`

### Task 4: 종합 리포트 강화 (P2)
- **Issues**: #62, #63, #61
- **PRs**: BE 1개 (리포트 보강 + 영역별 점수) + FE 1개 (리포트 UI)
- **상세 계획**: `plan-4-report.md`

**총 PR 예상: ~7개 (BE 3 + FE 4)**

---

## 의존성 그래프

```
Task 1 (AI 질문)     ──→ (독립)
Task 2 (면접 UX)     ──→ (독립)
Task 3 (모범 답변)   ──→ Sprint 0 완료 후
Task 4 (리포트)      ──→ Task 3 완료 후
```

### 실행 위상

| Phase | 태스크 | 실행 방식 | 선행 조건 |
|-------|--------|-----------|-----------|
| A | Task 1 + Task 2 | parallel | 없음 |
| B | Task 3 | sequential | Sprint 0 완료 |
| C | Task 4 | sequential | Task 3 완료 |

---

## 성공 기준

1. 9개 이슈 모두 PR로 커버됨
2. 모든 PR이 develop 브랜치 기반, BE/FE 분리
3. 각 PR에 테스트 계획 포함
4. progress.md에 최종 상태 반영
