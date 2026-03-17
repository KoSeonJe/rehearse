# Plan 06: 커스텀 도메인 + HTTPS 설정

> 상태: In Progress (EC2 Nginx/HTTPS 코드 완료, EC2 수동 작업 필요)
> 작성일: 2026-03-17

## Why

프론트엔드(HTTPS/CloudFront)에서 백엔드(HTTP/EC2)로 API 호출 시 Mixed Content 에러 발생.
커스텀 도메인 + HTTPS 적용으로 해결하고, 서비스 URL도 정리한다.

## 도메인 계획

| 용도 | 도메인 | 연결 대상 |
|------|--------|-----------|
| 프론트엔드 (dev) | `dev.rehearse.co.kr` | CloudFront (`d2n8xljv54hfw0.cloudfront.net`) |
| 백엔드 API (dev) | `api-dev.rehearse.co.kr` | EC2 (`54.180.188.135`) |

## 작업 내용

### 1. 도메인 구매 (사용자)
- 가비아에서 `rehearse.co.kr` 구매

### 2. 가비아 DNS 레코드 추가 (사용자)

| 타입 | 호스트 | 값 | TTL |
|------|--------|-----|-----|
| A | `api-dev` | `54.180.188.135` | 3600 |
| CNAME | `dev` | `d2n8xljv54hfw0.cloudfront.net` | 3600 |

### 3. EC2: Nginx + Let's Encrypt (자동)
- Nginx 리버스 프록시 설치 (80 → 8080)
- Certbot으로 `api-dev.rehearse.co.kr` SSL 인증서 발급
- 보안 그룹에 443(HTTPS) 인바운드 추가

### 4. CloudFront: 커스텀 도메인 + ACM 인증서 (자동)
- ACM(us-east-1)에서 `dev.rehearse.co.kr` 인증서 발급
- CloudFront에 대체 도메인(CNAME) + 인증서 연결

### 5. 환경변수 업데이트 (자동)
- `CORS_ALLOWED_ORIGINS` → `https://dev.rehearse.co.kr`
- `VITE_API_URL` → `https://api-dev.rehearse.co.kr`
- GitHub Secrets 업데이트

## 담당 에이전트
- Implement: `devops-engineer`
- Review: `deployment-engineer`

## 검증
- `curl https://api-dev.rehearse.co.kr/actuator/health` → `{"status":"UP"}`
- `https://dev.rehearse.co.kr` 접속 → 프론트엔드 정상 로딩
- API 호출 시 Mixed Content 에러 없음
