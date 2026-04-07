# Plan 02: BE DTO 직렬화 규약 변경

> 상태: Draft
> 작성일: 2026-04-07

## Why

`requirements.md`의 결정 2 실행. Lambda가 보내는 `CommentBlock` 객체를 BE가 받아 기존 컬럼에 JSON 문자열로 저장하고, 응답 시 다시 객체로 노출. **DB 스키마/엔티티/Flyway 변경 없음**. `accuracy_issues` 컬럼이 이미 동일 패턴(`TimestampFeedbackResponse.parseAccuracyIssues`)을 사용 중이므로 그대로 따른다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/questionset/dto/SaveFeedbackRequest.java` | `*Comment` 필드 5종을 `String` → `CommentBlock` 객체로 변경, `CommentBlock` 정적 클래스 추가 |
| `backend/src/main/java/com/rehearse/api/domain/questionset/dto/TimestampFeedbackResponse.java` | `parseCommentBlock` 헬퍼 추가, `ContentFeedback`/`NonverbalFeedback`/`VocalFeedback`/`DeliveryFeedback` 응답 필드를 `CommentBlock` 객체로 변경 |
| `backend/src/main/java/com/rehearse/api/domain/questionset/service/InternalQuestionSetService.java` (또는 저장 서비스) | Lambda에서 받은 `CommentBlock`을 `ObjectMapper.writeValueAsString` 으로 직렬화해 엔티티에 저장 |
| `backend/src/test/java/com/rehearse/api/domain/questionset/dto/TimestampFeedbackResponseTest.java` (신규 또는 기존 테스트 보강) | `parseCommentBlock` 3 케이스 단위 테스트 |

엔티티 (`TimestampFeedback.java`)는 **변경하지 않는다** — 필드 타입 String 유지, 저장된 값만 ✓△→ 대신 JSON 문자열로 바뀔 뿐.

## 상세

### 1. `SaveFeedbackRequest.java`

`TimestampFeedbackItem`의 5개 필드 타입을 변경하고 정적 `CommentBlock` 클래스 추가:

```java
@Getter @NoArgsConstructor
public static class TimestampFeedbackItem {
    private Long questionId;
    @NotNull private Long startMs;
    @NotNull private Long endMs;
    private String transcript;

    // feedback-v3: ✓△→ 단일 String → 정형 객체
    private CommentBlock verbalComment;
    private CommentBlock nonverbalComment;
    private CommentBlock vocalComment;
    private CommentBlock attitudeComment;
    private CommentBlock overallComment;

    private Integer fillerWordCount;
    private String expressionLabel;

    private String eyeContactLevel;
    private String postureLevel;
    private String toneConfidenceLevel;

    private List<String> fillerWords;
    private String speechPace;
    private String emotionLabel;

    private String accuracyIssues;
    private String coachingStructure;
    private String coachingImprovement;
}

@Getter @NoArgsConstructor
public static class CommentBlock {
    private String positive;
    private String negative;
    private String suggestion;
}
```

> Lambda는 같은 키 이름(`verbalComment`, `nonverbalComment` 등)으로 객체를 보내므로 Jackson이 자동 매핑된다.

### 2. 저장 서비스 (직렬화)

저장 처리 코드에서 `CommentBlock` 객체를 JSON 문자열로 직렬화해 엔티티 setter에 넘긴다:

```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

private String serialize(SaveFeedbackRequest.CommentBlock block) {
    if (block == null) return null;
    try {
        return OBJECT_MAPPER.writeValueAsString(block);
    } catch (JsonProcessingException e) {
        throw new IllegalStateException("CommentBlock 직렬화 실패", e);
    }
}

// 사용
feedback.setVerbalComment(serialize(item.getVerbalComment()));
feedback.setNonverbalComment(serialize(item.getNonverbalComment()));
feedback.setVocalComment(serialize(item.getVocalComment()));
feedback.setAttitudeComment(serialize(item.getAttitudeComment()));
feedback.setOverallComment(serialize(item.getOverallComment()));
```

> 정확한 호출부는 `InternalQuestionSetService` 또는 `TimestampFeedbackRepository` 위에 위치한 저장 메서드. 구현 시 grep으로 `setVerbalComment` 호출처를 찾아 동일하게 갱신.

### 3. `TimestampFeedbackResponse.java`

응답 DTO에 `CommentBlock` 정적 클래스 추가하고 매퍼 헬퍼 작성:

```java
@Getter @Builder
public static class CommentBlock {
    private final String positive;
    private final String negative;
    private final String suggestion;
}

private static CommentBlock parseCommentBlock(String json) {
    if (json == null || json.isBlank()) return null;
    try {
        return OBJECT_MAPPER.readValue(json, CommentBlock.class);
    } catch (Exception e) {
        // legacy ✓△→ 또는 손상된 문자열 → positive에만 raw 입력
        return CommentBlock.builder().positive(json).build();
    }
}
```

`ContentFeedback`/`NonverbalFeedback`/`VocalFeedback`/`DeliveryFeedback` 빌더에서 String 필드를 `CommentBlock`으로 교체:

```java
@Getter @Builder
public static class ContentFeedback {
    private final CommentBlock verbalComment;   // 변경
    private final List<AccuracyIssue> accuracyIssues;
    private final CoachingResponse coaching;
}

@Getter @Builder
public static class NonverbalFeedback {
    private final String eyeContactLevel;
    private final String postureLevel;
    private final String expressionLabel;
    private final CommentBlock nonverbalComment; // 변경
}

@Getter @Builder
public static class VocalFeedback {
    private final String fillerWords;
    private final Integer fillerWordCount;
    private final String speechPace;
    private final String toneConfidenceLevel;
    private final String emotionLabel;
    private final CommentBlock vocalComment;     // 변경
}

@Getter @Builder
public static class DeliveryFeedback {
    private final NonverbalFeedback nonverbal;
    private final VocalFeedback vocal;
    private final CommentBlock attitudeComment;  // 변경
}
```

`overallComment`는 응답의 어디에 노출할지 확인 후 동일 패턴 적용. (현재 미노출이라면 적절한 위치에 추가하거나 별도 필드로 빼기)

`from(TimestampFeedback feedback)` 매퍼에서 빌더 호출 시 `parseCommentBlock(feedback.getVerbalComment())` 등으로 변환.

### 4. 단위 테스트

```java
@Test
void parseCommentBlock_정상_JSON() {
    String json = "{\"positive\":\"좋음\",\"negative\":\"개선\",\"suggestion\":\"이렇게\"}";
    CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(json);
    assertThat(block.getPositive()).isEqualTo("좋음");
    assertThat(block.getNegative()).isEqualTo("개선");
    assertThat(block.getSuggestion()).isEqualTo("이렇게");
}

@Test
void parseCommentBlock_null_반환() {
    assertThat(TimestampFeedbackResponse.parseCommentBlock(null)).isNull();
    assertThat(TimestampFeedbackResponse.parseCommentBlock("")).isNull();
}

@Test
void parseCommentBlock_legacy_raw_문자열() {
    String legacy = "✓ 잘했음\n△ 보완\n→ 이렇게";
    CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(legacy);
    assertThat(block.getPositive()).isEqualTo(legacy);
    assertThat(block.getNegative()).isNull();
    assertThat(block.getSuggestion()).isNull();
}
```

`parseCommentBlock`이 private이면 `from()` 매퍼를 통해 간접 검증하거나 package-private으로 노출.

## 담당 에이전트

- Implement: `backend` — DTO 변경, 직렬화, 단위 테스트
- Review: `architect-reviewer` — 레이어링 (서비스에서 ObjectMapper 사용 위치), DTO 책임 분리

## 검증

- `./gradlew :backend:test --tests "*TimestampFeedbackResponseTest*"` 통과
- `./gradlew :backend:test` 전체 회귀 통과
- 로컬 H2로 BE 기동 후 mock Lambda 페이로드(curl)로 저장 → 응답 재조회 시 `CommentBlock` 객체가 정상 노출되는지
- legacy 데이터 (✓△→ 문자열) 가 들어있는 행을 수동 INSERT 후 응답 fallback 동작 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
