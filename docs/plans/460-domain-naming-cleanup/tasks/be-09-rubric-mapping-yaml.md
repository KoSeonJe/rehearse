# BE Task 09 — `RubricFamily.MappingRule` 필드 + YAML 키 + `RubricLoader` 파싱 키 → `rubricCategory`

## 목적

루브릭 영역의 `feedbackPerspective` 단어를 `rubricCategory` 로 일괄 정리. record 컴포넌트 + YAML 키 + Loader 파싱 동기화.

## 변경 파일

### 정의 변경
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricFamily.java`
  - record `MappingRule(_, _, String feedbackPerspective, _)` 컴포넌트명 → `rubricCategory` (line 41)
  - `matches()` 본문 line 54-58 — `feedbackPerspective` 변수 / 비교 식 → `rubricCategory`
  - `ctx.feedbackPerspective()` 호출 (line 55, 58) — `MatchContext` 측 메서드명 확인 필요. **`MatchContext` 도 `rubricCategory()` 로 변경 시 cascade 발생** → 전체 record/메서드 일관성 위해 동시 변경.
  - 단, `MatchContext.feedbackPerspective()` 메서드 호출처가 `RubricFamily` 내부 한정이면 본 task 영역. 외부에서도 호출되면 별도 cascade.

### YAML 키 변경
- `backend/src/main/resources/rubric/_mapping.yaml`
  - line 8 주석 `feedbackPerspective: FeedbackPerspective (TECHNICAL / BEHAVIORAL / EXPERIENCE)` → `rubricCategory: RubricCategory (TECHNICAL / BEHAVIORAL / EXPERIENCE)`
  - line 38 키 `feedbackPerspective: EXPERIENCE` → `rubricCategory: EXPERIENCE`
  - 값 `EXPERIENCE` 자체는 그대로 (Phase 2 영역).
- `backend/src/main/resources/rubric/experience-technical-rubric.yaml`
  - line 9 키 `- feedbackPerspective: EXPERIENCE` → `- rubricCategory: EXPERIENCE`
  - **추가 발견 항목** (tech-spec 미반영). RubricLoader.parseRubric 은 본 키 미파싱 (메타/`applies_to` 영역) → 코드 영향 0이지만 단어 일관성 차원 변경.

### Loader 파싱 키 변경
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricLoader.java`
  - line 169 변수 `String feedbackPerspective = null;` → `String rubricCategory = null;`
  - line 180 `when.containsKey("feedbackPerspective")` → `when.containsKey("rubricCategory")`
  - line 181 `when.get("feedbackPerspective").toString()` → `when.get("rubricCategory").toString()`
  - line 184 `new MappingRule(..., feedbackPerspective, use)` → `new MappingRule(..., rubricCategory, use)`

### 테스트 갱신
- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/RubricLoaderTest.java`
  - 단언 키 갱신 (`rubricCategory:`)
- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/RubricScorerTest.java`
- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/RubricScoringEventListenerTest.java`
- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListenerIntegrationTest.java`

## 핵심 변경 (요지)

- YAML 키와 Loader 파싱 키가 **반드시 동시 변경**. 분리 시 룰 미매칭 → `RubricLoaderTest` RED.
- record 컴포넌트명 변경 = 호출처 cascade. IntelliJ Safe Rename 권장.
- `experience-technical-rubric.yaml` 추가 변경 = tech-spec 미반영 발견 사항 → 본 task 에서 처리.

## 미정 사항 (실제 구현 시 발견 가능)

- 다른 rubric YAML 파일 (`concept-*.yaml`, `fallback-generic-rubric.yaml`, `nonverbal-*.yaml`, `resume-rubric.yaml`) 에 `feedbackPerspective:` 키 존재 여부 확인 필요. 본 plan grep 검증 결과 = `experience-technical-rubric.yaml` + `_mapping.yaml` 2 파일만. 다른 파일에서 발견 시 발견 사항 보고 + 본 task 범위 확장.

## 테스트

- 카테고리: Service Integration (`RubricLoaderTest`, `RubricScorerTest`).
- 실행: `./gradlew test --tests "*Rubric*"`
- 회귀 핵심: YAML 룰 매칭 GREEN — `rubricCategory: EXPERIENCE` 키 → 기존 룰 동일하게 매칭.

## 완료 기준

- [ ] `grep -n "feedbackPerspective" backend/src/main/resources/rubric/_mapping.yaml` = 0
- [ ] `grep -n "feedbackPerspective" backend/src/main/resources/rubric/experience-technical-rubric.yaml` = 0
- [ ] `grep -rn "feedbackPerspective" backend/src/main/resources/rubric/*.yaml` = 0 (전체 yaml)
- [ ] `grep -rn "feedbackPerspective" backend/src/main/java/com/rehearse/api/domain/feedback/rubric/` = 0
- [ ] `RubricLoaderTest` GREEN (룰 매칭 회귀 0)

## 의존

- 선행: T2 (`RubricCategory` rename).
- 후행: 없음.

## 커밋

```
refactor(BE): Rubric 영역 feedbackPerspective → rubricCategory (record + YAML 키 + Loader 파싱)
```

## 위험

- **YAML 키 / Loader 파싱 키 분리 변경 위험**: 한쪽만 변경 시 룰 미매칭 → 런타임 회귀. **완화** = 본 task 단일 commit 내 동시 변경 + `RubricLoaderTest` 즉시 실행.
- **`experience-technical-rubric.yaml` 영향**: Loader 파싱 미적용 = 코드 영향 0이지만 변경 누락 시 grep 검증 실패.
