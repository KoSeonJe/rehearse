# Task 01 — QuestionType enum 정리

> **위치**: `tasks/p2-be-01-question-type.md`
> **답하는 질문**: QuestionType enum 어떻게 정리?

---

## 목적

`RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 폐기 + `RESUME_MAIN` / `RESUME_FOLLOWUP` 신설. AC 3번 (question row 1개로 트랙 식별) 충족 기반.

## 에이전트

- **구현**: `backend` — domain/question/entity QuestionType enum + isMain/isFollowUp/isResume 분기 갱신
- **리뷰**: `code-reviewer-backend` — enum 변경 / 호출 grep / 회귀

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java` — 멤버 추가 (`RESUME_MAIN`, `RESUME_FOLLOWUP`) + 제거 (`RESUME_PLAYGROUND`, `RESUME_INTERROGATION`) + `isMain`/`isFollowUp`/`isResume` 분기 확장
- `backend/src/test/.../QuestionTypeTest.java` — 신규/제거 멤버 + 분기 메서드 회귀

## 핵심 로직

```java
public enum QuestionType {
    TECH_MAIN(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    TECH_FOLLOWUP(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    BEHAVIORAL_MAIN(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    BEHAVIORAL_FOLLOWUP(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    RESUME_OPENER(ReferenceType.GUIDE, RubricCategory.EXPERIENCE),
    RESUME_MAIN(ReferenceType.GUIDE, RubricCategory.TECHNICAL),       // NEW
    RESUME_FOLLOWUP(ReferenceType.GUIDE, RubricCategory.TECHNICAL);   // NEW

    public boolean isMain() {
        return this == TECH_MAIN || this == BEHAVIORAL_MAIN || this == RESUME_MAIN;
    }
    public boolean isFollowUp() {
        return this == TECH_FOLLOWUP || this == BEHAVIORAL_FOLLOWUP || this == RESUME_FOLLOWUP;
    }
    public boolean isResume() {
        return this == RESUME_OPENER || this == RESUME_MAIN || this == RESUME_FOLLOWUP;
    }
}
```

## 의존
- 선행 Task: 없음 (Phase 2 진입점)
- 외부: 없음

## 테스트 케이스
- [ ] `RESUME_MAIN.isMain()` = true, `.isFollowUp()` = false, `.isResume()` = true
- [ ] `RESUME_FOLLOWUP.isMain()` = false, `.isFollowUp()` = true, `.isResume()` = true
- [ ] `RESUME_OPENER.isMain()` = false, `.isFollowUp()` = false, `.isResume()` = true
- [ ] `QuestionType.valueOf("RESUME_PLAYGROUND")` → `IllegalArgumentException`
- [ ] `QuestionType.valueOf("RESUME_INTERROGATION")` → `IllegalArgumentException`
- [ ] 컴파일: 모든 `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 참조 제거 확인

## 완료 기준
- [ ] enum 멤버 변경 + 분기 메서드 회귀 통과
- [ ] grep `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 잔존 0 (소스 + 테스트 + 리소스)
- [ ] `./gradlew compileJava compileTestJava` 통과
- [ ] code-reviewer-backend 실행 (Phase 2 종료 후 통합 리뷰)

## 커밋 메시지

```
refactor(BE): QuestionType RESUME_MAIN/FOLLOWUP 신설 + PLAYGROUND/INTERROGATION 폐기
```

## 비고

본 task 후 `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 참조 클래스 컴파일 실패 발생. Task 06 (FollowUpService 분기 제거) / 09 (Resume FSM 폐기) 진행 시 자연 해소.
