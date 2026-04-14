# Plan 12: prod DB Flyway 최초 마이그레이션

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 02 (EC2), Plan 06 (docker-compose.prod.yml), Plan 01 (application-prod.yml)

## Why

prod EC2 최초 기동 시 `db` 컨테이너는 빈 MySQL이다. Spring Boot backend가 기동되면서 Flyway가 `db/migration/V1__*.sql`부터 최신 `V*__*.sql`까지 순차 적용해야 prod DB 스키마가 완성된다. 두 가지 리스크를 관리해야 한다:

1. **`baseline-on-migrate: true` 오용** — 이 옵션은 이미 데이터가 있는 DB에 도입할 때 기존 스키마를 baseline으로 찍는 용도다. **빈 prod DB에서 true로 부팅하면 아무 문제 없다** (baseline 필요 없으므로 그냥 V1부터 시작). 하지만 실수로 `baseline-version`을 함께 설정하면 V1을 건너뛸 수 있다 → baseline-version 미설정 확인 필수.
2. **실패 시 롤백 어려움** — Flyway Community는 `undo` 미지원. 마이그레이션이 중간에 실패하면 수동 정리 + 신규 `V__fix` 마이그레이션 추가로 롤포워드.

dev에서 이미 검증된 마이그레이션이므로 prod 적용 리스크는 낮지만, 최초 기동은 반드시 **수동 감시** 하에 진행한다.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `backend/src/main/resources/db/migration/*.sql` | **변경 없음** (dev와 동일 파일 사용) |
| `backend/src/main/resources/application-prod.yml` | `baseline-on-migrate: true` 포함 확인 (plan-01에서 설정) |
| `docs/plans/prod-environment-setup/plan-13-cutover-smoke-test.md` | Flyway 최초 기동 체크리스트 포함 |
| `docs/guides/prod-db-backup-restore.md` | 신규 작성 (백업/복구 절차) |

## 상세

### 사전 확인

```bash
# 1. 마이그레이션 파일 목록 확인
ls backend/src/main/resources/db/migration/
# V1__init_schema.sql
# V2__...
# ...
# V14__analysis_state_redesign.sql (또는 최신)

# 2. 각 V*.sql이 idempotent하지 않아도 되지만 실수 방지 검토
grep -l "CREATE TABLE IF NOT EXISTS" backend/src/main/resources/db/migration/*.sql
```

### 최초 기동 순서 (plan-13 컷오버 중)

```bash
# EC2 ssh 접속
ssh -i ~/.ssh/rehearse-prod-key.pem ubuntu@<prod-EIP>
cd ~/rehearse/backend

# 1. .env 확인 및 셸 환경에 로드 (후속 docker exec에서 사용)
cat .env | grep -E '^(DB_|SPRING_PROFILES)'
set -a; source .env; set +a
# SPRING_PROFILES_ACTIVE는 compose에서 prod로 하드코딩이라 .env에 없어도 OK
# DB_USERNAME, DB_PASSWORD, DB_ROOT_PASSWORD 값이 쉘 환경변수에 로드됐는지:
: "${DB_ROOT_PASSWORD:?DB_ROOT_PASSWORD not set}"

# 2. DB 컨테이너만 먼저 기동 (backend 부팅 전)
docker compose --env-file .env up -d db

# 3. MySQL 초기화 완료 대기 (healthcheck 통과까지)
docker compose --env-file .env ps db
# STATUS "healthy" 확인 (~30초)

# 4. 빈 DB 상태 검증
docker exec rehearse-db mysql -u root -p"$DB_ROOT_PASSWORD" -e "SHOW DATABASES;"
# rehearse 데이터베이스 존재 확인
docker exec rehearse-db mysql -u root -p"$DB_ROOT_PASSWORD" rehearse -e "SHOW TABLES;"
# Empty set 확인 (테이블 없음)

# 5. backend 기동 → Flyway 자동 실행
docker compose --env-file .env up -d backend

# 6. Flyway 로그 실시간 모니터링
docker compose --env-file .env logs -f backend | grep -i flyway
# 기대 출력:
# Flyway Community Edition x.y.z
# Database: jdbc:mysql://db:3306/rehearse
# Successfully validated N migrations
# Creating schema history table `rehearse`.`flyway_schema_history`
# Current version of schema `rehearse`: << Empty Schema >>
# Migrating schema `rehearse` to version "1 - init_schema"
# ...
# Successfully applied N migrations to schema `rehearse`, now at version v14

# 7. Health check 통과 확인
docker exec rehearse-backend curl -sf http://localhost:8080/actuator/health
# {"status":"UP"}

# 8. schema history 검증
docker exec rehearse-db mysql -u root -p"$DB_ROOT_PASSWORD" rehearse \
  -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
# 모든 success = 1 확인
```

### 실패 시 복구 절차

**Case 1: 마이그레이션 일부 실패**
1. `flyway_schema_history`에서 `success=0` 레코드 확인
2. 해당 V 번호의 SQL을 수동 원복 (DROP TABLE 등)
3. `flyway_schema_history`에서 실패 row 삭제
4. 코드 수정 후 재배포 (`docker compose pull backend && up -d`)
5. Flyway 재시도

**Case 2: DB 전체 초기화**
```bash
docker compose --env-file .env down backend db
docker volume rm rehearse-backend_mysql_data_prod  # prod 볼륨명 (compose 프로젝트명에 따라 상이)
docker compose --env-file .env up -d db
# 다시 4번부터
```

**Case 3: `V1__init_schema.sql` 자체 실패**
- 가장 가능성 낮음 (dev에서 수차례 검증됨)
- 발생 시 IDE 문제보다는 MySQL 버전 불일치 가능 → `mysql:8.0` 태그 재확인

### 첫 backup 스냅샷 생성 (성공 직후)

```bash
# Flyway 완료 직후 baseline 백업
docker exec rehearse-db mysqldump -u root -p"$DB_ROOT_PASSWORD" --single-transaction rehearse \
  > ~/backups/rehearse-prod-initial-$(date +%Y%m%d-%H%M%S).sql

# S3로 복사 (plan-14 runbook의 일일 백업 자동화 전에 수동 1회)
aws s3 cp ~/backups/rehearse-prod-initial-*.sql s3://rehearse-videos-prod/db-backups/ \
  --storage-class STANDARD_IA
```

### 시드 데이터 적용 (Flyway 성공 직후)

Flyway 마이그레이션 완료 후, question_pool 시드 데이터를 수동 적용한다.

> **`--default-character-set=utf8mb4` 필수** — 누락 시 한글 이중 인코딩 발생 (dev 환경에서 실제 발생했던 이슈, `.omc/plans/humming-rolling-moon.md` 참고)

```bash
# 1. 로컬에서 EC2로 시드 파일 전송
scp -i ~/.ssh/rehearse-prod-key.pem -r backend/src/main/resources/db/seed/ ubuntu@<prod-EIP>:/tmp/seed/

# 2. 시드 실행 (utf8mb4 필수)
ssh -i ~/.ssh/rehearse-prod-key.pem ubuntu@<prod-EIP>
for f in /tmp/seed/*.sql; do
  [[ "$(basename $f)" == "README.md" ]] && continue
  echo "Loading $(basename $f)..."
  docker exec -i rehearse-db mysql -u rehearse -p"$DB_PASSWORD" --default-character-set=utf8mb4 rehearse < "$f"
done

# 3. 검증
docker exec rehearse-db mysql -u rehearse -p"$DB_PASSWORD" --default-character-set=utf8mb4 rehearse \
  -e "SELECT COUNT(*) FROM question_pool;"
# 기대: 1438

# 4. 인코딩 검증 (한글 정상 표시 확인)
docker exec rehearse-db mysql -u rehearse -p"$DB_PASSWORD" --default-character-set=utf8mb4 rehearse \
  -e "SELECT id, category, LEFT(content, 40) FROM question_pool WHERE cache_key = 'JUNIOR:CS_FUNDAMENTAL' LIMIT 3;"
# category: 자료구조, 운영체제 등 한글 정상 표시

# 5. 임시 파일 정리
rm -rf /tmp/seed/
```

### 향후 마이그레이션 운영 원칙

- **신규 `V*__*.sql`은 반드시 dev에서 검증 후 develop→main merge** (CI `Backend CI` test가 테스트 DB에 적용)
- **Destructive 마이그레이션** (DROP COLUMN, RENAME TABLE 등)은 2단계 분할 권장: V(n)에서 추가 → 코드 배포 → V(n+1)에서 제거
- Flyway repair는 데이터 파괴 가능 → 신중
- prod 마이그레이션 전 `mysqldump` 백업 자동화는 plan-14에서 설계

## 담당 에이전트

- Implement: `backend` — Flyway 실행 감시, 로그 검증
- Review: `architect-reviewer` — 롤백 전략, baseline 설정 의미

## 검증

- `flyway_schema_history` 테이블에 V1~최신 전부 `success=1` 존재
- `SHOW TABLES;` 결과가 dev와 동일 (동일 마이그레이션 세트)
- backend `/actuator/health` → `{"status":"UP"}`
- backend 로그에 Flyway 에러 없음
- 최초 백업 `.sql` 파일 S3 업로드 확인
- `progress.md` Task 12 → Completed
