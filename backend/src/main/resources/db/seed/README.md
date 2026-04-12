# Question Pool Seed Data

면접 질문 풀 시드 데이터. Flyway가 아닌 수동 실행으로 적용합니다.

## 적용 방법

```bash
# 전체 적용 (--default-character-set=utf8mb4 필수 — 누락 시 한글 이중 인코딩 발생)
for f in backend/src/main/resources/db/seed/*.sql; do
  mysql --default-character-set=utf8mb4 -u root -p rehearse_dev < "$f"
done

# 개별 적용
mysql --default-character-set=utf8mb4 -u root -p rehearse_dev < backend/src/main/resources/db/seed/cs-fundamental-junior.sql

# Docker 환경 (EC2 등)
docker exec -i rehearse-db mysql -u rehearse -p'<password>' --default-character-set=utf8mb4 rehearse < seed-file.sql
```

> `INSERT IGNORE`를 사용하므로 중복 실행 시 에러 없이 스킵됩니다.

## 파일 구조

### Position-Agnostic (cache_key: `{Level}:{Type}`)

| 파일 | cache_key | 문항수 |
|------|-----------|--------|
| `cs-fundamental-junior.sql` | `JUNIOR:CS_FUNDAMENTAL` | ~120 |
| `cs-fundamental-mid.sql` | `MID:CS_FUNDAMENTAL` | ~120 |
| `cs-fundamental-senior.sql` | `SENIOR:CS_FUNDAMENTAL` | ~120 |
| `behavioral-junior.sql` | `JUNIOR:BEHAVIORAL` | 30 |
| `behavioral-mid.sql` | `MID:BEHAVIORAL` | 30 |
| `behavioral-senior.sql` | `SENIOR:BEHAVIORAL` | 30 |
| `system-design-junior.sql` | `JUNIOR:SYSTEM_DESIGN` | 30 |
| `system-design-mid.sql` | `MID:SYSTEM_DESIGN` | 30 |
| `system-design-senior.sql` | `SENIOR:SYSTEM_DESIGN` | 30 |

### Position-Specific (cache_key: `{Position}:{Level}:{TechStack}:{Type}`)

| 파일 | 스택 | 문항수 |
|------|------|--------|
| `backend-java-spring.sql` | JAVA_SPRING | ~90 |
| `backend-python-django.sql` | PYTHON_DJANGO | ~90 |
| `backend-node-nestjs.sql` | NODE_NESTJS | ~90 |
| `backend-kotlin-spring.sql` | KOTLIN_SPRING | ~90 |
| `frontend-react-ts.sql` | REACT_TS (LANGUAGE_FRAMEWORK + UI_FRAMEWORK) | ~180 |
| `frontend-vue-ts.sql` | VUE_TS | ~90 |
| `devops-aws-k8s.sql` | AWS_K8S (INFRA_CICD + CLOUD) | ~180 |
| `fullstack-react-spring.sql` | REACT_SPRING | ~90 |

## 스키마 (V20 이후)

```sql
INSERT IGNORE INTO question_pool
  (cache_key, content, category, model_answer, reference_type, is_active, created_at)
VALUES ('{cache_key}', '{질문}', '{카테고리}', '{모범답변}', 'MODEL_ANSWER', TRUE, NOW());
```

- `reference_type`: `MODEL_ANSWER` (기술 질문) 또는 `GUIDE` (행동 면접, STAR 가이드)
