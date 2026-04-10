# Question Pool 데이터 워밍 + 사용자별 중복 방지 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-10

## Why

### 1. Why? — 어떤 문제를 해결하는가?

현재 `question_pool`은 사용자가 면접을 시작할 때 AI(Claude API)로 lazy 생성된다. 이로 인해:
- **첫 사용자 경험 저하**: 질문 생성 대기 시간 발생 (3-10초)
- **AI 비용 비효율**: 매 면접마다 반복적 API 호출
- **품질 불균일**: 실시간 생성 질문의 품질 편차

또한 현재 시스템은 **사용자별 기출 질문 추적이 없어**, 동일 사용자가 반복 면접 시 같은 질문을 받을 수 있다.

### 2. Goal — 구체적 결과물과 성공 기준

1. **시드 데이터 적재**: CS 4분야 × 30개 × 3레벨 + 프레임워크 9스택 × 30개 × 3레벨 + Behavioral/System Design = 총 ~1,350개
2. **사용자별 중복 방지**: 동일 사용자가 이전에 받은 질문을 다시 받지 않도록 필터링
3. **온디맨드 풀 확장**: 미사용 질문이 부족해지면 AI 생성으로 풀 자동 확장
4. **성공 기준**: 시드 적재 후 면접 생성 시 AI 호출 없이 즉시 질문 제공 (pool hit rate 90%+)

### 3. Evidence — 근거 데이터

웹 리서치를 통해 실제 한국 개발자 기술 면접 빈출 질문을 수집 (출처 포함):
- **CS 기본**: InterviewBit, GeeksforGeeks, gyoogle/tech-interview-for-developer, JaeYeopHan/Interview_Question_for_Beginner 등
- **프레임워크**: Guru99, Velog 면접 정리, F-lab, 각 기술 공식 문서
- **Behavioral**: Tech Interview Handbook, Amazon Leadership Principles, 항해99, 원티드 면접 가이드

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 선택 | 대안 | 포기한 이유 |
|------|------|------------|
| .sql 파일로 시드 | Flyway 마이그레이션 | 시드 데이터는 환경별 선택 적용이 필요하므로 Flyway 강제 실행보다 유연한 방식 선호 |
| 사용자별 중복 방지 | 주기적 스케줄러 갱신 | 스케줄러는 불필요한 API 비용 발생, 사용자 관점에서 중복 방지가 본질적 해결책 |
| 기본 + 인기 스택 시딩 | 전체 17개 스택 | 비주류 스택은 사용 빈도 낮아 lazy 생성으로 충분 |
| 기존 테이블 관계 활용 | 새 user_question_history 테이블 | interview → question_set → question → question_pool 관계로 이미 추적 가능 |

---

## 아키텍처 / 설계

### 현재 캐시 키 구조

```
Position-Agnostic (CS_FUNDAMENTAL, BEHAVIORAL, SYSTEM_DESIGN):
  캐시키 = {Level}:{Type}
  예: JUNIOR:CS_FUNDAMENTAL, MID:BEHAVIORAL

Position-Specific (LANGUAGE_FRAMEWORK, UI_FRAMEWORK, INFRA_CICD, CLOUD 등):
  캐시키 = {Position}:{Level}:{TechStack}:{Type}
  예: BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK
```

### 사용자별 중복 방지 플로우

```
[면접 생성 요청 (userId 포함)]
    ↓
[CacheableQuestionProvider.provide()]
    ↓
[1] question_pool에서 cacheKey로 활성 질문 전체 조회
    ↓
[2] 해당 userId의 기출 question_pool_id 조회 (기존 테이블 JOIN)
    ↓
[3] 풀에서 기출 제외 → 미사용 후보
    ↓
[4-A] 미사용 후보 >= requiredCount → 카테고리 분배 선택 → 완료
[4-B] 미사용 후보 < requiredCount → AI 생성으로 풀 확장 → 선택 → 완료
```

### 기출 조회 쿼리 (새 테이블 불필요)

```sql
SELECT DISTINCT q.question_pool_id 
FROM question q 
JOIN question_set qs ON q.question_set_id = qs.id
JOIN interview i ON qs.interview_id = i.id
WHERE i.user_id = :userId AND q.question_pool_id IS NOT NULL
```

---

## Scope

### In (포함)
- CS 4분야(자료구조/운영체제/네트워크/데이터베이스) × 30개 × 3레벨 = 360개
- Behavioral 5카테고리 × 6개 × 3레벨 = 90개
- System Design × 30개 × 3레벨 = 90개
- 프레임워크 9스택(JAVA_SPRING, PYTHON_DJANGO, NODE_NESTJS, KOTLIN_SPRING, REACT_TS, VUE_TS, AWS_K8S(INFRA_CICD), AWS_K8S(CLOUD), REACT_SPRING(FULLSTACK_STACK)) × 30개 × 3레벨 = 810개
- 사용자별 중복 방지 로직
- 미사용 질문 부족 시 온디맨드 풀 확장

### Out (제외)
- 비주류 TechStack 시딩 (GO, SVELTE, ANGULAR, FLINK, DBT_SNOWFLAKE 등) → lazy 생성 유지
- RESUME_BASED 질문 (항상 FRESH 생성)
- UI_FRAMEWORK(VUE_TS), BROWSER_PERFORMANCE 등 소수 타입 → lazy 생성 유지
- 질문 품질 자동 평가 시스템 (qualityScore 기반 감쇠)
- 주기적 스케줄러

---

## 제약조건 / 환경

- **Pool soft cap**: cacheKey당 200개 (QuestionPoolService.POOL_SOFT_CAP)
- **Pool 충분성**: activeCount >= requiredCount * 3 (기존 로직)
- **Flyway 마이그레이션**: 다음 버전 V20 (스키마 변경 시 사용, V19는 service_feedback에서 사용 중)
- **기존 시드**: `db/seed/junior-cs-fundamental.sql` (60개), `db/seed/junior-behavioral.sql` (20개) 존재 → 통합 필요
- **질문 데이터 형식**: content, category, modelAnswer, referenceType 필수 (evaluationCriteria, followUpStrategy, questionOrder, qualityScore는 Plan 06에서 삭제됨)
