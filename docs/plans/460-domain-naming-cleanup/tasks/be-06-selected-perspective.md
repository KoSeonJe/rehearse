# BE Task 06 — `selectedPerspective` → `selectedAnswerFeedbackPerspective`

## 목적

`FollowUpRequest$FollowUpExchange.selectedPerspective` / `FollowUpResponse.selectedPerspective` 필드명 + JSON 키 + Builder + 호출처 일괄 갱신.

## 변경 파일

### 필드명 + JSON 키 (BE+FE 동시)
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java`
  - `private String selectedPerspective;` → `private String selectedAnswerFeedbackPerspective;` (line 36)
  - 생성자 / getter cascade
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpResponse.java`
  - `private final String selectedPerspective;` → `private final String selectedAnswerFeedbackPerspective;` (line 24)
  - Builder 자동 갱신 (`@Builder`)

### 호출처 갱신
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java`
  - `.selectedPerspective(followUp.getSelectedPerspective())` (line 162) → `.selectedAnswerFeedbackPerspective(followUp.getSelectedAnswerFeedbackPerspective())`
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedFollowUp.java`
  - 필드명 (line 36) `private String selectedPerspective;` → `private String selectedAnswerFeedbackPerspective;`
  - copy 로직 (line 55) `copy.selectedPerspective = this.selectedPerspective;` → `copy.selectedAnswerFeedbackPerspective = this.selectedAnswerFeedbackPerspective;`

### 주석 (코드 영향 0)
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java` line 128-129 — 주석에 `selectedPerspective` 언급. **단어 일관성 차원에서 갱신** (단순 문자열 치환).

### 테스트 갱신
- `FollowUpServiceTest`, `StandardFollowUpPolicyTest`, `ResumeTrackPolicyTest`, `TestFixtures` (있는 경우).

## 핵심 변경 (요지)

- 필드 타입 = `String` 유지 (enum 미적용). 단순 필드명 / 키 rename.
- IntelliJ Safe Rename 권장.
- `GeneratedFollowUp.copy` 메서드 본문 (`copy.x = this.x` 패턴) 도 자동 갱신 — 누락 시 빌드 RED.

## API Contract (BE+FE 동시)

```jsonc
// 요청 (FollowUpRequest.previousExchanges[*])
// 변경 전
{ ..., "selectedPerspective": "TRADEOFF" }
// 변경 후
{ ..., "selectedAnswerFeedbackPerspective": "TRADEOFF" }

// 응답 (FollowUpResponse)
// 변경 전
{ ..., "selectedPerspective": "TRADEOFF" }
// 변경 후
{ ..., "selectedAnswerFeedbackPerspective": "TRADEOFF" }
```

## 테스트

- 카테고리: Service Integration (`FollowUpServiceTest`).
- 실행: `./gradlew test --tests "*FollowUp*" "*Policy*"`

## 완료 기준

- [ ] `grep -rn "selectedPerspective" backend/src/main/java` = 0 (주석 포함)
- [ ] `grep -rn "selectedAnswerFeedbackPerspective" backend/src/main/java | wc -l` ≥ 5 (FollowUpRequest, FollowUpResponse, FollowUpService, GeneratedFollowUp x2)
- [ ] `./gradlew compileJava` GREEN
- [ ] `FollowUpServiceTest` GREEN

## 의존

- 선행: T1 (`AnswerFeedbackPerspective` 단어 정렬을 위해). 단, 필드 타입 = String 유지 → 컴파일상 T1 무관 → 단어 일관성 위해 T1 후 정렬 권장.
- 후행: 없음.

## 커밋

```
refactor(BE): selectedPerspective → selectedAnswerFeedbackPerspective 일괄 (필드 + JSON 키 + Builder + 호출처)
```
