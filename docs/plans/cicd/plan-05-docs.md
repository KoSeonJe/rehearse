# Plan 05: 배포 가이드 + 환경변수 예시

> 상태: Completed
> 작성일: 2026-03-16

## Why

AWS 리소스 생성, 시크릿 등록, EC2 초기 세팅 등 수동 작업 가이드 필요.

## 생성 파일

| 파일 | 설명 |
|------|------|
| `docs/guides/DEPLOYMENT.md` | 전체 배포 운영 가이드 |
| `.env.dev-server.example` | 서버 환경변수 템플릿 |

## 담당 에이전트
- Implement: `devops`

## 검증
- DEPLOYMENT.md 내 모든 섹션 작성 확인
- .env.dev-server.example 환경변수 빠짐없이 정의
- `progress.md` 상태 업데이트 (Task 6 → Completed)
