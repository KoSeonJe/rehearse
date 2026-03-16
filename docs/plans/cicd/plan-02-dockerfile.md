# Plan 02: Backend Dockerfile

> 상태: Completed
> 작성일: 2026-03-16

## Why

EC2 t3.micro(1GB RAM)에서 직접 빌드 시 메모리 부족 위험.
GitHub Actions에서 빌드 → ECR push → EC2에서 pull만 하는 전략.

## 생성 파일

| 파일 | 설명 |
|------|------|
| `backend/Dockerfile` | Multi-stage 빌드 (JDK 21 → JRE 21) |
| `backend/.dockerignore` | 빌드 불필요 파일 제외 |

## 상세

- Stage 1 (builder): temurin:21-jdk, Gradle 의존성 캐싱 → bootJar
- Stage 2 (runtime): temurin:21-jre, non-root 사용자
- 이미지 크기 최적화: JRE만 포함

## 담당 에이전트
- Implement: `devops`
- Review: `architect-reviewer`

## 검증
- `docker build` 성공 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
