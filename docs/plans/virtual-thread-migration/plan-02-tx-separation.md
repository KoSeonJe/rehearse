# Plan 02: generateFollowUp() 트랜잭션 범위 분리

> 상태: Draft
> 작성일: 2026-03-23

## Why

`InterviewService.generateFollowUp()`이 `@Transactional` 안에서 Whisper STT + Claude API를 호출하여 DB 커넥션을 60~90초 점유한다. 외부 API 호출을 트랜잭션 밖으로 분리하면 커넥션 점유 시간이 ~40ms로 단축된다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/.../interview/service/FollowUpTransactionHandler.java` | 신규 — DB 조회/저장 트랜잭션 분리 |
| `backend/src/main/java/.../interview/dto/FollowUpContext.java` | 신규 — Phase 간 데이터 전달 record |
| `backend/src/main/java/.../interview/service/InterviewService.java` | `generateFollowUp()` @Transactional 제거, 오케스트레이션으로 변경 |

## 상세

### 변경 전 (현재)

```
@Transactional { DB조회 → STT(60s) → Claude(30s) → DB저장 }  // 커넥션 ~90s
```

### 변경 후

```
STT(트랜잭션X) → readOnly TX{DB조회}(~20ms) → Claude(트랜잭션X) → TX{DB저장}(~20ms)
```

### FollowUpTransactionHandler (@Component)

- `loadFollowUpContext(Long interviewId, Long questionSetId)` — `@Transactional(readOnly=true)`
  - 면접 조회 + IN_PROGRESS 상태 검증
  - QuestionSet 조회 + followUp 라운드 제한 검증
  - `FollowUpContext` record 반환
- `saveFollowUpResult(Long questionSetId, GeneratedFollowUp followUp, int orderIndex)` — `@Transactional`
  - QuestionSet 재조회 → Question 생성 → 저장 → Question 반환

### FollowUpContext (record)

```java
public record FollowUpContext(
    Position position,
    TechStack effectiveTechStack,
    InterviewLevel level,
    Long questionSetId,
    int nextOrderIndex
) {}
```

### InterviewService.generateFollowUp() 변경

1. `@Transactional` 제거 (클래스 레벨 readOnly는 유지)
2. answerText 결정 (STT 또는 텍스트) — 트랜잭션 없음
3. `followUpTransactionHandler.loadFollowUpContext()` — 짧은 readOnly TX
4. `aiClient.generateFollowUpQuestion()` — 트랜잭션 없음
5. `followUpTransactionHandler.saveFollowUpResult()` — 짧은 write TX

### 이동 대상

- `validateFollowUpRoundLimit()` (line 224-232) → `FollowUpTransactionHandler`로 이동
- `MAX_FOLLOWUP_ROUNDS` 상수 → 함께 이동

## 담당 에이전트

- Implement: `backend` — 서비스 리팩토링
- Review: `architect-reviewer` — 트랜잭션 경계 정확성, 프록시 동작 검증

## 검증

- `generateFollowUp()` 메서드에 `@Transactional` 없음 확인
- `FollowUpTransactionHandler`의 두 메서드에만 `@Transactional` 존재 확인
- 기존 `InterviewServiceTest` 통과
- `progress.md` 상태 업데이트 (Task 2 → Completed)
