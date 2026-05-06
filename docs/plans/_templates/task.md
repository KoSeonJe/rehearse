# Task {NN} — {제목}

> **사용 시점**: `implement*.md` 분리 임계 초과 시 (Task 8개+ / 단일 Task 50줄+)
> **위치**: `tasks/{be|fe|}-{NN}-{slug}.md`
> **답하는 질문**: 이 단일 Task 어떻게 구현?

---

## 목적

이 Task 가 무엇을 달성하는가. 1-2 문장.

## 에이전트

- **구현**: `{backend | frontend | general-purpose}` — {영역 책임 1줄}
- **리뷰**: `{code-reviewer-backend | code-reviewer-frontend}` — {리뷰 포커스 1줄}

## 변경 파일

- `path/to/File.java` — 무엇을 / 왜
- `path/to/Other.tsx` — 무엇을 / 왜
- `path/to/migration.sql` — DB 변경

## 핵심 로직 / 변경 요약

단계별 의사코드 또는 코드 스케치. 50줄 이상이면 코드블록 활용.

```
1. X 입력 받음
2. validation: ...
3. service.foo() 호출
4. 결과 매핑 + 응답
```

## 의존

- 선행 Task: (이전 Task NN / 없음)
- 외부: (라이브러리 / 인프라)

## 테스트 케이스

- [ ] 정상 케이스: ...
- [ ] 에러 케이스 A: ...
- [ ] 에러 케이스 B: ...
- [ ] 경계 케이스 (null / 빈값 / 최대값)

## 완료 기준

- [ ] 변경 파일 commit
- [ ] 테스트 모두 통과
- [ ] 린트 / 빌드 통과
- [ ] **지정 리뷰어 실행** (구현 직후, MANDATORY)
- [ ] Critical / Major 지적 fix 반영

## 커밋 메시지

```
feat(BE): xxx 기능 yyy 단계 구현
```

## 비고

위험 / 함정 / 향후 개선 항목 (TODO).
