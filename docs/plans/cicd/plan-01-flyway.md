# Plan 01: Flyway 도입 + 초기 마이그레이션 + dev 프로필

> 상태: Completed
> 작성일: 2026-03-16

## Why

H2 인메모리(dev)에서 MySQL(운영)으로 전환 시 스키마 불일치 위험 제거.
Flyway로 버전 관리된 마이그레이션을 적용하여 안정적인 DB 관리 확보.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/build.gradle.kts` | Flyway 의존성 추가 |
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | ERD 9개 테이블 DDL |
| `backend/src/main/resources/application-dev.yml` | 개발 서버 프로필 |

## 상세

- Flyway 의존성: `flyway-core` + `flyway-mysql`
- V1 마이그레이션: ERD 기준 9개 테이블 + FK + 인덱스
- dev 프로필: MySQL + validate + Flyway enabled + baseline-on-migrate

## 담당 에이전트
- Implement: `backend`
- Review: `architect-reviewer`

## 검증
- Backend 시작 로그에서 `Successfully applied N migrations` 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
