# Handoff — 436-feedback-perspective-label-routing

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 / 컨텍스트 잔여
> **다음 세션**: plan 폴더 진입 시 **이 파일 먼저 읽음**

---

## 현재 상태

- 진행: product-spec.md / tech-spec.md / implement-be.md / implement-fe.md 작성 완료 (사용자 승인). BE/FE 구현 완료. 리뷰 완료. **Blocker 로 인해 push/PR 보류 중.**
- 브랜치 (BE): `feat/436-be-perspective-routing` (마지막 commit `2de2f94`) — 로컬 보존, push 안 함
- 브랜치 (FE): `feat/436-fe-perspective-routing` (마지막 commit `0f18ebc`) — 로컬 보존, push 안 함
- 관련 PR: 없음 (Blocker 대기)
- 빌드: 미확인 (push 전)
- 테스트: 로컬 패스 (리뷰 시 이슈 없음)

## 다음 세션 시작점

- **첫 명령**: `git log develop --oneline -10` — 컬럼 제거 PR 머지 여부 확인
- Blocker 해소 확인 후:
  1. spec Evidence 갱신: `questionScore.feedback_perspective` 컬럼 → `QuestionType.feedbackPerspective()` enum 메서드 소스 변경
  2. tech-spec.md Architecture / Data Model 재작성 (DB 컬럼 조회 → enum 메서드 조회)
  3. implement-be.md 변경 파일 재추정: `TimestampFeedbackResponse.toTechnicalFeedback` 에서 `question.getQuestionType().feedbackPerspective().name()` 호출 (QuestionScore 가 Question 참조 보유 여부 먼저 확인)
  4. FE 작업물 그대로 유효 — P1 4건 반영 후 재커밋
- 예상 변경 파일 (BE 재설계):
  - `backend/src/main/java/.../TimestampFeedbackResponse.java` (또는 인접 DTO)
  - `QuestionType.java` — `feedbackPerspective()` 메서드 위치 확인

## 미해결 질문 / Blocker

- **[Blocker]** 다른 워크트리에서 `question` / `question_pool` / `question_score` 테이블의 `referencetype`, `feedback_perspective` 컬럼 제거 작업 진행 중. 본 plan 핵심인 `questionScore.getFeedbackPerspective()` (DB 컬럼) 이 사라질 예정. 컬럼 제거 PR 머지 후 BE 재설계 필요. **재진입 전 머지 여부 반드시 확인.**
- BE P2 (Service Integration OPENER null 케이스 — Domain Unit 과 중복) — 재설계 시 함께 정리
- FE P1-1: `perspective` union 타입 정의 추가
- FE P1-2 / P1-4: fallback 메시지 perspective 별 분기 처리
- FE P1-3: 테스트 RTL 변환
- mock fixture 추가 여부 — 활용처 부재로 생략 결정. spec 갱신 권장 (구현 agent 발견사항)

## 컨텍스트 메모

- **핵심 목적**: perspective 응답 schema 추가 + FE 라벨 분기. 데이터 소스(컬럼 vs enum)는 부차적 — QuestionType enum 으로 이전 가능하며 FE 작업물에 영향 없음
- **BE 우선 머지 필수 룰 유지**: FE 단독 머지 시 `perspective` undefined → 모든 turn fallback 회귀
- **FE 작업물 재사용 가능**: BE 데이터 소스 변경과 무관. 재설계 시 P1 4건만 반영 후 거의 그대로 사용
- **로컬 브랜치 보존 결정**: 재설계 시 참고용 (git checkout 으로 diff 확인)
- `QuestionType.feedbackPerspective()` 메서드는 이미 존재 — enum 위치: `find /Users/koseonje/dev/devlens/backend/src/main/java -name "QuestionType.java"`

## 참고 명령

```bash
# 컬럼 제거 PR 머지 확인
git log develop --oneline -10

# BE 작업물 확인
git checkout feat/436-be-perspective-routing && git diff develop

# FE 작업물 확인
git checkout feat/436-fe-perspective-routing && git diff develop

# QuestionType enum 위치
find /Users/koseonje/dev/devlens/backend/src/main/java -name "QuestionType.java"

# feedbackPerspective 현 사용처
grep -rn "feedbackPerspective" backend/src/main/java/
```

---

업데이트: 2026-05-08 (세션 종료, 컬럼 제거 PR 대기)
