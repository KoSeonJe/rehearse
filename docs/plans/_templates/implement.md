# Implement — {제목}

> **작성자**: 구현 agent
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 작업 (BE 만 / FE 만 / lambda 만). BE+FE 동시 작업이면 `implement-be.md` + `implement-fe.md` 사용.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 예상 PR | 의존 |
|-------|------|--------|------|
| 1 | 기반 셋업 | #N | - |
| 2 | 핵심 로직 | #N+1 | Phase 1 |
| 3 | 통합 / 테스트 | #N+2 | Phase 2 |

> Task 8개+ 또는 단일 Phase 50줄+ → `tasks/{NN}-{slug}.md` 분리. 본 파일은 목록 + 링크.

---

## Phase 1: {제목}

### 변경 파일
- `path/to/File.java` — 무엇을 / 왜
- `path/to/Other.tsx` — 무엇을 / 왜

### 핵심 로직 / 변경 요약
- 단계별 요약. 코드 스케치 (의사코드 OK).

### 의존
- 선행 phase: (없음 / Phase X)
- 외부 의존: (BE API / 라이브러리 / 인프라)

### Verification Hook
- 명령: `./gradlew test --tests XxxTest`
- 통과 기준: 모든 테스트 green
- 관찰 가능 동작: (수동 검증 시나리오)

### 커밋 메시지 (예상)
```
feat(BE): xxx 기능의 yyy 추가
```

---

## Phase 2: {제목}

(동일 구조)

---

## Phase 3: {제목}

(동일 구조)

---

## 통합 Verification

전체 작업 완료 판정. tech-spec.md Verification 섹션과 동일하면 "tech-spec.md 참조" 1줄로 충분.

- [ ] tech-spec.md Verification 항목 모두 통과
- [ ] 추가 회귀 체크: (있을 시)

## 리뷰 게이트

- [ ] 지정 리뷰어 병렬 실행 (`code-reviewer-backend` / `code-reviewer-frontend`)
- [ ] 컨벤션 위반 0건
- [ ] Pre/Post State diff 일치
