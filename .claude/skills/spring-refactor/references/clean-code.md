# 클린코드 레퍼런스 (Java Spring 맥락)

## 핵심 원칙

클린코드의 목표는 **"읽는 사람이 의도를 빠르게 파악할 수 있는 코드"**를 만드는 것이다.
코드는 쓰는 시간보다 읽는 시간이 10배 이상 많다. 따라서 읽기 쉬운 코드가 좋은 코드다.

## 함수/메서드

### 크기 제한
- **메서드**: 20줄 이내 권장, 30줄 초과 시 분리 검토
- **클래스**: 200줄 이내 권장, 300줄 초과 시 책임 분리 검토
- **파라미터**: 3개 이내 권장, 4개 이상이면 객체로 묶기

```java
// BAD: 파라미터 6개
public Interview create(Long userId, Position position, int duration,
                        String resumeText, InterviewType type, InterviewLevel level) { ... }

// GOOD: 요청 객체로 묶기
public Interview create(CreateInterviewRequest request, Long userId) { ... }
```

### 한 가지 일만 하기
함수는 **하나의 추상화 수준**에서 **하나의 작업**만 수행해야 한다.

```java
// BAD: 여러 추상화 수준이 혼재
public void completeInterview(Long id) {
    Interview interview = repository.findById(id)                // 저수준: DB 조회
        .orElseThrow(() -> new BusinessException(NOT_FOUND));
    if (interview.getStatus() != IN_PROGRESS)                    // 비즈니스 규칙
        throw new BusinessException(INVALID_STATUS);
    interview.setStatus(COMPLETED);                              // 상태 변경
    interview.setCompletedAt(LocalDateTime.now());               // 상태 변경
    feedbackService.requestAnalysis(interview);                  // 외부 호출
    notificationService.notify(interview.getUserId(), "완료");   // 외부 호출
    repository.save(interview);                                  // 저수준: DB 저장
}

// GOOD: 동일한 추상화 수준, 각각 한 가지 일
@Transactional
public void completeInterview(Long id) {
    Interview interview = interviewFinder.findById(id);
    interview.complete();  // Entity가 자기 상태 전이 책임
    // Domain Event로 후속 처리 디커플링
}
```

### 명령-조회 분리 (CQS)
메서드는 **상태를 변경하거나(Command)** **값을 반환하거나(Query)** 둘 중 하나만 해야 한다.

```java
// BAD: 변경하면서 반환
public Interview updateAndReturn(Long id, UpdateRequest request) { ... }

// GOOD: 분리
public void update(Long id, UpdateRequest request) { ... }     // Command
public InterviewResponse findById(Long id) { ... }             // Query
```

단, 생성 후 ID 반환 같은 경우는 실용적으로 허용:
```java
public Long create(CreateInterviewRequest request) { ... }  // 허용
```

## 네이밍

### 의도를 드러내는 이름
```java
// BAD
int d;                          // 경과 시간? 거리? 기간?
List<int[]> list1;              // 무슨 리스트?
String s;                       // 무슨 문자열?

// GOOD
int elapsedTimeInDays;
List<Question> unansweredQuestions;
String resumeText;
```

### 맥락을 제공하는 이름
```java
// BAD: 맥락 없는 약어
String fn, ln, addr;

// GOOD: 클래스로 맥락 제공
public record UserProfile(String firstName, String lastName, String address) {}
```

### 검색 가능한 이름
```java
// BAD: 매직 넘버
if (questions.size() > 10) { ... }
Thread.sleep(5000);

// GOOD: 상수로 추출
private static final int MAX_QUESTIONS_PER_SET = 10;
private static final long RETRY_DELAY_MS = 5000L;
```

## 조건문

### Early Return (가드 절)
```java
// BAD: 중첩 if
public InterviewResponse getInterview(Long id, Long userId) {
    Interview interview = repository.findById(id).orElse(null);
    if (interview != null) {
        if (interview.getUserId().equals(userId)) {
            return InterviewResponse.from(interview);
        } else {
            throw new BusinessException(FORBIDDEN);
        }
    } else {
        throw new BusinessException(NOT_FOUND);
    }
}

// GOOD: 가드 절로 평탄화
public InterviewResponse getInterview(Long id, Long userId) {
    Interview interview = interviewFinder.findById(id);
    interview.validateOwner(userId);
    return InterviewResponse.from(interview);
}
```

### 부정 조건 회피
```java
// BAD
if (!interview.isNotCompleted()) { ... }

// GOOD
if (interview.isCompleted()) { ... }
```

### 조건 추출
```java
// BAD: 복잡한 조건식 인라인
if (interview.getStatus() == COMPLETED && 
    interview.getFeedbacks().size() >= 3 && 
    interview.getAnalysis() != null) { ... }

// GOOD: 의미 있는 메서드로 추출
if (interview.isFullyAnalyzed()) { ... }

// Entity 내부
public boolean isFullyAnalyzed() {
    return status == COMPLETED && feedbacks.size() >= MIN_FEEDBACKS && analysis != null;
}
```

## 주석

### 좋은 주석
```java
// 법적 주석
// Copyright (c) 2026 Rehearse

// TODO: 성능 최적화 필요 — 현재 N+1 쿼리 발생
// WARNING: 이 메서드는 외부 API를 호출하므로 타임아웃 주의

// 정규식 설명
// 형식: HH:mm:ss.SSS (00:00:00.000 ~ 23:59:59.999)
Pattern.compile("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");
```

### 나쁜 주석
```java
// BAD: 코드를 반복하는 주석
// 인터뷰를 생성한다
public Interview createInterview() { ... }

// BAD: 주석 처리된 코드 (git이 관리함)
// interview.setStatus(COMPLETED);
// interview.setCompletedAt(LocalDateTime.now());

// BAD: 이력 주석
// 2026-03-11 KSJ: 상태 검증 추가
// 2026-03-15 KSJ: 에러 메시지 변경
```

## 에러 처리

### 예외를 사용한 흐름 제어
```java
// BAD: null 반환 후 호출자가 체크
public Interview findById(Long id) {
    return repository.findById(id).orElse(null);
}
// 호출자: if (interview == null) { ... }  — 누락 가능

// GOOD: 예외로 비정상 흐름 명확화
public Interview findById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new BusinessException(INTERVIEW_NOT_FOUND));
}
```

### 예외 계층
- `BusinessException`: 비즈니스 규칙 위반 (4xx)
- `SystemException`: 시스템 오류 (5xx) — 외부 API 실패, DB 장애 등
- **절대 하지 말 것**: `catch (Exception e) { }` — 예외 삼키기

## 코드 스멜 탐지

| 스멜 | 시그널 | 해결 |
|-----|--------|-----|
| Long Method | 30줄+ 메서드 | Extract Method |
| Long Parameter List | 4개+ 파라미터 | Introduce Parameter Object |
| Duplicate Code | 3곳+ 동일 로직 | Extract Method/Class |
| Magic Number | 하드코딩 숫자/문자열 | Extract Constant |
| Dead Code | 호출되지 않는 메서드/변수 | 삭제 |
| Inconsistent Naming | 같은 개념 다른 이름 | 통일 |
| Nested Callbacks | 콜백 3단계+ 중첩 | 메서드 분리 또는 CompletableFuture 체이닝 |
