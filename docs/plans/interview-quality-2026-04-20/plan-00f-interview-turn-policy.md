# Plan 00f: Interview Turn Policy Abstraction (Phase 0) `[parallel:00c]`

> 상태: Completed (2026-04-24, S3b)
> 산출물: `domain/interview/policy/` 5 파일 + `Interview.getTrack()` + `FollowUpTransactionHandler` 리팩터 + 4 테스트
> 작성일: 2026-04-21
> 완료일: 2026-04-24
> 주차: W2 (plan-00c와 병렬, plan-07 선행)
> 해결 RC: "턴 최대치(`MAX_FOLLOWUP_ROUNDS=2`)가 Transaction Handler에 하드코딩되어 트랙별 확장 불가"
>
> **실측 교정 (2026-04-24)**: 원래 plan 은 `interview.getResumeSkeletonId() != null` 로 라우팅 분기. 실제 `Interview` 엔티티에 `resumeSkeletonId` 필드 부재. → `Interview.getTrack()` 이 `interviewTypes.contains(InterviewType.RESUME_BASED)` 로 RESUME 트랙 판정. Resolver 는 `Interview.getTrack()` switch 로 라우팅.

## Why

`FollowUpTransactionHandler.java:25` 의 `MAX_FOLLOWUP_ROUNDS = 2` 상수와 `validateFollowUpRoundLimit()` (라인 89-97) 은 **CS 트랙 전용 UX 튜닝 값이 시스템 불변식처럼 영속 계층에 하드코딩**된 설계 결함이다. plan-07 Resume 트랙은 `playground-max-turns: 3` + `chain-max-depth: 4` = **프로젝트당 최대 7턴**이 필요한데, 현재 구조는 3번째 follow-up에서 `QuestionErrorCode.MAX_FOLLOWUP_EXCEEDED` (code=`QUESTION_SET_004`) 를 던져 **plan-07 구현 자체를 차단**한다.

근본 원인은 "턴 수 제한"이라는 **도메인 규칙**이 Transaction/Persistence 계층에 박혀 있고, 트랙(CS/Language/Resume)마다 턴 의미가 다른데(지식 확인 vs 파고들기 vs 놀이터) 단일 상수로 전 트랙을 통제하는 데 있다.

**해결**: `InterviewTurnPolicy` Strategy 도입 → 트랙별 정책 분리 → Transaction Handler 는 정책 위임만. Resume 트랙은 자체 정책으로 자연스럽게 확장된다. CS/Language 트랙은 기존 "2턴" 동작을 정책 구현체로 옮겨 **행위 무변경**.

### 설계 결함 근거

1. **SRP 위반**: Transaction Handler 가 트랜잭션 경계 + 비즈니스 규칙(턴 제한) 동시 소유
2. **OCP 위반**: 신규 트랙 추가 시 `FollowUpTransactionHandler` 수정 필요 → plan-07/11 등 트랙 추가마다 if-else 증식
3. **도메인 개념 누락**: "트랙"이라는 개념이 코드에 명시적 타입 없이 `resumeSkeletonId != null` 같은 임시 검사로 흩뿌려질 위험
4. **테스트 격리 어려움**: 현재 `FollowUpTransactionHandlerTest.loadFollowUpContext_maxFollowUpExceeded` 는 정책 + 트랜잭션을 동시에 검증 → 정책만 단위 테스트 불가

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/InterviewTrack.java` | 신규 enum — `CS`, `LANGUAGE`, `RESUME` (향후 확장 포인트) |
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/InterviewTurnPolicy.java` | 신규 인터페이스 — `assertCanContinue(Interview, QuestionSet)`, `getTrack()` |
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/StandardFollowUpPolicy.java` | 신규. CS/Language 트랙용. `maxRounds=2` 를 YAML 주입. `QuestionType.FOLLOWUP` 카운트 로직 이관 |
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/ResumeTrackPolicy.java` | 신규. Resume 트랙용. `playground-max-turns`+`chain-max-depth` 설정 소비. plan-07 의 `ChainStateTracker` 주입은 **plan-07 에서 완성**. 본 plan 에서는 skeleton + 7턴 하드 상한 구현 |
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/InterviewTurnPolicyResolver.java` | 신규. `Interview` → `InterviewTurnPolicy` 매핑. `interview.getResumeSkeletonId() != null` → `ResumeTrackPolicy`, else `StandardFollowUpPolicy` |
| `backend/src/main/java/com/rehearse/api/domain/interview/entity/Interview.java` | **수정**. `getTrack()` 도출 메서드 추가 (파생 값). DB 컬럼 추가 **안 함** |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandler.java` | **수정**. `MAX_FOLLOWUP_ROUNDS` 상수 + `validateFollowUpRoundLimit()` 삭제 → `turnPolicyResolver.resolve(interview).assertCanContinue(...)` 위임 |
| `backend/src/main/java/com/rehearse/api/domain/question/exception/QuestionErrorCode.java` | **유지**. `MAX_FOLLOWUP_EXCEEDED` code 그대로 (FE 호환성). Resume 전용 별도 코드 도입 안 함 |
| `backend/src/main/resources/application.yml` | **수정**. `rehearse.interview.policy.standard.max-follow-up-rounds: 2` 추가 |
| `backend/src/test/java/com/rehearse/api/domain/interview/policy/StandardFollowUpPolicyTest.java` | 신규. 0/1/2/3턴 시나리오 |
| `backend/src/test/java/com/rehearse/api/domain/interview/policy/ResumeTrackPolicySkeletonTest.java` | 신규. 7턴 상한 하드 리밋 검증 |
| `backend/src/test/java/com/rehearse/api/domain/interview/policy/InterviewTurnPolicyResolverTest.java` | 신규. resumeSkeletonId 유/무별 라우팅 |
| `backend/src/test/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandlerTest.java` | **수정**. `loadFollowUpContext_maxFollowUpExceeded` 를 정책 Mock 기반으로 재작성. 기존 검증 행위 유지(회귀 0) |

## 상세

### InterviewTurnPolicy 인터페이스
```java
public interface InterviewTurnPolicy {
    InterviewTrack getTrack();

    /** 다음 턴 수용 가능한지. 불가 시 BusinessException throw. */
    void assertCanContinue(Interview interview, QuestionSet questionSet);
}
```

### StandardFollowUpPolicy (CS/Language — 행위 무변경)
```java
@Component
public class StandardFollowUpPolicy implements InterviewTurnPolicy {
    private final int maxRounds; // @Value("${rehearse.interview.policy.standard.max-follow-up-rounds:2}")

    public InterviewTrack getTrack() { return InterviewTrack.CS; }

    public void assertCanContinue(Interview interview, QuestionSet questionSet) {
        long count = questionSet.getQuestions().stream()
            .filter(q -> q.getQuestionType() == QuestionType.FOLLOWUP)
            .count();
        if (count >= maxRounds) {
            throw new BusinessException(QuestionErrorCode.MAX_FOLLOWUP_EXCEEDED);
        }
    }
}
```

### InterviewTurnPolicyResolver

`interview.type == RESUME_BASED` 조건만으로 단순 분기한다. Feature Flag runtime toggle은 사용하지 않는다.

```java
@Component
@RequiredArgsConstructor
public class InterviewTurnPolicyResolver {
    private final StandardFollowUpPolicy standard;
    private final ResumeTrackPolicy resume;

    public InterviewTurnPolicy resolve(Interview interview) {
        // interview.type == RESUME_BASED(resumeSkeletonId 존재) 조건만으로 분기
        // Feature Flag 없이 단일 경로로 동작
        return interview.getResumeSkeletonId() != null ? resume : standard;
    }
}
```

### FollowUpTransactionHandler 변경 (diff)
```diff
- private static final int MAX_FOLLOWUP_ROUNDS = 2;
+ private final InterviewTurnPolicyResolver turnPolicyResolver;

  public FollowUpContext loadFollowUpContext(...) {
      ...
-     validateFollowUpRoundLimit(questionSet);
+     turnPolicyResolver.resolve(interview).assertCanContinue(interview, questionSet);
      ...
  }
-
- private void validateFollowUpRoundLimit(QuestionSet questionSet) { ... }
```

### ResumeTrackPolicy (skeleton, 본체는 plan-07)
- 본 plan 에서는 **7턴 하드 상한**만 구현 (`FOLLOWUP count ≥ 7` → exception)
- 세부 판정(playground→interrogation 전환, chain level 진행)은 plan-07 `ChainStateTracker` 가 담당
- plan-07 구현 시 `ChainStateTracker` 를 이 policy 에 주입해 판정 위임

### Interview.getTrack() (파생, DB 무변경)
```java
public InterviewTrack getTrack() {
    return resumeSkeletonId != null ? InterviewTrack.RESUME : InterviewTrack.CS;
}
```
Language 트랙은 후속 plan 에서 별도 식별자 도입 시 분기 추가.

## plan-07 에 미치는 영향

- plan-07 "생성/수정 파일" 의 `FollowUpService` 수정 표현 → "`ResumeTrackPolicy` 에 `ChainStateTracker` 주입 및 판정 위임 구현" 으로 축소
- plan-07 의존성에 `plan-00f` 추가
- plan-07 의 `chain-max-depth`/`playground-max-turns` 설정 키는 유지하되 `ResumeTrackPolicy` 가 소비

## 담당 에이전트

- Implement: `backend-architect` — Policy/Resolver 설계, SRP/OCP 준수
- Implement: `backend` — 기존 Handler 리팩토링, 테스트 재작성
- Review: `architect-reviewer` — 계층 경계(Policy 는 domain 하위), `InterviewTrack` 도메인 개념 타당성
- Review: `code-reviewer` — 회귀 리스크, 에러 코드 호환성, 테스트 적정성

## 검증

1. `./gradlew test --tests "FollowUpTransactionHandlerTest"` — 기존 테스트 **전원 통과** (행위 무변경 증명)
2. `./gradlew test --tests "StandardFollowUpPolicyTest"` — 2턴 정책 단위 검증
3. `./gradlew test --tests "InterviewTurnPolicyResolverTest"` — 라우팅 검증
4. `./gradlew test --tests "ResumeTrackPolicySkeletonTest"` — 7턴 상한 검증
5. 기존 CS/Language 면접 E2E 스모크(수동 1회) — 2턴 초과 시 동일 에러 메시지/code 반환 (FE 영향 0)
6. `application.yml` 의 `max-follow-up-rounds` 를 3으로 변경 시 CS 트랙 3턴 허용 (튜닝 가능성 증명)
7. plan-07 착수 시 `ResumeTrackPolicy` 에 `ChainStateTracker` 주입만 하면 되도록 skeleton 준비됨
8. `progress.md` 에 plan-00f → Completed, plan-07 의존성에 00f 추가

## Out of Scope

- Language 트랙 정책 세분화 (현재 CS 와 동일 정책 공유, 향후 `LanguageTrackPolicy` 도입)
- Policy 의 DB 동적 설정 (YAML + Spring Config 수준으로 충분)
- `MAX_FOLLOWUP_EXCEEDED` 에러 코드 분리 (FE 호환성 위해 단일 코드 유지)
