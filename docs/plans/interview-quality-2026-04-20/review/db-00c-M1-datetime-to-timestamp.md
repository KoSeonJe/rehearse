---
id: db-00c-M1-datetime-to-timestamp
severity: major
category: database
plan: plan-00c-session-state-persistence
reviewer: database-optimization
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# `DATETIME` 대신 `TIMESTAMP` 사용 — UTC 보장

## 문제

V24~V27 모두 `created_at DATETIME NOT NULL` 로 선언. `DATETIME` 은 타임존 정보를 저장하지 않아 서버 타임존이 바뀌면 값 해석이 달라진다.

## 원인

plan-00c 명세가 `DATETIME NOT NULL` 로 기술되어 그대로 반영. MySQL 8.0 의 `TIMESTAMP` 는 UTC 로 저장하고 세션 타임존으로 변환하지만 `DATETIME` 은 저장된 문자값 그대로 해석 — 멀티 리전 / 서머타임 전환 시 불일치.

## 발생 상황

- **언제**:
  - EC2 인스턴스 타임존이 UTC 가 아닌 환경에서 배포
  - `resume_skeleton` 의 `file_hash` 캐시 hit 검사 시 시각 비교 로직이 들어가면
  - AWS 리전 간 read replica 설정 시
- **누가**: 운영팀, 데이터 이관 담당
- **파장**: 시각 기반 조인/정렬이 리전 경계에서 틀어짐. 감사 추적(audit trail) 시각 불일치

## 해결 방법

V24~V27 의 `created_at` 을 `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` 로 변경. JPA `@CreatedDate` 없이도 INSERT 시 자동 채움.

```sql
-- Before (V24~V27 공통)
created_at DATETIME NOT NULL

-- After
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

**대안(기각)**: `DATETIME` 유지 + JPA Entity 에서 `@CreatedDate` 로 UTC 강제. 애플리케이션 layer 에만 의존 → DB 직접 INSERT 시 (마이그레이션 스크립트 등) 여전히 위험.

**참고**: MySQL 8.0 의 `TIMESTAMP` 범위는 `1970-01-01 ~ 2038-01-19` — 피드백 데이터 유지 기간 고려 시 문제없음 (30+ 년 마진).

## 결과

- 수정: V24~V27 전부 `created_at` 컬럼
- Entity 에 `@CreatedDate` 또는 `@Column(insertable=false, updatable=false)` 추가 여부는 본 plan 범위 아님 (후속 plan 에서 Entity 도입 시 결정)
- 검증: `grep -r "DATETIME NOT NULL" backend/src/main/resources/db/migration/V2[4-7]*` → 0건
- 기존 운영 DB 미배포 — ALTER 불필요, 파일 수정만
