# 피드백 테이블 설계 리뷰 + 개선

- **Status**: Completed
- **Date**: 2026-03-19

---

## Context

Lambda 분석 결과를 저장하는 피드백 테이블 구조에 설계 문제가 있음.
`TimestampFeedback`이 어떤 `Question`에 대한 피드백인지 직접 참조가 없고,
`QuestionSetAnswer`와 타임스탬프 데이터가 중복되며, 네이밍도 부정확.

---

## 1. 현재 테이블 관계

```
Interview
  └─ QuestionSet (1:N)
       ├─ Question (1:N)
       │    ├─ questionType: MAIN / FOLLOWUP_1 / FOLLOWUP_2 / FOLLOWUP_3
       │    ├─ questionText: "Spring DI란?"
       │    ├─ modelAnswer: "IoC 컨테이너는..."
       │    └─ referenceType: MODEL_ANSWER / GUIDE
       │
       ├─ QuestionSetAnswer (1:N, question FK)
       │    ├─ question → Question
       │    ├─ startMs: 0
       │    └─ endMs: 45000
       │
       ├─ FileMetadata (1:1) — 영상 파일
       │
       └─ QuestionSetFeedback (1:1)
            ├─ questionSetScore: 75
            ├─ questionSetComment: "전반적으로..."
            │
            └─ TimestampFeedback (1:N)
                 ├─ answerType: MAIN         ← Question 참조 없이 타입만
                 ├─ startMs: 0               ← QuestionSetAnswer와 중복
                 ├─ endMs: 45000             ← QuestionSetAnswer와 중복
                 ├─ transcript: "..."
                 ├─ verbalScore: 80
                 ├─ verbalComment: "..."
                 ├─ fillerWordCount: 3
                 ├─ eyeContactScore: 70
                 ├─ postureScore: 85
                 ├─ expressionLabel: "neutral"
                 ├─ nonverbalComment: "..."
                 └─ overallComment: "..."
```

---

## 2. 문제점

### 2-1. TimestampFeedback → Question 참조 없음

```
TimestampFeedback { answerType: MAIN, startMs: 0, endMs: 45000, verbalScore: 80 }
```

- 어떤 질문에 대한 피드백인지 **직접 연결 없음**
- `answerType(MAIN)` + `startMs/endMs`로 간접 매칭해야 함
- 질문 텍스트, 모범답변과 피드백을 조인하려면 번거로운 역추적 필요

### 2-2. QuestionSetAnswer와 TimestampFeedback 타임스탬프 중복

```
QuestionSetAnswer:  { question FK, startMs: 0, endMs: 45000 }    ← FE가 먼저 저장
TimestampFeedback:  { answerType: MAIN, startMs: 0, endMs: 45000 } ← Lambda가 나중에 저장
```

- 같은 답변 구간 데이터를 두 테이블에 각각 저장
- 저장 시점만 다름 (FE 답변 완료 시 vs Lambda 분석 완료 시)
- 서로 참조 없음

### 2-3. answerType 중복

```
TimestampFeedback.answerType = MAIN
Question.questionType = MAIN
```

- Question을 참조하면 `questionType`으로 알 수 있으므로 `answerType` 불필요

### 2-4. 네이밍 부정확

| 현재 | 실제 의미 | 개선 |
|------|----------|------|
| `QuestionSetAnswer` | 질문에 대한 답변 타임스탬프 | `QuestionAnswer` |
| `QuestionSetFeedback` | 질문세트 전체 피드백 | 유지 (이건 맞음) |
| `TimestampFeedback` | 유지 (이것도 맞음) | 유지 |

---

## 3. 개선안

### 3-1. TimestampFeedback에 question FK 추가

```java
// 현재
@Entity
public class TimestampFeedback {
    @ManyToOne
    private QuestionSetFeedback questionSetFeedback;
    private QuestionType answerType;  // 간접 매칭
    private long startMs;
    private long endMs;
    // ... scores
}

// 개선
@Entity
public class TimestampFeedback {
    @ManyToOne
    private QuestionSetFeedback questionSetFeedback;

    @ManyToOne
    private Question question;  // ← 추가: 어떤 질문에 대한 피드백인지 직접 참조

    // answerType 제거 — question.questionType으로 대체

    private long startMs;
    private long endMs;
    // ... scores
}
```

**효과:**
- `TimestampFeedback → Question → modelAnswer` 직접 조인 가능
- 피드백 페이지에서 "질문 + 모범답변 + 피드백" 한번에 표시
- `answerType` 필드 제거 가능

### 3-2. QuestionSetAnswer → QuestionAnswer 리네이밍

```java
// 현재
@Entity
@Table(name = "question_set_answer")
public class QuestionSetAnswer {
    private Question question;
    private long startMs;
    private long endMs;
}

// 개선
@Entity
@Table(name = "question_answer")
public class QuestionAnswer {
    private Question question;
    private long startMs;
    private long endMs;
}
```

### 3-3. QuestionSetAnswer와 TimestampFeedback 관계 정리

두 가지 선택지:

**A. 분리 유지 (추천)**
```
QuestionAnswer — FE가 답변 완료 시 저장 (분석 전)
TimestampFeedback — Lambda가 분석 완료 시 저장 (분석 후)
```
- 저장 시점이 다르므로 분리 유지가 자연스러움
- 다만 TimestampFeedback에 question FK 추가하면 startMs/endMs 중복은 남음

**B. 통합**
- `TimestampFeedback`이 `QuestionAnswer`를 참조하는 구조
- Lambda가 분석 결과를 기존 `QuestionAnswer`에 연결
- 중복 제거되지만 Lambda가 QuestionAnswer ID를 알아야 하는 의존성 생김

---

## 4. 개선 후 테이블 관계

```
Interview
  └─ QuestionSet (1:N)
       ├─ Question (1:N)
       │    ├─ questionType: MAIN / FOLLOWUP
       │    ├─ questionText
       │    ├─ modelAnswer
       │    └─ referenceType
       │
       ├─ QuestionAnswer (1:N, question FK)  ← 리네이밍
       │    ├─ question → Question
       │    ├─ startMs
       │    └─ endMs
       │
       ├─ FileMetadata (1:1)
       │
       └─ QuestionSetFeedback (1:1)
            ├─ questionSetScore
            ├─ questionSetComment
            │
            └─ TimestampFeedback (1:N)
                 ├─ question → Question     ← 추가
                 ├─ startMs
                 ├─ endMs
                 ├─ transcript
                 ├─ verbalScore, verbalComment
                 ├─ fillerWordCount
                 ├─ eyeContactScore, postureScore
                 ├─ expressionLabel
                 ├─ nonverbalComment
                 └─ overallComment
                 (answerType 제거)
```

---

## 5. 변경 우선순위

| 우선순위 | 변경 | 이유 |
|---------|------|------|
| **높음** | `TimestampFeedback`에 `question` FK 추가 | 질문-피드백 직접 연결 필수 |
| **높음** | `answerType` 필드 제거 | `question.questionType`으로 대체, 중복 제거 |
| **중간** | `QuestionSetAnswer` → `QuestionAnswer` 리네이밍 | 의미 명확화 |
| **낮음** | `QuestionSetAnswer`-`TimestampFeedback` 통합 | 저장 시점 차이로 분리 유지 권장 |

---

## 6. Lambda 연동 영향

| 항목 | 변경 |
|------|------|
| Lambda → `POST /api/internal/.../feedback` | `questionId` 필드 추가 필요 |
| `SaveFeedbackRequest.TimestampFeedbackItem` | `questionId` 추가, `answerType` 제거 |
| `InternalQuestionSetService.saveFeedback()` | Question 조회 + TimestampFeedback에 연결 |
| Lambda 분석 코드 | QuestionSetAnswer에서 questionId를 읽어서 피드백에 포함시키도록 수정 |
