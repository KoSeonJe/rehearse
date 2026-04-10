# Plan 05: 기존 시드 파일 정리 및 적용 방법 결정

> 상태: Draft
> 작성일: 2026-04-10

## Why

기존 `db/seed/` 디렉토리에 `junior-cs-fundamental.sql`(60개), `junior-behavioral.sql`(20개)이 있지만 Flyway에서 자동 실행되지 않고, 네이밍도 새 파일들과 불일치한다. 통합 정리하고 적용 방법을 확립한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/seed/junior-cs-fundamental.sql` | 삭제 (cs-fundamental-junior.sql로 통합) |
| `backend/src/main/resources/db/seed/junior-behavioral.sql` | 삭제 (behavioral-junior.sql로 통합) |
| `backend/src/main/resources/db/seed/README.md` | 시드 적용 가이드 문서 작성 |

## 상세

### 기존 파일 → 새 파일 매핑

| 기존 파일 | 새 파일 | 조치 |
|----------|--------|------|
| `junior-cs-fundamental.sql` (60개) | `cs-fundamental-junior.sql` (120개) | 기존 60개 포함 + 60개 추가 후 기존 파일 삭제 |
| `junior-behavioral.sql` (20개) | `behavioral-junior.sql` (30개) | 기존 20개 포함 + 10개 추가 후 기존 파일 삭제 |

### 시드 적용 방법

기존 패턴을 유지하여 **Flyway 자동 실행이 아닌 수동/스크립트 적용** 방식:

```bash
# 로컬 개발환경 적용 예시
mysql -u root -p rehearse_dev < backend/src/main/resources/db/seed/cs-fundamental-junior.sql
mysql -u root -p rehearse_dev < backend/src/main/resources/db/seed/behavioral-junior.sql
# ... 또는 일괄 적용 스크립트
```

### README.md 내용

```markdown
# Question Pool Seed Data

질문 풀 초기 데이터. Flyway 마이그레이션이 아니므로 수동 적용 필요.

## 적용 방법

### 전체 적용 (최초 설정)
for f in backend/src/main/resources/db/seed/*.sql; do
  mysql -u root -p rehearse_dev < "$f"
done

### 개별 적용
mysql -u root -p rehearse_dev < backend/src/main/resources/db/seed/{파일명}.sql

## 파일 목록
- cs-fundamental-{junior|mid|senior}.sql: CS 기본 (자료구조/운영체제/네트워크/DB)
- behavioral-{junior|mid|senior}.sql: 인성 면접
- system-design-{junior|mid|senior}.sql: 시스템 설계
- backend-java-spring.sql: Java/Spring 3레벨
- backend-python-django.sql: Python/Django 3레벨
- backend-node-nestjs.sql: Node.js/NestJS 3레벨
- backend-kotlin-spring.sql: Kotlin/Spring 3레벨
- frontend-react-ts.sql: React/TypeScript 3레벨 (LANGUAGE_FRAMEWORK + UI_FRAMEWORK)
- frontend-vue-ts.sql: Vue.js/TypeScript 3레벨
- devops-aws-k8s.sql: AWS/K8s 3레벨 (INFRA_CICD + CLOUD)
- fullstack-react-spring.sql: React+Spring 풀스택 3레벨

## 주의사항
- 모든 시드 SQL은 `INSERT IGNORE`를 사용하여 중복 실행 시 에러 방지
- 적용 순서 무관 (상호 의존성 없음)
- V19 마이그레이션(컬럼 삭제) 적용 후에 시드를 실행할 것
```

## 담당 에이전트

- Implement: `backend` — 파일 정리, README 작성
- Review: `code-reviewer` — 데이터 무결성, 기존 데이터 손실 없는지 확인

## 검증

- 기존 파일 삭제 후 새 파일로 대체 확인
- 기존 60 + 20 = 80개 질문이 새 파일에 모두 포함되어 있는지 diff 확인
- README.md 적용 명령어 테스트
- `progress.md` 상태 업데이트 (Task 5 → Completed)
