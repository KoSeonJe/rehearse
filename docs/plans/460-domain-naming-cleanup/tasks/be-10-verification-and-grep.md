# BE Task 10 — 테스트 일괄 갱신 + grep 검증 + 빌드 GREEN

## 목적

T1~T9 누적 변경에 대한 **최종 검증 task**. 누락 grep / 컴파일 / 테스트 회귀를 잡는 안전망.

## 검증 항목

### 1. grep 검증 (handoff `참고 명령` 4개 + 추가 2개)

```bash
# enum 임포트 0
grep -rn "QuestionSetCategory" backend/src/main/java
grep -rn "import com.rehearse.api.domain.feedback.entity.FeedbackPerspective" backend/src/main/java
grep -rn "import com.rehearse.api.domain.interview.entity.Perspective\b" backend/src/main/java

# 변환 함수 단일 출처
grep -rEn "private static String (formatPerspectives|toReferenceLabel)" backend/src/main/java

# 필드 / JSON 키
grep -rn "selectedPerspective" backend/src/main/java
grep -n "private String answer\b" backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java

# YAML 키 (추가)
grep -rn "feedbackPerspective" backend/src/main/resources/rubric/
grep -rn "feedbackPerspective" backend/src/main/java/com/rehearse/api/domain/feedback/
grep -rn "feedbackPerspective" backend/src/main/java/com/rehearse/api/domain/question/dto/
```

**기대값**: 모두 **0건**.

### 2. 빌드

```bash
./gradlew clean build
```

**기대값**: BUILD SUCCESSFUL. 컴파일 에러 / 테스트 실패 0.

### 3. 전체 테스트

```bash
./gradlew test
```

**기대값**: BUILD SUCCESSFUL. testing.md 카테고리별 통과 (Domain Unit ≥60% / Service Integration / E2E / Repository / Smoke / ArchUnit).

### 4. 회귀 체크 (수동)

- [ ] `FollowUpExchange` JSON 송신 키 = `answerText` (FE wire 키 일치 — FE PR 구현 후 통합 검증 영역).
- [ ] `_mapping.yaml` / `experience-technical-rubric.yaml` 키 변경 후 `RubricLoaderTest` GREEN.
- [ ] `question_set.category VARCHAR(50)` 컬럼 = 기존 row 호환 (Testcontainers + Flyway 자동).
- [ ] AskedPerspectives 변수명 `askedPerspectives` 41건 = **잔존** (Phase 1 비스코프 — 잘못 변경 안 했는지 확인).

## 추가 보강 (있으면)

- 신규 `PromptFormattersTest` 작성 (Task 4 미완 시 본 task 에서 보강).
- `PromptFormatters.formatPerspectives` / `toReferenceLabel` 의 **출력 의미 동등성** 회귀가 기존 PromptBuilder 테스트로 자동 검증되는지 점검. 회귀 발견 = 즉시 사용자 보고 + Phase 2 이관 (tech-spec L390).

## 완료 기준

- [ ] grep 8종 모두 0건
- [ ] `./gradlew build` BUILD SUCCESSFUL
- [ ] `./gradlew test` BUILD SUCCESSFUL
- [ ] testing.md 카테고리 비중 변동 없음 (신규 추가 = `PromptFormattersTest` Domain Unit 1개 권장)
- [ ] 회귀 체크 4종 PASS

## 의존

- 선행: T1~T9 모두.
- 후행: 없음 (PR 생성 / 머지는 별도 — git-manager 위임).

## 커밋

본 task 는 **검증 전용** = 테스트 추가 외 코드 변경 0이면 별도 커밋 불필요. 단, `PromptFormattersTest` 신규 추가 시:

```
test(BE): PromptFormatters 단위 테스트 추가 + 도메인 네이밍 정리 grep 검증
```

또는 추가 코드 변경 0이면 본 task 자체 커밋 없음. PR description 의 verification 섹션에 grep / 빌드 결과 첨부.

## 다음 단계 (본 plan 외)

- BE PR 생성 → `code-reviewer-backend` 리뷰 (자기 코드 셀프 승인 금지).
- 사용자 명시 승인 후 `git-manager` → `gh pr merge --squash`.
- BE 머지 직후 FE PR 즉시 머지 (`implement-fe.md`).
- Epic Issue close 후 `handoff.md` 제거.
