# Plan 08: 부하테스트 결과 문서화

> 상태: Completed
> 작성일: 2026-03-23

## Why

부하테스트 결과를 체계적으로 기록하여 VT 마이그레이션의 효과를 증명하고, 향후 성능 기준선(baseline)으로 활용한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/plans/virtual-thread-migration/load-test-results.md` | 부하테스트 결과 보고서 |

## 문서 구조

```markdown
# Virtual Thread Migration — 부하테스트 결과 보고서

> 테스트일: YYYY-MM-DD

## 1. 테스트 환경

| 항목 | 값 |
|------|-----|
| OS | macOS / Darwin |
| JVM | Java 21 |
| Spring Boot | 3.4.3 |
| HikariCP | 6.2.1 |
| DB | H2 / Docker MySQL |
| 외부 API | WireMock stub (Claude 2s, Whisper 1s) |
| HikariCP pool-size | 10 |

## 2. 시나리오 A: 트랜잭션 분리 전후

| 지표 | 분리 전 | 분리 후 | 개선율 |
|------|---------|---------|--------|
| 커넥션 평균 점유 시간 | ? | ? | ? |
| 동시 10명 p95 | ? | ? | ? |
| 동시 20명 p95 | ? | ? | ? |
| 동시 50명 p95 | ? | ? | ? |
| 동시 50명 에러율 | ? | ? | ? |
| HikariCP max active | ? | ? | ? |

## 3. 시나리오 B: VT 도입 전후

| 지표 | Platform Thread | Virtual Thread | 개선율 |
|------|----------------|----------------|--------|
| 동시 10명 p95 | ? | ? | ? |
| 동시 50명 p95 | ? | ? | ? |
| 동시 100명 p95 | ? | ? | ? |
| 동시 200명 p95 | ? | ? | ? |
| 동시 200명 에러율 | ? | ? | ? |
| JVM 스레드 수 (동시 200명) | ? | ? | ? |
| 메모리 사용량 | ? | ? | ? |

## 4. 시나리오 C: RateLimiter

| 지표 | 값 |
|------|-----|
| 요청 속도 | 50 req/s |
| 429 응답 비율 | ? |
| 정상 처리 비율 | ? |
| 평균 대기 시간 | ? |

## 5. 결론 및 권장사항

- 성능 개선 요약
- 운영 환경 적용 시 주의사항
- HikariCP pool-size 권장값
- RateLimiter 한도 조정 가이드
```

## 담당 에이전트

- Implement: `backend` — 결과 데이터 수집 및 문서 작성
- Review: `architect-reviewer` — 결론 타당성 검증

## 검증

- 모든 시나리오(A/B/C) 결과 테이블 채워짐 확인
- 결론 및 권장사항 작성 완료
- `progress.md` 상태 업데이트 (Task 8 → Completed)
