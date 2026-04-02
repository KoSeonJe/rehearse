# Plan 04: BE 엔티티 + DTO + DB 마이그레이션

> 상태: Draft
> 작성일: 2026-04-01

## Why

Lambda에서 새로 생성하는 `structureLevel`, `structureComment`, `attitudeComment` 필드를 DB에 저장하고 FE에 내려주려면 BE의 Entity, DTO, 마이그레이션을 변경해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/migration/V18__add_feedback_v2_columns.sql` | 3개 컬럼 ALTER TABLE (신규) |
| `backend/src/main/java/.../entity/TimestampFeedback.java` | 3개 필드 추가 |
| `backend/src/main/java/.../dto/SaveFeedbackRequest.java` | 3개 필드 추가 |
| `backend/src/main/java/.../dto/TimestampFeedbackResponse.java` | ContentFeedback에 structure 추가, 응답 구조 재편 |
| `backend/src/main/java/.../service/InternalQuestionSetService.java` | 새 필드 매핑 로직 |

## 상세

### 1. Flyway 마이그레이션

```sql
ALTER TABLE timestamp_feedback ADD COLUMN structure_level VARCHAR(20);
ALTER TABLE timestamp_feedback ADD COLUMN structure_comment TEXT;
ALTER TABLE timestamp_feedback ADD COLUMN attitude_comment TEXT;
```

모든 컬럼 nullable — 기존 데이터 하위 호환.

### 2. TimestampFeedback Entity

3개 필드 추가 + `@Builder` 생성자 파라미터에도 추가:

```java
@Column(length = 20)
private String structureLevel;

@Column(columnDefinition = "TEXT")
private String structureComment;

@Column(columnDefinition = "TEXT")
private String attitudeComment;
```

> **주의**: Lombok `@Builder`는 생성자 파라미터 기반이므로, `TimestampFeedback.java`의 `@Builder` 생성자에 `structureLevel`, `structureComment`, `attitudeComment` 파라미터를 반드시 추가해야 builder에서 사용 가능하다.

### 3. SaveFeedbackRequest

Lambda → BE 저장 요청 DTO에 3개 필드 추가 (nullable).

### 4. InternalQuestionSetService — builder 체인에 신규 필드 추가

`InternalQuestionSetService.saveFeedback()`의 `TimestampFeedback.builder()` 체인에 추가:

```java
.structureLevel(item.getStructureLevel())
.structureComment(item.getStructureComment())
.attitudeComment(item.getAttitudeComment())
```

### 5. TimestampFeedbackResponse 구조 재편

**ContentFeedback** (기존 + 확장):
```java
@Getter
@Builder
public static class ContentFeedback {
    private final String verbalComment;
    private final String structureLevel;      // 신규
    private final String structureComment;    // 신규
    private final List<AccuracyIssue> accuracyIssues;
    private final CoachingResponse coaching;
}
```

**DeliveryFeedback** 구조는 유지하되, `attitudeComment` 추가:
```java
@Getter
@Builder
public static class DeliveryFeedback {
    private final NonverbalFeedback nonverbal;
    private final VocalFeedback vocal;
    private final String attitudeComment;      // 신규
}
```

> 기존 `delivery` 필드명과 하위 구조(nonverbal, vocal)는 유지한다. FE 탭 이름만 "자세·말투 분석"으로 변경.

### 6. `TimestampFeedbackResponse.from()` 매핑 업데이트

`from()` 메서드의 builder 체인에 신규 필드 매핑 추가:

```java
// ContentFeedback builder (기존 line 91-95)
ContentFeedback content = ContentFeedback.builder()
        .verbalComment(feedback.getVerbalComment())
        .structureLevel(feedback.getStructureLevel())          // 신규
        .structureComment(feedback.getStructureComment())      // 신규
        .accuracyIssues(accuracyIssues)
        .coaching(coaching)
        .build();

// DeliveryFeedback builder (기존 line 113-116)
DeliveryFeedback delivery = DeliveryFeedback.builder()
        .nonverbal(nonverbal)
        .vocal(vocal)
        .attitudeComment(feedback.getAttitudeComment())        // 신규
        .build();
```

> **참고**: `coachingStructure`(coaching의 구조 코칭)와 `structureLevel`(답변 구조 평가)은 같은 "structure" 단어지만 다른 의미. 혼동 주의.

## 담당 에이전트

- Implement: `backend` — DB 마이그레이션 + Entity/DTO 변경
- Review: `architect-reviewer` — 데이터 모델 일관성, 하위 호환

## 검증

- H2 DB에서 마이그레이션 적용 확인 (`./gradlew test` 또는 애플리케이션 부팅)
- Lambda Mock 요청으로 3개 신규 필드 포함 피드백 저장 → 조회 API에서 반환 확인
- 기존 데이터 (신규 필드 null) 조회 시 에러 없이 null 반환 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
