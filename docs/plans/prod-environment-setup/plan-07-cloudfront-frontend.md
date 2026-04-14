# Plan 07: prod Frontend S3 + CloudFront 구성

> 상태: Draft
> 작성일: 2026-04-12 (가비아 DNS 유지로 Alternate domain 조정)
> 선행: Plan 08 (ACM 인증서 — `www.rehearse.co.kr` 단일)

## 도메인 정책 (Plan 08과 정합)

- **CloudFront Alternate domain**: `www.rehearse.co.kr` **단일**
- **apex(`rehearse.co.kr`)는 CloudFront에 연결하지 않음** — 가비아 apex CNAME 미지원
- apex 트래픽은 Plan 08의 EC2 nginx 리디렉션으로 `https://www.rehearse.co.kr`로 301 전환

## Why

dev 프론트는 CloudFront `d2n8xljv54hfw0`로 서빙되며 alias는 `dev-domain-restore`(2026-04-09) 작업으로 `dev.rehearse.co.kr` 단일로 축소되었다. prod 프론트는 동일 distribution을 재사용하지 않는다:

- dev/prod 캐시 invalidation이 겹치면 상호 영향
- OAC 단위 S3 권한 분리 명확화
- prod 전용 로깅/보안 정책 필요
- 장애 시 물리 격리 확보

신규 CloudFront distribution + 신규 S3 버킷 `rehearse-frontend-prod`를 생성한다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| S3 버킷 `rehearse-frontend-prod` | 신규 생성 |
| CloudFront distribution `rehearse-prod-frontend` | 신규 생성 |
| CloudFront OAC | 신규 생성 |
| S3 버킷 정책 | OAC 허용 |
| `docs/architecture/infrastructure-status.md` | prod CloudFront 항목 추가 |

## 상세

### S3 버킷 `rehearse-frontend-prod`

| 설정 | 값 |
|---|---|
| Region | `ap-northeast-2` |
| Block Public Access | 전부 ON (OAC만 접근) |
| Versioning | Disabled (SPA는 매 배포 전체 교체, 롤백은 CI 재실행) |
| 서버사이드 암호화 | SSE-S3 |
| 정적 웹 호스팅 | OFF (OAC를 통한 private 접근) |

### S3 버킷 정책 (OAC 허용)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontServicePrincipal",
      "Effect": "Allow",
      "Principal": { "Service": "cloudfront.amazonaws.com" },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::rehearse-frontend-prod/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::776735194358:distribution/<신규 distribution ID>"
        }
      }
    }
  ]
}
```

### CloudFront distribution

| 설정 | 값 |
|---|---|
| Origin domain | `rehearse-frontend-prod.s3.ap-northeast-2.amazonaws.com` |
| Origin access | **OAC (Origin Access Control)** 신규 생성, `Sign requests` |
| Viewer protocol policy | `Redirect HTTP to HTTPS` |
| Allowed HTTP methods | `GET, HEAD, OPTIONS` |
| Cache policy | `CachingOptimized` (managed) — 기본 |
| Origin request policy | None |
| Response headers policy | `SecurityHeadersPolicy` (managed) — HSTS / X-Frame-Options / X-Content-Type-Options |
| Price class | Use only North America / Europe / Asia (dev와 동일 등급) |
| Alternate domain names (CNAMEs) | `www.rehearse.co.kr` (apex는 제외 — Plan 08 EC2 nginx가 담당) |
| SSL certificate | plan-08 us-east-1 ACM 인증서 (`www.rehearse.co.kr` 단일) |
| Default root object | `index.html` |
| HTTP version | HTTP/2, HTTP/3 |
| IPv6 | Enabled |
| Logging | Enabled → S3 `rehearse-cloudfront-logs-prod/` (선택, plan-14 연동) |
| WAF | 비활성 (별건) |

### Custom Error Responses (SPA 라우팅 필수)

| HTTP Error Code | Response Page Path | HTTP Response Code | Cache TTL |
|---|---|---|---|
| 403 | `/index.html` | 200 | 0 |
| 404 | `/index.html` | 200 | 0 |

**이유**: SPA 클라이언트 라우팅에서 S3 direct path 요청은 404가 되므로 모두 `index.html`로 폴백해야 React Router가 경로를 해석한다. dev CloudFront에서 이미 검증된 패턴(`dev-domain-restore/requirements.md:152-153` 참조).

### 캐시 정책 세부

- **정적 자산** (`/assets/*`, `*.js`, `*.css`, 해시 포함): 기본 CachingOptimized (max-age 24h+)
- **index.html**: 별도 cache behavior — `Cache-Control: no-cache, no-store` 응답 헤더 (CloudFront Function 또는 S3 메타데이터)
  - 이유: SPA 엔트리포인트는 즉시 반영되어야 새 배포 버전 인식
  - 구현: plan-11의 deploy 워크플로우에서 `aws s3 cp --cache-control "no-cache" index.html` 강제

### OAC 설정

1. CloudFront → Origin Access → Create control setting
2. Name: `rehearse-frontend-prod-oac`
3. Signing behavior: `Sign requests (recommended)`
4. Origin type: S3
5. Distribution origin에 OAC 연결
6. S3 버킷 정책에 위 JSON 적용

### dev distribution 비교표

| 항목 | dev | prod |
|---|---|---|
| Distribution ID | `d2n8xljv54hfw0` | 신규 |
| Alias | `dev.rehearse.co.kr` | `www.rehearse.co.kr` (apex는 EC2 nginx 리디렉션) |
| Origin S3 | dev 버킷 | `rehearse-frontend-prod` |
| OAC | dev OAC | prod OAC 신규 |
| Cert (us-east-1) | dev 인증서 | plan-08 신규 |

## 담당 에이전트

- Implement: `devops-engineer` — S3/CloudFront/OAC/버킷 정책 생성
- Review: `architect-reviewer` — 캐시 정책·SPA 폴백·OAC 경계 검증

## 검증

- `aws s3api list-buckets | grep rehearse-frontend-prod` → 존재
- `aws cloudfront list-distributions --query 'DistributionList.Items[?Aliases.Items[?contains(@, \`rehearse.co.kr\`)]]'` → 신규 distribution 반환
- `aws cloudfront get-distribution-config --id <prod-dist-id>` → Alternate domain에 apex + www 포함
- 버킷 정책이 OAC ARN을 조건으로 포함하는지 확인
- 테스트 파일 업로드 후 distribution 도메인(`d*.cloudfront.net`)으로 접근 가능 확인 — 가비아 DNS/ACM 완료 전이라도 검증 가능
- 존재하지 않는 경로 요청 시 index.html(200) 반환 (SPA 폴백)
- `progress.md` Task 7 → Completed
