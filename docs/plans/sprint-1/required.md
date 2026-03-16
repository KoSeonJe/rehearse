# Sprint 1: 핵심 품질 개선 — 전체 요구사항

## Why

MVP 기능은 완성되었으나 실사용 테스트에서 18개 이슈가 발견됨.
영상 파이프라인 불안정, AI 질문 품질 미흡, 비언어 분석 미작동 등 핵심 가치에 직접 영향을 미치는 문제들이므로 런칭 전 반드시 해결 필요.

---

## 이슈 매트릭스

| # | 이슈 | 타입 | 스코프 | 태스크 | 우선순위 |
|---|------|------|--------|--------|----------|
| #64 | 영상 파이프라인 시각화 문서 | docs | - | Task 1 | P0 |
| #60 | 영상 녹화/저장/재생 파이프라인 전면 재구축 | enhancement | FE | Task 1 | P0 |
| #57 | 답변 시에만 녹화 → 전체 면접 연속 녹화 전환 | enhancement | FE | Task 1 | P0 |
| #56 | 타임스탬프 피드백이 실제 영상과 불일치 | bug | FE/BE | Task 1 | P0 |
| #55 | 영상과 음성 싱크 불일치 | bug | FE | Task 1 | P0 |
| #54 | 자기소개와 질문1 답변이 함께 저장 | bug | FE | Task 1 | P0 |
| #53 | 피드백 페이지 영상 초기 위치 ≠ 0초 | bug | FE | Task 1 | P0 |
| #49 | AI 면접 질문 품질 개선 | enhancement | BE | Task 2 | P0 |
| #48 | 후속질문이 작동하지 않음 | bug | FE/BE | Task 2 | P0 |
| #50 | 면접 시간 기반 질문 수 동적 생성 | enhancement | BE | Task 2 | P0 |
| #51 | 면접 중간 종료 버튼 UX 개선 | enhancement | FE | Task 3 | P1 |
| #52 | 면접 질문 생성 중 로딩 UX 개선 | enhancement | FE | Task 3 | P1 |
| #59 | 비언어적 피드백이 생성되지 않음 | bug | FE/BE | Task 4 | P1 |
| #58 | 질문별 모범 답변 및 학습 자료 제공 | enhancement | BE | Task 4 | P1 |
| #62 | 종합 리포트 정보 보강 | enhancement | BE | Task 5 | P2 |
| #63 | 점수 세부 기준 및 영역별 점수 체계 | enhancement | FE/BE | Task 5 | P2 |
| #61 | 종합 리포트 페이지 UI/UX 개선 | enhancement | FE | Task 5 | P2 |
| #47 | 음성인식 정확도 개선 (AI STT 전환) | enhancement | FE | Task 6 | P2 |

---

## 태스크 그룹 (6개)

### Task 1: 영상 파이프라인 재구축 (P0)
- **Issues**: #64, #60, #57, #56, #55, #54, #53
- **근거**: 답변별 분리 녹화가 5+ 버그의 근본 원인. 개별 버그 픽스 불가.
- **PRs**: BE 1개 (타임스탬프 검증) + FE 1개 (파이프라인 재작성) + docs 1개
- **상세 계획**: `plan-1-video-pipeline.md`

### Task 2: AI 질문 품질 + 후속질문 (P0)
- **Issues**: #49, #48, #50
- **PRs**: BE 1개 (프롬프트 튜닝 + 후속질문 수정 + 동적 질문수) + FE 1개 (후속질문 UI 수정)
- **상세 계획**: `plan-2-ai-quality.md`

### Task 3: 면접 페이지 UX (P1)
- **Issues**: #51, #52
- **PRs**: FE 1개
- **상세 계획**: `plan-3-interview-ux.md`

### Task 4: 피드백/분석 강화 (P1)
- **Issues**: #59, #58
- **PRs**: BE 1개 (모범답변 API) + FE 1개 (비언어 파이프라인 수정)
- **상세 계획**: `plan-4-feedback.md`

### Task 5: 종합 리포트 강화 (P2)
- **Issues**: #62, #63, #61
- **PRs**: BE 1개 (리포트 보강 + 영역별 점수) + FE 1개 (리포트 UI)
- **상세 계획**: `plan-5-report.md`

### Task 6: STT 전환 (P2)
- **Issues**: #47
- **PRs**: FE 1개 (+ BE 1개 서버사이드 STT 선택 시)
- **상세 계획**: `plan-6-stt.md`

**총 PR 예상: ~11개 (BE 4~5 + FE 6 + docs 1)**

---

## 의존성 그래프

```
Task 1 (영상 파이프라인) ──→ Task 4 (피드백 강화) ──→ Task 5 (리포트)
                         ──→ Task 6 (STT 전환)
Task 2 (AI 질문)         ──→ Task 6 (STT 전환)
Task 3 (면접 UX)         ──→ (독립)
```

### 실행 위상

| Phase | 태스크 | 실행 방식 | 선행 조건 |
|-------|--------|-----------|-----------|
| A | Task 1 + Task 2 + Task 3 | parallel | 없음 |
| B | Task 4 + Task 6 | parallel | Task 1, 2 완료 |
| C | Task 5 | sequential | Task 4 완료 |

---

## 성공 기준

1. 18개 이슈 모두 PR로 커버됨
2. 모든 PR이 develop 브랜치 기반, BE/FE 분리
3. 각 PR에 테스트 계획 포함
4. progress.md에 최종 상태 반영
