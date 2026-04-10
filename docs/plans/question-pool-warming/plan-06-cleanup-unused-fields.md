# Plan 06: QuestionPool 불필요 필드 삭제

> 상태: Draft
> 작성일: 2026-04-10

## Why

QuestionPool 엔티티에 4개 필드가 저장만 되고 어디서도 읽히지 않는다. 시드 데이터 작성 전에 정리하여 SQL INSERT를 간결하게 하고, 불필요한 복잡도를 제거한다.

| 필드 | 저장 | 읽기 | 삭제 이유 |
|------|------|------|----------|
| `evaluationCriteria` | O (NOT NULL) | X | Question 엔티티로 복사되지 않고, 어디서도 조회/활용 안 됨 |
| `followUpStrategy` | O (ENUM) | X | 후속 질문 생성 시 사용하지 않음. 전부 REALTIME으로 동작 |
| `questionOrder` | O (INT) | X | Question 엔티티에 별도 orderIndex 존재. pool 내 순서 무의미 |
| `qualityScore` | O (DECIMAL) | X | 선택/정렬에 사용 안 됨. 항상 1.00 하드코딩 |

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/migration/V20__cleanup_question_pool_columns.sql` | 4개 컬럼 DROP |
| `backend/src/main/java/com/rehearse/api/domain/questionpool/entity/QuestionPool.java` | 4개 필드 제거, create() 시그니처 변경, Builder 수정 |
| `backend/src/main/java/com/rehearse/api/domain/questionpool/entity/FollowUpStrategy.java` | enum 삭제 |
| `backend/src/main/java/com/rehearse/api/domain/questionpool/service/QuestionPoolService.java` | convertAndCacheIfEligible()에서 제거된 필드 참조 삭제, parseFollowUpStrategy() 삭제 |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedQuestion.java` | evaluationCriteria, followUpStrategy 필드 유지 (AI 응답 파싱용) — pool 저장 시만 무시 |
| `backend/src/main/resources/db/seed/*.sql` | 기존 시드 파일의 INSERT 컬럼에서 제거 |

## 상세

### 1. Flyway 마이그레이션 (V20)

```sql
-- V20: QuestionPool 불필요 컬럼 삭제
ALTER TABLE question_pool DROP COLUMN evaluation_criteria;
ALTER TABLE question_pool DROP COLUMN follow_up_strategy;
ALTER TABLE question_pool DROP COLUMN question_order;
ALTER TABLE question_pool DROP COLUMN quality_score;
```

### 2. QuestionPool 엔티티 변경

```java
// 삭제할 필드:
// - String evaluationCriteria
// - Integer questionOrder  
// - FollowUpStrategy followUpStrategy
// - BigDecimal qualityScore

// create() 시그니처 변경:
public static QuestionPool create(String cacheKey, String content, String category,
        String modelAnswer, String referenceType) {
    // evaluationCriteria null 체크 제거
    // followUpStrategy 기본값 로직 제거
    // qualityScore 초기화 제거
}
```

### 3. QuestionPoolService 변경

```java
// convertAndCacheIfEligible() 내 변경:
List<QuestionPool> pools = generated.stream()
        .map(gq -> QuestionPool.create(
                cacheKey,
                gq.getContent(),
                gq.getCategory(),
                gq.getModelAnswer(),
                gq.getReferenceType()))  // followUpStrategy, evaluationCriteria, questionOrder 제거
        .collect(Collectors.toList());

// parseFollowUpStrategy() 메서드 삭제
```

### 4. FollowUpStrategy enum 삭제

`FollowUpStrategy.java` 파일 자체를 삭제.

### 5. 시드 SQL INSERT 형식 (정리 후)

```sql
INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('{cache_key}', '{질문}', '{카테고리}',
 '{모범 답변}',
 'MODEL_ANSWER', TRUE, NOW());
```

기존 대비 `question_order`, `evaluation_criteria`, `follow_up_strategy`, `quality_score` 4개 컬럼 제거.
`INSERT IGNORE`로 중복 실행 시 에러 방지.

## 담당 에이전트

- Implement: `backend` — 마이그레이션, 엔티티, 서비스 수정
- Review: `architect-reviewer` — 스키마 변경 영향 범위, 기존 데이터 호환성

## 검증

- V20 마이그레이션 실행 성공 (H2/MySQL)
- 기존 QuestionPool 데이터의 다른 필드 보존 확인
- QuestionGenerationService의 기존 플로우 정상 동작 (AI 생성 → pool 저장 → Question 생성)
- 기존 테스트 통과
- `progress.md` 상태 업데이트 (Task 6 → Completed)
