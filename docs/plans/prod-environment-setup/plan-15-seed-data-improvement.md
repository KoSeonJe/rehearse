# Task 15: 질문 풀 시드 데이터 prod 반영

> **상태**: Draft
> **의존**: Task 12 (Flyway 최초 마이그레이션 — `tts_content` 컬럼 존재 필수)
> **담당**: `backend` (실행) / `devops` (검증)

## Why

시드 데이터(`model_answer` 퀄리티 개선 + `tts_content` 채움)는 이미 로컬에서 개선 완료되어 `backend/src/main/resources/db/seed/*.sql`에 반영되어 있다. 이제 운영 DB에 한 번만 적재하면 된다.

이전에 호스트에서 `mysql` 클라이언트로 직접 실행했을 때 **이중 인코딩(UTF-8 → latin1 → UTF-8)** 문제로 한글이 깨진 이력이 있어, 이번에는 **scp로 EC2에 전송 → docker 컨테이너 내부에서 실행**하는 방식으로 한글 깨짐을 원천 차단한다.

## Goal

- `question_pool` 테이블에 시드 SQL 18개 파일 전부 적재
- 한글/특수문자 인코딩 정상 (mojibake 0건)
- `tts_content`, `model_answer` 모든 활성 질문에 채워져 있음

## 대상 파일

`backend/src/main/resources/db/seed/` 하위 18개 SQL:

- backend-{java-spring, kotlin-spring, node-nestjs, python-django}.sql
- frontend-{react-ts, vue-ts}.sql
- fullstack-react-spring.sql
- devops-aws-k8s.sql
- batch.sql
- behavioral-{junior, mid, senior}.sql
- cs-fundamental-{junior, mid, senior}.sql
- system-design-{junior, mid, senior}.sql

## 실행 절차

### Step 1: Flyway 마이그레이션 선행 확인

Task 12 완료 상태 (`question_pool.tts_content` 컬럼 존재) 확인.

```bash
# EC2 내부 / 또는 컨테이너 진입 후
docker exec -it <mysql-container> mysql -u<user> -p<pass> <db> \
  -e "SHOW COLUMNS FROM question_pool LIKE 'tts_content';"
```

### Step 2: 백업

반영 전 `question_pool` 전체 덤프 저장.

```bash
docker exec <mysql-container> mysqldump -u<user> -p<pass> <db> question_pool \
  > ~/backup/question_pool_$(date +%Y%m%d_%H%M%S).sql
```

### Step 3: scp로 시드 SQL 업로드

로컬에서 EC2로 파일 전송 (호스트 측에서 로케일 변환이 끼어들지 않도록 바이너리 전송).

```bash
# 로컬
scp -i <key.pem> backend/src/main/resources/db/seed/*.sql \
    ec2-user@<EC2-IP>:~/seed/
```

### Step 4: 컨테이너 내부로 복사 후 실행

호스트 `mysql` 클라이언트를 거치지 않고, MySQL 컨테이너 내부에서 직접 실행하여 이중 인코딩 방지.

```bash
# EC2 SSH 접속 후
docker cp ~/seed/. <mysql-container>:/tmp/seed/

# 컨테이너 내부에서 실행 (UTF-8 강제)
docker exec -i <mysql-container> bash -c '
  for f in /tmp/seed/*.sql; do
    echo "=== loading $f ==="
    mysql --default-character-set=utf8mb4 -u<user> -p<pass> <db> < "$f" || exit 1
  done
'
```

> 키 포인트:
> - `--default-character-set=utf8mb4` 명시
> - 컨테이너 내부 실행 → 호스트 locale 영향 제거
> - 실패 시 즉시 중단 (`|| exit 1`)

### Step 5: 검증

```sql
-- 개수 및 채움률
SELECT COUNT(*) AS total,
       SUM(CASE WHEN tts_content IS NOT NULL AND tts_content <> '' THEN 1 ELSE 0 END) AS has_tts,
       SUM(CASE WHEN CHAR_LENGTH(model_answer) >= 50 THEN 1 ELSE 0 END) AS good_answer
FROM question_pool
WHERE is_active = 1;

-- 인코딩 샘플 확인 (한글 + 특수문자 혼용 질문)
SELECT id, content, tts_content
FROM question_pool
WHERE content LIKE '%@Transactional%' OR content LIKE '%Require_new%'
LIMIT 10;
```

육안으로 한글/기호가 정상 출력되는지 확인.

## 롤백

- 문제 발생 시 Step 2 백업 SQL로 복원:
  ```bash
  docker exec -i <mysql-container> mysql -u<user> -p<pass> <db> \
    < ~/backup/question_pool_<timestamp>.sql
  ```

## 체크리스트

- [ ] Task 12 완료 (`tts_content` 컬럼 존재)
- [ ] `question_pool` 백업 생성
- [ ] scp로 시드 파일 18개 전송 완료
- [ ] `docker cp` + 컨테이너 내부 mysql 실행으로 적재
- [ ] 검증 쿼리로 채움률 100% 확인
- [ ] 한글/특수문자 mojibake 없음 (샘플 10건 육안 확인)
