# Product Spec — interview 도메인 발견 이슈 통합 (보안/안정성/cleanup 8건)

> Issue: #404
> 작성일: 2026-05-06

---

## Why / Background

interview 도메인 정책 문서 작성 중 코드 추적으로 발견된 보안 / 안정성 / cleanup 이슈 8건. 정책 결정 확정된 코드-정책 갭 (#1, #3) 포함. 단일 Epic 묶어 우선순위·진행 추적.

- 현재 상태: 8건 발견 완료. PR 미진행.
- 문제점:
  - 보안 — `Interview.userId NULL` row 권한 우회 (#1), `csSubTopics` 자유문자열 prompt 삽입 (#2)
  - validation — audio mime/길이/매직바이트 미검증 (#3)
  - 안정성 — `retryQuestionGeneration` 무한 재시도 (#4), RESUME_BASED retry 무한 RESUME_PLAN_NOT_READY (#5)
  - 운영 — `InterviewPlan` 갱신 경로 부재 (#7)
  - cleanup — `CANNOT_DELETE_COMPLETED` dead code (#8)
- 동기: 보안 노출 + AI 비용 폭증 + 상태 무결성 위험. P1 (이번 주).

## Goal

- [ ] 7개 항목 모두 코드 반영 + 회귀 테스트 포함된 단일 PR 머지 (#6 제외)
- [ ] 보안 항목 (#1, #2, #3) 회귀 테스트로 우회 차단 검증
- [ ] 안정성 항목 (#4, #5) 재시도 제한 / 입력 보존 검증

## 수용 기준

- [ ] #1 `Interview.validateOwner` — userId NULL row 조회/접근 차단 (회귀 테스트 포함)
- [ ] #2 `csSubTopics` enum / whitelist 도입, 자유 문자열 prompt 주입 차단
- [ ] #3 audio upload — mime whitelist (`audio/webm|mp4|mpeg|wav`) + 길이 cap (5분) + 매직바이트 1차 검증
- [ ] #4 `retryQuestionGeneration` retry counter / cooldown 도입, 무한 재시도 차단
- [ ] #5 RESUME_BASED 첫 생성 실패 후 retry 재현 시나리오 수정 + 회귀 테스트
- [ ] #7 `InterviewPlan` replan API 추가 — 운영 SQL 없이 정정 가능
- [ ] #8 `InterviewErrorCode.CANNOT_DELETE_COMPLETED` dead code 제거
- [ ] 7건 일괄 PR 머지 (#6 제외), BE CI 통과

## 비스코프

- userId NULL 레거시 row 마이그레이션 정책 — 별도 사용자 결정
- #6 `Interview.@Version` 낙관락 도입 — 별도 결정 후 진행 (이번 작업 보류)
- 상태 전이 락 정책 (낙관 vs 비관) — 별도 결정
- soft-delete 도입 여부 — 별도
- 통계 시간대 정책 (Asia/Seoul vs 사용자 타임존) — 별도
- AI 메트릭 알람 임계 정책 — 별도

## 참고

- 관련 Issue: #404
- 발견 plan: `docs/plans/00x-domain-docs-infra/`
- 도메인 문서: `docs/domain/interview/`
