# Pipeline Resilience 스펙

## Status: In Progress

## Why
3개 외부 API(OpenAI, Claude)에 재시도 없이 단일 장애점 존재. 분산 환경(Lambda×N + 스케줄러)에서 상태 전이 race condition 가능. 파이프라인 안정성 확보 + 이력서 소재 확보가 목표.

## Phase 1: 안정성 기반 (Critical)
- Task 1: Lambda 내부 HTTP 재시도 데코레이터
- Task 2: 상태 전이 낙관적 잠금 (@Version)
- Task 3: OpenAI Rate Limit + STT 재시도

## Phase 2: 복원력 강화 (High)
- Task 4: Claude API 재시도 (Spring Retry)
- Task 5: File 재시도 API + Convert 멱등성
- Task 6: Correlation ID 전파
