# 질문 다양성 개선 + 질문 수 조정

- **Status**: Completed
- **Date**: 2026-03-18

---

## Context

- 같은 직무/레벨/유형으로 면접을 생성하면 매번 비슷한 질문이 나옴
- 질문 수가 5분/1개로 부족한 느낌 → 3분/1개로 변경 요청
- 후속 질문이 존재하므로 메인 질문 당 실질 소요 시간은 답변 + 후속질문 포함

---

## Why

1. **질문 반복**: 프롬프트가 동일 입력에 대해 결정적 → 반복 사용 시 학습 효과 저하
2. **질문 수 부족**: 5분/1개는 후속질문 포함 시간 기준. 메인 질문만 보면 너무 적음

---

## 1. 질문 다양성 개선

### 기존

**`ClaudeRequest.java`**
```java
@Getter
@Builder
public class ClaudeRequest {
    private final String model;
    private final int maxTokens;
    private final String system;
    private final List<Message> messages;
    // temperature 없음 → API 기본값 적용
}
```

**`ClaudePromptBuilder.buildQuestionUserPrompt()`**
```java
// 동일 입력 → 동일 프롬프트 → 비슷한 질문 반복
prompt.append(String.format("""
        직무: %s
        레벨: %s
        면접 유형: %s
        질문 수: %d개
        """, positionKorean, levelKorean, typesKorean, questionCount));
```

### 개선

**A. `ClaudeRequest`에 `temperature` 필드 추가**
```java
@Getter
@Builder
public class ClaudeRequest {
    private final String model;
    private final int maxTokens;
    private final String system;
    private final List<Message> messages;

    // 추가
    @Builder.Default
    private final double temperature = 0.9;  // 다양성 확보 + JSON 형식 유지
}
```

**B. User prompt에 변동 요소 삽입**
```java
// buildQuestionUserPrompt() 마지막에 추가
prompt.append(String.format("세션 ID: %s\n", UUID.randomUUID()));
prompt.append("이전 면접과 중복되지 않는 새로운 관점의 질문을 생성해주세요.\n");
```

- `UUID`가 매번 달라지므로 같은 입력이어도 Claude가 다른 질문을 생성
- `temperature: 0.9`로 출력 분포 확장하되, JSON 파싱이 깨지지 않는 수준 유지

---

## 2. 질문 수 조정 (5분 → 3분 당 1개)

### 기존

**`ClaudePromptBuilder.java`**
```java
private static final int MINUTES_PER_QUESTION = 5;
private static final int MIN_QUESTION_COUNT = 2;
private static final int MAX_QUESTION_COUNT = 24;

public static int calculateQuestionCount(Integer durationMinutes, int typeCount) {
    if (durationMinutes != null) {
        int count = (int) Math.round((double) durationMinutes / MINUTES_PER_QUESTION);  // 5분당 1개
        return Math.max(MIN_QUESTION_COUNT, Math.min(count, MAX_QUESTION_COUNT));
    }
    // ...
}
```

| 면접 시간 | 질문 수 |
|----------|---------|
| 15분 | 3개 |
| 20분 | 4개 |
| 30분 | 6개 |
| 45분 | 9개 |
| 60분 | 12개 |

### 개선

```java
private static final int MINUTES_PER_QUESTION = 3;  // 5 → 3
```

| 면접 시간 | 기존 (5분) | 개선 (3분) |
|----------|-----------|-----------|
| 15분 | 3개 | 5개 |
| 20분 | 4개 | 7개 |
| 30분 | 6개 | 10개 |
| 45분 | 9개 | 15개 |
| 60분 | 12개 | 20개 |

- `MAX_QUESTION_COUNT(24)` 변경 없음

---

## 3. 변경 파일 요약

| 파일 | 변경 내용 |
|------|----------|
| `ClaudeRequest.java` | `temperature` 필드 추가 (기본값 0.9) |
| `ClaudePromptBuilder.java` | `MINUTES_PER_QUESTION` 5→3, user prompt에 UUID + 다양성 지시 추가 |
