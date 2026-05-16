# Task 02 — AnswerAnalysis 재설계 + TurnAnalysis 폐기

> **위치**: `tasks/p2-be-02-answer-analysis.md`
> **답하는 질문**: AnswerAnalysis 어떻게 재설계?

---

## 목적

부족점 표현 = Rubric dimension 단일 축으로 통일 (`dimensionGaps: Map<String, Integer>` + `weakestDimension`). `missingPerspectives` / `answerQuality` / `turnId` / `applyL1FalseNegativeGuard` 폐기. `TurnAnalysis` interface / `TurnAnalysisResult` record / `TurnAnalysisPipeline` 서비스 동시 폐기 — 다형성 사용 0.

## 에이전트

- **구현**: `backend` — AnswerAnalysis record 5 필드 재설계 + 관련 marker class 폐기 + AudioTurnAnalyzer 반환 타입 변경
- **리뷰**: `code-reviewer-backend` — record 불변성 / 시그니처 회귀 (FollowUpService / AudioTurnAnalyzer / TextFallbackTurnAnalyzer)

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java` — record 재정의 (5 필드)
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/TurnAnalysis.java` — 파일 삭제
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/TurnAnalysisResult.java` — 파일 삭제
- `backend/src/main/java/com/rehearse/api/domain/interview/service/TurnAnalysisPipeline.java` — 파일 삭제
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerFeedbackPerspective.java` — 파일 삭제 (enum 폐기)
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AskedPerspectives.java` — 파일 삭제 (record)
- `backend/src/main/java/com/rehearse/api/infra/ai/analyzer/AudioTurnAnalyzer.java` — `analyze` 반환 타입 `TurnAnalysisResult` → `AnswerAnalysis`. `recordAnalysis` 호출 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/analyzer/AnswerAnalyzer.java` — `applyL1FalseNegativeGuard` 호출 제거. `recordAnalysis` 호출 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/analyzer/TextFallbackTurnAnalyzer.java` — `analyze` 반환 타입 갱신
- `backend/src/test/.../AnswerAnalysisTest.java` — 신규 record 케이스
- `backend/src/test/.../AudioTurnAnalyzerTest.java` — 반환 타입 회귀

## 핵심 로직

```java
public record AnswerAnalysis(
        List<Claim> claims,
        Map<String, Integer> dimensionGaps,
        String weakestDimension,
        List<String> unstatedAssumptions,
        RecommendedNextAction recommendedNextAction
) {
    public AnswerAnalysis {
        claims = claims != null ? List.copyOf(claims) : List.of();
        dimensionGaps = dimensionGaps != null ? Map.copyOf(dimensionGaps) : Map.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public static AnswerAnalysis empty() {
        return new AnswerAnalysis(List.of(), Map.of(), null, List.of(), RecommendedNextAction.SKIP);
    }
}
```

- `dimensionGaps` 키 = `RubricDimension.id` (String). gap 0~3 (0=완전, 3=부재)
- `weakestDimension` = `dimensionGaps` 중 max gap dimension id. tie 시 LLM 자체 결정 (코드 단언 X)
- 추적 식별자 (`mainQuestionId` / `turnId`) 외부 호출자 보유 → record 외 분리
- `applyL1FalseNegativeGuard` 메서드 제거 = `RecommendedNextAction` LLM 자율 결정

## 의존
- 선행 Task: 01 (QuestionType — `AudioTurnAnalyzer` 가 `Question.questionType` 참조)
- 외부: 없음

## 테스트 케이스
- [ ] `AnswerAnalysis.empty()` 정상 생성 (모든 collection 빈 값)
- [ ] `dimensionGaps` 키 = RubricDimension.id, gap 0~3 범위
- [ ] `applyL1FalseNegativeGuard` 메서드 컴파일 부재 (메서드 호출 코드 grep 0)
- [ ] `TurnAnalysis` interface / `TurnAnalysisResult` / `TurnAnalysisPipeline` 파일 부재
- [ ] `AudioTurnAnalyzer.analyze` 반환 타입 = `AnswerAnalysis` (컴파일)
- [ ] 표준 트랙 회귀 = `FollowUpServiceTest` (CS) / `BehavioralFollowUpServiceTest` 통과

## 완료 기준
- [ ] 신규 record 컴파일 + 단위 테스트 green
- [ ] grep `applyL1FalseNegativeGuard` / `TurnAnalysisResult` / `TurnAnalysisPipeline` / `TurnAnalysis` interface / `missingPerspectives` / `answerQuality` / `AnswerFeedbackPerspective` / `AskedPerspectives` 잔존 0
- [ ] 표준 트랙 회귀 통과
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): AnswerAnalysis dimension 단일 축 재설계 + TurnAnalysis 마커 클래스 폐기
```

## 비고

`AudioTurnAnalyzer` / `AnswerAnalyzer` / `TextFallbackTurnAnalyzer` 시그니처 변경 = 표준 트랙 (CS / BEHAVIORAL) 호출자 영향. `FollowUpService:72,82,97` 호출 코드 갱신은 Task 06 진행.
