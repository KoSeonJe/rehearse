# Tech Spec — RESUME 트랙 채점 perspective 별 라벨/영역 라우팅 정상화

> **작성자**: Staff Engineer (by Claude)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement-be.md / implement-fe.md 진입 ★

---

## Why → Goal (1줄 미러)

채점 perspective (TECHNICAL / EXPERIENCE) 메타가 응답 → FE 라벨까지 흐르도록 노출 단계 정상화. RESUME PLAYGROUND 평가가 "기술 피드백" 라벨로 오노출되는 결함 제거.

## Evidence

- 현재 구조:
  - BE perspective 단일 진실 원천: `QuestionType.feedbackPerspective()` 메서드 (#447 V46 머지 후). `question_score.feedback_perspective` 컬럼/필드 제거됨.
  - BE 응답 매핑: `TimestampFeedbackResponse.java:147-168` — `toTechnicalFeedback` perspective 무관 모든 dimension 매핑. `from()` 진입부 (`line 97`) 이미 `feedback.getQuestion()` 영속 객체 추출 중.
  - BE DTO: `TechnicalFeedback` 클래스 (rubricId / levelFlag / dimensions) — perspective 필드 없음
  - BE 진입점: `QuestionSetFeedbackResponse.java:33-41` — `TimestampFeedback` 객체 전달 (`Question` 연관 객체 가용)
  - FE 타입: `frontend/src/types/interview.ts:170-174` — TechnicalFeedback (perspective 없음)
  - FE 컴포넌트: `frontend/src/components/feedback/content-tab.tsx:13-29` — "기술 피드백" 라벨 고정
  - FE 진입점: `frontend/src/components/feedback/feedback-panel.tsx:163` — `<ContentTab technicalFeedback={feedback.technicalFeedback} />`
- 도메인 enum: `FeedbackPerspective.java` — TECHNICAL / BEHAVIORAL / EXPERIENCE
- 컨벤션: `backend/.claude/rules/conventions.md` (DTO Builder + 정적 팩토리 from), `frontend/.claude/rules/conventions.md` (props drilling ≤ 2 levels), `frontend/.claude/rules/testing.md` (Integration 우선)
- 유사 spec: `docs/plans/433-resume-followup-questionid/tech-spec.md` (BE/FE 동시 분리 패턴)
- 사용자 결정 (product-spec):
  - NULL perspective row 백필 X
  - DTO 구조 = TechnicalFeedback 에 perspective 필드 추가
  - NULL / BEHAVIORAL fallback = "비어있음" 표시
- 추정 / 미확인: 운영 신규 인터뷰의 perspective 값 분포 (PLAYGROUND vs INTERROGATION 비율) 미측정. 본 작업 정확성에 영향 없음.

## Trade-offs

### Option A (채택) — TechnicalFeedback DTO 에 perspective 필드 추가
- 장점: 응답 구조 변경 최소 (필드 1개 추가), 기존 dimension 컬렉션 재사용, FE 단일 컴포넌트 분기로 처리 단순.
- 단점: TechnicalFeedback 클래스명이 perspective 무관 의미와 혼재 (네이밍 불일치 — 단 본 spec scope 외).
- 사유: product-spec 사용자 사전 결정. BE/FE 양쪽 변경 폭 최소.

### Option B (폐기) — perspective 별 별도 응답 필드 (technicalFeedback / experienceFeedback)
- 장점: 의미 명시 강함.
- 폐기 사유: 신규 perspective 추가마다 응답 구조 확장 필요. 화면 단일 영역 재사용 어려움. dimension 구조 중복.

### Option C (폐기) — DTO 무변경 + FE 가 dimension ref 로 perspective 추론
- 장점: BE 변경 0.
- 폐기 사유: 책임 도치 (채점 메타 단일 소스 위반). dimension ref 신설마다 FE 매핑 갱신 필요.

## Architecture

```
[Client] GET /api/v1/.../feedback (기존 endpoint)
  → FeedbackController → FeedbackService
    → QuestionSetFeedbackResponse.from(feedback, ..., qsByQid, dimsByQsId)
      → TimestampFeedbackResponse.from(timestamp, qs, dims)
        → toTechnicalFeedback(question, qs, dims)
            ├─ question.getQuestionType().feedbackPerspective().name() → TechnicalFeedback.perspective
            ├─ qs.rubricId → TechnicalFeedback.rubricId
            └─ dims → dimensions
  → JSON {
      ...,
      technicalFeedback: { perspective, rubricId, levelFlag, dimensions } | null
    }

데이터 소스: perspective 는 DB 컬럼이 아닌 `QuestionType` enum 메서드에서 파생 (#447 후 단일 진실 원천).
N+1 위험 없음: `from()` 이미 `feedback.getQuestion()` 영속 객체 추출 중 → 재호출 불필요.

[FE] FeedbackPanel → FeedbackCard → ContentTab(technicalFeedback)
  → switch (technicalFeedback.perspective):
      TECHNICAL  → 라벨 "기술 피드백" + dimensions 노출
      EXPERIENCE → 라벨 "경험 평가" + dimensions 노출
      그 외 (NULL / BEHAVIORAL / unknown) → 기존 "준비 중" fallback
```

### BE 변경 클래스
- `TimestampFeedbackResponse$TechnicalFeedback` — `perspective: String` 필드 추가. 기존 `@Getter @Builder` 패턴 유지 (`backend/.claude/rules/conventions.md` Lombok 룰 — `@Data` / `@Setter` / `@AllArgsConstructor` 금지). Builder 에 perspective 포함만.
- `TimestampFeedbackResponse#toTechnicalFeedback` — 시그니처 변경: `(QuestionScore, List<QuestionScoreDimension>)` → `(Question, QuestionScore, List<QuestionScoreDimension>)`. enum 경유 perspective 주입: `question.getQuestionType().feedbackPerspective().name()`. `question == null` 가드. 호출부 `from()` 에서 이미 추출한 `question` 변수 그대로 전달.

### FE 변경 클래스
- `frontend/src/types/interview.ts` — `TechnicalFeedback.perspective: string | null` 필드 추가
- `frontend/src/components/feedback/content-tab.tsx` — perspective 분기 라벨 매핑 + fallback 분기

## Data Model

변경 없음. perspective 는 `QuestionType` enum 메서드 파생 — DB 컬럼/필드 의존 없음. Flyway 마이그레이션 없음.

## API Contract

### Endpoint
기존 피드백 조회 endpoint (path 변경 없음). `FeedbackController` 의 `QuestionSetFeedbackResponse` 응답 schema 만 확장.

### Response (변경 부분)
```json
{
  "timestampFeedbacks": [
    {
      "id": 1,
      "questionId": 148,
      "technicalFeedback": {
        "perspective": "EXPERIENCE",
        "rubricId": "resume-v1",
        "levelFlag": null,
        "dimensions": [
          {
            "dimension": "experience_concreteness",
            "score": 1,
            "observation": "vague response, lacked specific details",
            "evidenceQuote": null
          }
        ]
      }
    }
  ]
}
```

### perspective 값
- `"TECHNICAL"` — STANDARD TECH_MAIN/FOLLOWUP, RESUME_INTERROGATION
- `"EXPERIENCE"` — RESUME_OPENER (채점 없음), RESUME_PLAYGROUND
- `"BEHAVIORAL"` — BEHAVIORAL_MAIN/FOLLOWUP (현재 채점 비활성, 도착 시 fallback 처리)
- `null` — 채점 자체 없음 (technicalFeedback null) 또는 question 메타 부재 (가드 조건)

### Error
변경 없음 (응답 schema 추가만).

### Backward Compatibility
- BE 단독 머지: FE 가 신규 필드 무시 (기존 라벨 그대로 노출 — 결함 잔존하나 회귀 X)
- FE 단독 머지: perspective undefined → fallback "준비 중" 표시 (모든 turn) → **프로덕션 모든 인터뷰 피드백 영역 공백 표시** (STANDARD 트랙 운영 사용자 전체 영향. 시각 회귀 강함)
- → **BE 우선 머지 필수**. BE prod 배포 + 응답에 perspective 필드 노출 확인 후 FE 머지.

## Verification (완료 판정)

### BE
- [ ] Domain Unit: `TimestampFeedbackResponse.toTechnicalFeedback` — RESUME_PLAYGROUND question → perspective="EXPERIENCE", RESUME_INTERROGATION question → perspective="TECHNICAL", question=null 가드 → perspective=null 매핑
- [ ] 빌드: `./gradlew test`

> Service Integration 신규 케이스 미추가. Domain Unit 으로 매핑 로직 충분히 커버 (이전 P2 지적 수용). 응답 schema 회귀는 기존 `QuestionSetFeedbackResponse*` 테스트에 의존.

### FE
- [ ] Integration (`ContentTab` 단독, RTL): perspective="EXPERIENCE" → "경험 평가" 라벨 노출 + dimensions 렌더
- [ ] Integration (`ContentTab` 단독, RTL): perspective="TECHNICAL" → "기술 피드백" 라벨 노출 (회귀 보호)
- [ ] Integration (`ContentTab` 단독, RTL): perspective=null → "해당 턴은 평가 대상이 아닙니다" fallback 노출
- [ ] Integration (`ContentTab` 단독, RTL): perspective="BEHAVIORAL" → "해당 턴은 평가 대상이 아닙니다" fallback 노출
- [ ] 빌드: `npm run lint && npm run test && npm run build` (TS strict union exhaustiveness)

> mock fixture 미생성: `frontend/src/mocks/` 는 dev 폴백 전용 (msw 미사용). 활용처 부재. spec 갱신으로 대체.
> RTL 단언: `screen.getByText({ exact: true })` / `queryByText` 사용. `renderToStaticMarkup` 패턴 폐기.

> 카테고리 결정: `frontend/.claude/rules/testing.md` 결정 트리 = "props 분기 → 화면 변화" → Integration. ContentTab 단독 props 변형으로 4 케이스 검증 충분. FeedbackPanel → ContentTab 통합 E2E 는 별도 추가 안 함 (회귀 위험 동일 범위).

### 관찰 가능 동작
- [ ] dev 서버 머지 후 신규 RESUME PLAYGROUND 인터뷰 진입 → 피드백 페이지 PLAYGROUND turn 클릭 → "경험 평가" 라벨 + 점수/관찰 노출
- [ ] STANDARD TECH_MAIN 인터뷰 회귀 확인 — "기술 피드백" 라벨 그대로

### 회귀 체크
- [ ] STANDARD 트랙 TECH_MAIN/FOLLOWUP 응답 schema 변경 없음 (`perspective="TECHNICAL"` 추가만)
- [ ] OPENER turn 채점 없음 → `technicalFeedback == null` 그대로 (현행 유지)

## Pre / Post State

### Pre (현재)
```java
// TimestampFeedbackResponse.java
TechnicalFeedback {
    String rubricId;
    String levelFlag;
    List<TechnicalDimensionFeedback> dimensions;
}

private static TechnicalFeedback toTechnicalFeedback(QuestionScore qs, List<QuestionScoreDimension> dims) { ... }
```
```ts
// frontend/src/types/interview.ts
interface TechnicalFeedback {
  rubricId: string
  levelFlag: string | null
  dimensions: TechnicalDimensionFeedback[]
}
```
- ContentTab: 라벨 "기술 피드백" 고정. perspective 분기 없음.

### Post (구현 후)
```java
TechnicalFeedback {
    String perspective;   // 추가: "TECHNICAL"|"EXPERIENCE"|"BEHAVIORAL"|null
    String rubricId;
    String levelFlag;
    List<TechnicalDimensionFeedback> dimensions;
}

// 시그니처 변경: Question 파라미터 추가. 데이터 소스 = QuestionType enum 메서드.
private static TechnicalFeedback toTechnicalFeedback(Question question, QuestionScore qs, List<QuestionScoreDimension> dims) { ... }
```
```ts
type FeedbackPerspective = 'TECHNICAL' | 'EXPERIENCE' | 'BEHAVIORAL'

interface TechnicalFeedback {
  perspective: FeedbackPerspective | null   // 추가, union 강타입
  rubricId: string
  levelFlag: string | null
  dimensions: TechnicalDimensionFeedback[]
}
```
- ContentTab: perspective 분기 라벨 + perspective 별 fallback 메시지 분기 (TECHNICAL → "기술 피드백" / EXPERIENCE → "경험 평가" / BEHAVIORAL/null → "해당 턴은 평가 대상이 아닙니다").

## 위험 / 마이그레이션 / 롤백

- **위험 (낮음)**: 응답 schema 추가만. 기존 클라이언트 영향 없음. DB 변경 없음.
- **마이그레이션**: 없음. 데이터 소스 = enum 메서드 (#447 V46 후 단일 진실 원천). DB 컬럼 의존 없음. 기존 row 영향 없음.
- **관찰성**: 로깅 / 메트릭 변경 없음. 기존 `RubricScoringEventListener` / `QuestionScorePersister` 로그 그대로 유지.
- **보안**: perspective 는 본인 인터뷰 응답 일부. 신규 노출 채널 / 인가 영역 변경 없음 (`security.md` 영향 없음).
- **롤백**:
  - BE 롤백 시 FE 가 perspective 못 받음 → 모든 turn fallback "준비 중" 표시 (프로덕션 시각 회귀). 핫픽스 = FE 도 같이 롤백.
  - FE 롤백 시 라벨 "기술 피드백" 단일로 복귀 (원래 결함 상태). BE 롤백 불필요.
- **머지 순서**: BE 우선 → prod 배포 확인 → FE 머지 (필수).

## 분기 결정

- [ ] 단일 영역 → `implement.md` 1개
- [x] **BE+FE 동시 → `implement-be.md` + `implement-fe.md`** (API contract 합의 후 병렬)
- [ ] BE 선행 강제 (강결합) → 강결합 아님. 단 머지 순서는 BE 우선 권장 (프롬프트 무관 — backward compat 안전)

API contract 합의 = 사용자 승인 게이트. 합의 후 BE/FE 병렬 시작 가능. FE 는 mock (msw) 로 perspective 4 케이스 (TECHNICAL / EXPERIENCE / BEHAVIORAL / null) 픽스처 사용.
