# Plan 06: QuestionPoolService 테스트 `[blocking]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

QuestionPoolService는 8개 public 메서드를 가진 핵심 서비스. 캐시 충분성 판단, 카테고리 분배, soft cap 로직 등 복잡한 비즈니스 규칙 검증 필요.

## 의존성

- 선행: Plan 05 (QuestionPool 엔티티 테스트로 엔티티 행위 검증 완료)
- 후행: Plan 07 (CacheableQuestionProvider가 이 서비스에 의존)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../questionpool/service/QuestionPoolServiceTest.java` | 신규 생성 (~14 tests) |

## 상세

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock 대상: `QuestionPoolRepository`

### @Nested 그룹 및 테스트 케이스

**IsPoolSufficient** — 3개 오버로드
- `isPoolSufficient_sufficient_returnsTrue`: count >= required * multiplier
- `isPoolSufficient_insufficient_returnsFalse`: count < required * multiplier
- `isPoolSufficient_withCategoryFilter_delegatesCorrectly`: 카테고리 필터 전달
- `isPoolSufficient_withUsedPoolIds_appliesUserMultiplier`: usedPoolIds 제외 후 판단

**ShouldSaveToPool** — soft cap(200)
- `shouldSaveToPool_belowCap_returnsTrue`: active < 200
- `shouldSaveToPool_atCap_returnsFalse`: active >= 200

**SelectFromPool** — 3개 오버로드
- `selectFromPool_basic_returnsAll`: 기본 선택
- `selectFromPool_withFilter_appliesCategoryFilter`: 카테고리 필터
- `selectFromPool_withUsedIds_excludesUsed`: 사용된 ID 제외

**SelectWithCategoryDistribution** — 분배 알고리즘
- `selectWithCategoryDistribution_evenDistribution_balancesCategories`: 균등 분배
- `selectWithCategoryDistribution_singleCategory_returnsAll`: 단일 카테고리
- `selectWithCategoryDistribution_nullCategory_mappedToUnknown`: null → "UNKNOWN"
- `selectWithCategoryDistribution_candidatesLessThanRequired_returnsAll`: 후보 < 요청
- `selectWithCategoryDistribution_emptyList_returnsEmpty`: 빈 리스트

## 담당 에이전트

- Implement: `test-engineer` — 서비스 단위 테스트
- Review: `qa` — 분배 알고리즘 엣지 케이스 (나머지 처리, 빈 리스트, 단일 카테고리)

## 검증

- [ ] `./gradlew test --tests "QuestionPoolServiceTest"` 통과
- [ ] Mock: `QuestionPoolRepository`만 사용
- [ ] 8개 public 메서드 전체 커버
- [ ] @Nested 그룹 4개 이상
- [ ] `progress.md` 상태 업데이트 (Plan 06 → Completed)
