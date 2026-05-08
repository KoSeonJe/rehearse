# Implement (Backend) — RESUME 트랙 채점 perspective 별 라벨 라우팅 정상화

> **작성자**: backend agent
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` 응답 schema 합의 확인. 본 작업 = 기존 endpoint 응답 schema 에 `technicalFeedback.perspective` 필드 추가만.

- [ ] Endpoint 변경 없음 (기존 피드백 조회 endpoint 그대로)
- [ ] Response schema 추가: `technicalFeedback.perspective: "TECHNICAL"|"BEHAVIORAL"|"EXPERIENCE"|null`
- [ ] Error 코드 변경 없음
- [ ] 데이터 소스: `QuestionType.feedbackPerspective()` enum 메서드 (#447 V46 머지 후 단일 진실 원천)

미합의 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | TechnicalFeedback DTO + enum 매핑 + Domain Unit 테스트 | `backend` | #N | Phase 0 |

> Task 1개 / 단일 파일 유지 (분리 임계 미초과).

---

## Phase 1: TechnicalFeedback DTO 에 perspective 필드 추가 + 매핑 + 테스트

- **구현**: `backend` — TimestampFeedbackResponse DTO 확장 + `toTechnicalFeedback` 시그니처 변경 + enum 경유 매핑 + Domain Unit 테스트.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java`
  - `TechnicalFeedback` 클래스에 `private final String perspective` 필드 추가 (`@Getter @Builder` 유지)
  - `toTechnicalFeedback` 시그니처: `(QuestionScore, List<QuestionScoreDimension>)` → `(Question, QuestionScore, List<QuestionScoreDimension>)`
  - 빌더 호출 = `.perspective(question.getQuestionType().feedbackPerspective().name())` + `question == null` 가드
  - 호출부 `from()` 에서 `question` 변수 그대로 전달 (이미 `feedback.getQuestion()` 추출 완료 — 추가 fetch 없음)
- `backend/src/test/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponseTest.java`
  - perspective 매핑 케이스 추가: RESUME_PLAYGROUND → "EXPERIENCE", RESUME_INTERROGATION → "TECHNICAL", question=null 가드 → null

### 시그니처 결정 — Question 파라미터 추가

근거:
- `QuestionScore` 가 `Long questionId` 만 보유. JPA 연관 매핑 없음 → `qs → question` 참조 활용 불가
- `from()` 진입부 이미 `feedback.getQuestion()` 영속 객체 추출 중 → 재호출 불필요, N+1 추가 fetch 없음
- 인터페이스 정직성 ↑ (의존 명시)

### 핵심 로직

```java
// TechnicalFeedback (Builder 유지)
@Getter @Builder
public static class TechnicalFeedback {
    private final String perspective;   // 신규
    private final String rubricId;
    private final String levelFlag;
    private final List<TechnicalDimensionFeedback> dimensions;
}

// toTechnicalFeedback — 시그니처 변경 + enum 경유
private static TechnicalFeedback toTechnicalFeedback(
        Question question,
        QuestionScore questionScore,
        List<QuestionScoreDimension> dimensions) {
    if (questionScore == null || dimensions == null || dimensions.isEmpty()) {
        return null;
    }
    String perspective = (question != null && question.getQuestionType() != null)
            ? question.getQuestionType().feedbackPerspective().name()
            : null;

    List<TechnicalDimensionFeedback> dimFeedbacks = dimensions.stream()
            .sorted(Comparator.comparing(QuestionScoreDimension::getDimensionRef))
            .map(d -> TechnicalDimensionFeedback.builder()
                    .dimension(d.getDimensionRef())
                    .score(d.getScore())
                    .observation(d.getObservation())
                    .evidenceQuote(d.getEvidenceQuote())
                    .build())
            .toList();

    return TechnicalFeedback.builder()
            .perspective(perspective)   // 신규
            .rubricId(questionScore.getRubricId())
            .levelFlag(questionScore.getLevelFlag())
            .dimensions(dimFeedbacks)
            .build();
}

// 호출부 (from)
.technicalFeedback(toTechnicalFeedback(question, questionScore, dimensions))
```

### 의존

- 선행: Phase 0 (contract 합의)
- 외부: 없음

### Verification

- `./gradlew test --tests "TimestampFeedbackResponseTest"`
- Domain Unit: RESUME_PLAYGROUND question → `TechnicalFeedback.perspective == "EXPERIENCE"`
- Domain Unit: RESUME_INTERROGATION question → `TechnicalFeedback.perspective == "TECHNICAL"`
- Domain Unit: question == null 가드 → `TechnicalFeedback.perspective == null`
- `./gradlew test --tests "QuestionSetFeedbackResponse*"` — 응답 schema 회귀
- `./gradlew build`

### 커밋 메시지

```
feat(BE): 채점 perspective 응답 노출로 RESUME 라벨 분기 지원
```

---

## FE 와 통합 시점

- BE prod 배포 + 응답에 `perspective` 필드 노출 확인 → FE 머지 (tech-spec backward compat 룰).
- BE 머지 직후 FE 측에 알림 (Issue #436 댓글 또는 Slack).

## 통합 Verification

- [ ] tech-spec.md Verification 통과
- [ ] FE 통합 후 회귀 체크 (STANDARD 트랙 "기술 피드백" 영역 정상)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE+FE 동시 작업 → `code-reviewer-frontend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md Pre/Post 섹션)
