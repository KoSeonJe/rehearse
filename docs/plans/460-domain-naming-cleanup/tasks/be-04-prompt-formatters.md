# BE Task 04 — `PromptFormatters` 신규 + 변환 함수 단일 출처화

## 목적

`formatPerspectives` 정의 5곳 / `toReferenceLabel` 정의 3곳 중복 → `infra/ai/prompt/PromptFormatters` 단일 클래스로 통합. 호출 9곳을 `PromptFormatters.*` 호출로 치환.

## 변경 파일

### 신규
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/PromptFormatters.java`
  - `final class` + `private` 생성자 (인스턴스화 차단).
  - `public static String formatPerspectives(List<AnswerFeedbackPerspective> perspectives)`
  - `public static String toReferenceLabel(ReferenceType refType)`
  - 두 메서드 본문 = 기존 5곳 / 3곳 정의 중 동일한 구현 (의미 동등성 보존). 동일하지 않은 경우 발견 시 즉시 사용자 질문.

### 정의 제거 (private static 메서드 8개 삭제)
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilder.java`
  - `formatPerspectives` (line 70) + `toReferenceLabel` (line 60) 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AudioTurnAnalyzerPromptBuilder.java`
  - `formatPerspectives` (line 66) + `toReferenceLabel` (line 56) 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java`
  - `formatPerspectives` (line 126) 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/context/AnswerAnalysisJsonRenderer.java`
  - `formatPerspectives` (line 47) 제거
- `backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java`
  - `formatPerspectives` (line 101) + `toReferenceLabel` (line 91) 제거

### 호출 치환 (9곳)
- `AnswerAnalyzerPromptBuilder` line 49 (`toReferenceLabel`), line 54 (`formatPerspectives`) → `PromptFormatters.*`
- `AudioTurnAnalyzerPromptBuilder` line 48 (`toReferenceLabel`), line 50 (`formatPerspectives`) → `PromptFormatters.*`
- `FollowUpPromptBuilder` line 103, 106 (`formatPerspectives`) → `PromptFormatters.*`
- `AnswerAnalysisJsonRenderer` line 24, 27 (`formatPerspectives`) → `PromptFormatters.*`
- `AnswerAnalyzer` line 51 (`toReferenceLabel`), line 52 (`formatPerspectives`) → `PromptFormatters.*`

### 테스트 (영향 가능)
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilderTest.java` — 출력 단언 동일 (의미 동등성 보존) → 갱신 없음 또는 임포트만.
- `backend/src/test/java/com/rehearse/api/domain/interview/service/AnswerAnalyzerTest.java` — 동일.
- 신규: `PromptFormattersTest` (Domain Unit) **권장** — 입력별 라벨 매핑 단언.

## 핵심 변경 (요지)

- 의미 동등성 보존 = **출력 문자열 동일** 보장. 5곳 정의 본문이 미세 차이 (예: 구분자 ", " vs " / ") 발견 시 사용자 질문 (선택지: 어느 본문을 정본으로 쓸지).
- `final` + `private` 생성자 = 정적 유틸 클래스 패턴. 컨벤션 (`backend/.claude/rules/conventions.md`) 위반 없음.
- infra → domain 임포트 (`AnswerFeedbackPerspective`, `ReferenceType`) = 정상 방향.

## 미정 사항 (실제 구현 시 발견 가능)

- 5곳 `formatPerspectives` 본문 동일 여부 미검증 (현재 plan 단계). 다르면 사용자 질문 (`AskUserQuestion`):
  - Option A (추천): 가장 많이 쓰이는 본문 = 정본
  - Option B: 가장 표준적 (한국어 라벨 명확) 본문 = 정본
  - Option C: 본문별 분리 (PromptFormatters 메서드 2개 이상 분리) — 단일 출처 원칙 약화

## 테스트

- 카테고리: Domain Unit (정적 유틸 = 모킹 불필요).
- 신규 `PromptFormattersTest` 권장 — `formatPerspectives(List.of(TECHNICAL, BEHAVIORAL))` → 기대 라벨 단언. `toReferenceLabel(MODEL_ANSWER)` → 기대 라벨 단언.
- 기존 PromptBuilder 테스트 = 출력 단언 그대로 GREEN 유지 = 의미 동등성 회귀 검증.

## 완료 기준

- [ ] `grep -rEn "private static String (formatPerspectives|toReferenceLabel)" backend/src/main/java` = 0
- [ ] `grep -rEn "PromptFormatters\.(formatPerspectives|toReferenceLabel)" backend/src/main/java | wc -l` ≥ 9
- [ ] `PromptFormatters.java` `final` + `private` 생성자 확인
- [ ] 기존 `AnswerAnalyzerPromptBuilderTest` / `AnswerAnalyzerTest` GREEN (출력 동일)
- [ ] 신규 `PromptFormattersTest` GREEN (권장)

## 의존

- 선행: T1 (`AnswerFeedbackPerspective` 시그니처 확정).
- 후행: 없음.

## 커밋

```
refactor(BE): formatPerspectives / toReferenceLabel 변환 함수 PromptFormatters 단일 출처로 통합
```
