# 답변 흐름 + 개선 포인트

- **Status**: Completed
- **Date**: 2026-03-18

---

## 1. 현재 답변 흐름

### 1-1. 답변 시작

```
[답변 시작 버튼]
  → MediaRecorder 녹화 시작/재개
  → Web Speech API STT 시작
  → phase → 'recording'
```

### 1-2. 답변 완료 → 후속질문 루프

```
[답변 완료 버튼]
  │
  ├─ 녹화 pause, STT stop, TTS stop
  │
  ├─ 답변 텍스트 수집 (STT 누적 transcript에서 offset 이후만)
  │
  ├─ 답변 타임스탬프 기록 (questionId, startMs, endMs) — 스토어에 저장
  │
  ├─ 후속질문 답변이었으면 → followUpHistory에 push (메모리, 임시)
  │
  └─ 후속질문 가능? (3라운드 미만 && 답변텍스트 있음)
        │
        ├─ YES ──────────────────────────────────────────────────┐
        │   POST /api/v1/interviews/{id}/follow-up               │
        │   {                                                    │
        │     questionSetId,                                     │
        │     questionContent: "메인 질문 내용",                   │
        │     answerText: "현재 라운드 답변",                      │
        │     previousExchanges: [이전 후속Q&A 히스토리]            │
        │   }                                                    │
        │     ↓                                                  │
        │   BE: Claude API 호출 → Question(FOLLOWUP_N) DB 저장    │
        │     ↓                                                  │
        │   응답: { questionId, question, reason, type }          │
        │     ↓                                                  │
        │   FE: TTS로 후속질문 읽기 → 사용자 답변 대기              │
        │     ↓                                                  │
        │   (다시 답변 완료로 돌아옴, 최대 3회 반복)                 │
        │                                                        │
        └─ NO → 다음으로 전환 (transitionToNext)
              │
              ├─ 같은 세트 내 다음 질문
              │   → TTS: "다음 질문 드리겠습니다" → nextQuestion()
              │
              ├─ 질문세트 전환 (아래 1-3)
              │   → TTS: "다음 주제로 넘어가겠습니다" → 업로드 + nextQuestionSet()
              │
              └─ 마지막 질문
                  → TTS: "면접을 마치겠습니다" → completeInterview()
```

### 1-3. 질문세트 전환 시 API 호출

```
질문세트 마지막 질문 답변 완료
  │
  ├─ ① recorder.restart() → 현재 세트 영상 Blob 반환 + 새 녹화 시작
  │
  ├─ ② IndexedDB 폴백 저장
  │
  ├─ ③ 답변 타임스탬프 BE 전송
  │   POST /api/v1/interviews/{id}/question-sets/{qsId}/answers
  │   {
  │     answers: [
  │       { questionId: 1, startMs: 0, endMs: 45000 },       ← MAIN
  │       { questionId: 5, startMs: 48000, endMs: 72000 },   ← FOLLOWUP_1
  │       { questionId: 6, startMs: 75000, endMs: 95000 }    ← FOLLOWUP_2
  │     ]
  │   }
  │     ↓
  │   BE: QuestionSetAnswer 엔티티 생성 + DB 저장
  │       questionSet.analysisStatus → PENDING_UPLOAD
  │
  ├─ ④ Presigned URL 발급
  │   POST /api/v1/interviews/{id}/question-sets/{qsId}/upload-url
  │   { contentType: "video/webm" }
  │     ↓
  │   BE: FileMetadata 생성 + QuestionSet에 연결
  │       S3 키: videos/{interviewId}/qs_{qsId}.webm
  │     ↓
  │   응답: { uploadUrl, s3Key, fileMetadataId }
  │
  └─ ⑤ S3 직접 업로드 (백그라운드)
      PUT {presignedUrl} — 영상 Blob
      → 업로드 완료 → EventBridge → Lambda 분석 트리거
```

### 1-4. 답변 타임스탬프 용도

질문세트당 하나의 영상에 여러 답변이 연속 녹화됨:

```
[영상: qs_1.webm]
0s ──── 45s ──── 72s ──── 95s
│  MAIN 답변  │ FU1 답변 │ FU2 답변 │
```

타임스탬프가 있어야 Lambda가 영상에서 각 답변 구간을 잘라서 개별 분석 가능.

### 1-5. 후속질문 히스토리란?

FE 스토어 **메모리상** 임시 데이터 (DB 저장 아님):

```
followUpHistory (Map<questionIndex, Array>)
  └─ questionIndex=0: [
       { question: "IoC 컨테이너 내부는?", answer: "빈 팩토리가..." },
       { question: "실무에서 겪은 DI 문제는?", answer: "순환참조가..." },
     ]
```

목적: 다음 후속질문 생성 시 `previousExchanges`로 Claude API에 전달 → 중복 질문 방지.
면접 끝나면 사라짐.

---

## 2. 개선 포인트

### 2-1. QuestionType 단순화: FOLLOWUP_1/2/3 → FOLLOWUP

**기존**
```java
public enum QuestionType {
    MAIN, FOLLOWUP_1, FOLLOWUP_2, FOLLOWUP_3
}

// InterviewService.determineFollowUpType()
return switch ((int) followUpCount) {
    case 0 -> QuestionType.FOLLOWUP_1;
    case 1 -> QuestionType.FOLLOWUP_2;
    case 2 -> QuestionType.FOLLOWUP_3;
    default -> throw ...;
};
```

**개선**
```java
public enum QuestionType {
    MAIN, FOLLOWUP
}

// 몇 번째 후속질문인지는 orderIndex로 판단
// 3회 제한은 count로 체크
long followUpCount = questionSet.getQuestions().stream()
    .filter(q -> q.getQuestionType() == QuestionType.FOLLOWUP)
    .count();
if (followUpCount >= MAX_FOLLOWUP_ROUNDS) throw ...;
```

**이유**: `FOLLOWUP_1/2/3`은 정보 중복. `orderIndex`로 순서를 이미 알 수 있고, 제한은 count로 체크하면 됨.

### 2-2. QuestionSetAnswer → QuestionAnswer 네이밍 변경

**기존**
```
QuestionSetAnswer (question FK, startMs, endMs)
```

**개선**
```
QuestionAnswer (question FK, startMs, endMs)
```

**이유**: 실제 관계는 `Question ↔ Answer`이지 `QuestionSet ↔ Answer`가 아님. FK도 `question`을 참조. "질문세트의 답변"이라는 오해 방지.

### 2-3. referenceType 매핑 수정 (프롬프트 버그)

**기존** (잘못됨)
```
CS 질문 → referenceType: GUIDE
RESUME 질문 → referenceType: MODEL_ANSWER
```

**개선**
```
CS 질문 → referenceType: MODEL_ANSWER (정답이 있으므로 구체적 모범답변)
RESUME 질문 → referenceType: GUIDE (이력서 기반, 정답 없음, 답변 방향 가이드)
```

**수정 파일**: `ClaudePromptBuilder.buildQuestionSystemPrompt()` 프롬프트 내용

---

## 3. 변경 파일 요약

| 파일 | 변경 내용 |
|------|----------|
| `QuestionType.java` | `FOLLOWUP_1/2/3` → `FOLLOWUP` 단일화 |
| `InterviewService.determineFollowUpType()` | switch 제거, count 체크로 변경 |
| `QuestionSetAnswer.java` | `QuestionAnswer`로 리네이밍 |
| `QuestionSetAnswerRepository.java` | `QuestionAnswerRepository`로 리네이밍 |
| `ClaudePromptBuilder.java` | referenceType CS↔RESUME 매핑 수정 |
| FE `use-answer-flow.ts` | `FOLLOWUP_1/2/3` 분기 제거 |
