# Plan 03: Docker Compose

> 상태: Completed
> 작성일: 2026-03-16

## Why

EC2에서 backend + MySQL을 하나의 명령으로 관리하기 위한 오케스트레이션.

## 생성 파일

| 파일 | 설명 |
|------|------|
| `backend/docker-compose.yml` | backend + db 서비스 정의 |

## 상세

- `db`: MySQL 8.0, 헬스체크, 볼륨 마운트
- `backend`: ECR 이미지 pull, 80:8080 포트 매핑, db 의존
- 환경변수: `.env` 파일로 주입

## 담당 에이전트
- Implement: `devops`
- Review: `architect-reviewer`

## 검증
- `docker compose config` 문법 검증
- `progress.md` 상태 업데이트 (Task 3 → Completed)
