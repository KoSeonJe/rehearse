# Plan 02: CloudFront + ACM + DNS 현 상태 공식 확인 + Invalidation

> 상태: Draft (v2, 2026-04-08 개정)
> 작성일: 2026-04-08
> 의존: plan-01 [parallel with plan-03, plan-04]

## Why

v1 초안은 이 단계에서 ACM 발급, 가비아 CNAME 추가, CloudFront alternate domain 추가를 하려 했으나, 2026-04-08 실측 결과 **이 세 가지가 이미 전부 완료된 상태**임이 확인되었다 (`requirements.md` Evidence 섹션 참조):

```
$ dig +short dev.rehearse.co.kr
d2n8xljv54hfw0.cloudfront.net.
3.168.167.27 / .42 / .50 / .115

$ curl -I https://dev.rehearse.co.kr
HTTP/2 200
server: CloudFront
```

따라서 본 태스크의 역할은 **신규 생성이 아니라 "현 상태가 안정적이고 정확함을 공식 확인"**하는 것으로 축소된다. 추가로, 향후 plan-06에서 apex를 제거할 때 CloudFront CustomErrorResponses(SPA 라우팅용 403/404→index.html)가 실수로 드롭되지 않도록 **현재 설정의 기준점**을 확보해야 한다.

이 태스크가 의미 있는 순간: 누군가가 CloudFront·ACM·DNS 어느 한 곳에서 실수로 dev 관련 레코드를 건드려 plan-05 실행 직전에 결함이 드러나는 것을 방지하는 것.

## 생성/수정 파일

| 파일 / 리소스 | 작업 |
|------|------|
| `docs/plans/dev-domain-restore/survey-snapshot.md` | plan-01에서 만든 스냅샷에 본 태스크 확인 결과 추가 |
| CloudFront Distribution `d2n8xljv54hfw0` | 설정 변경 없음, invalidation `/*` 1회 집행 (전환 시 stale SPA 번들 제거) |
| ACM (us-east-1) | 조회만 |
| 가비아 DNS | 조회만 |

## 상세

### 1. CloudFront 현 상태 확인
```bash
# plan-01에서 얻은 Distribution ID 사용
DIST_ID=<ID>

aws cloudfront get-distribution --id $DIST_ID \
  --query "Distribution.{
    Status:Status,
    Aliases:DistributionConfig.Aliases.Items,
    Cert:DistributionConfig.ViewerCertificate.ACMCertificateArn,
    ErrorResponses:DistributionConfig.CustomErrorResponses.Items,
    DefaultRoot:DistributionConfig.DefaultRootObject
  }"
```
**기대 결과**
- `Status` = `Deployed`
- `Aliases`에 **`rehearse.co.kr`, `dev.rehearse.co.kr` 둘 다 포함** (`www.rehearse.co.kr`이 있는지도 확인)
- `Cert`가 두 도메인을 모두 커버하는 인증서의 ARN (SAN 포함 또는 wildcard)
- `ErrorResponses`에 403 또는 404 → `/index.html` + `ResponseCode: 200` 매핑 **존재** (SPA 라우팅에 필수)
- `DefaultRoot` = `index.html`

기대와 다르면 plan-06 실행 전에 **반드시 보정**. 이 시점에서 CloudFront를 수정하진 않고, 기록만 하고 plan-06 절차에서 콘솔 편집으로 안전하게 처리.

### 2. ACM 인증서 SAN 확인
```bash
# plan-01에서 기록한 ACM ARN 사용
CERT_ARN=<ARN>

aws acm describe-certificate --region us-east-1 --certificate-arn $CERT_ARN \
  --query "Certificate.{
    Domain:DomainName,
    SANs:SubjectAlternativeNames,
    Status:Status,
    InUseBy:InUseBy,
    NotAfter:NotAfter
  }"
```
**기대 결과**
- `Status` = `ISSUED`
- `SANs`에 `rehearse.co.kr`, `dev.rehearse.co.kr` 둘 다 포함 (또는 `*.rehearse.co.kr` + 루트)
- `InUseBy`에 CloudFront Distribution ARN이 포함
- `NotAfter`가 충분히 멀어야 함 (30일 이내 만료면 별건으로 갱신 필요)

### 3. 가비아 DNS 레코드 확인
```bash
# 권위 NS에 직접 질의 — 로컬 리졸버 캐시 무관
# 가비아 NS는 콘솔에서 확인 (보통 ns1.gabia.co.kr 등)
dig @<가비아-권위-NS> dev.rehearse.co.kr
dig @<가비아-권위-NS> rehearse.co.kr
dig @<가비아-권위-NS> api-dev.rehearse.co.kr
```
**기대 결과**
- `dev.rehearse.co.kr` → CNAME `d2n8xljv54hfw0.cloudfront.net` (실측 확인됨)
- `rehearse.co.kr` → CloudFront로 향하는 A/ALIAS (가비아는 CNAME-at-apex 미지원이라 보통 A 여러 개)
- `api-dev.rehearse.co.kr` → `54.180.188.135` A 레코드

### 4. CloudFront Invalidation 집행 (plan-05 직전 타이밍)
plan-04에서 `application-dev.yml`이 바뀌어 머지되면 deploy-dev 워크플로우가 프론트 번들을 재빌드하여 S3에 업로드한다. 이 때 CloudFront가 구버전 `index.html`을 잠시 캐시하고 있으면 사용자가 새 JS 번들 해시를 받지 못하는 레이스가 발생할 수 있다.

```bash
aws cloudfront create-invalidation \
  --distribution-id $DIST_ID \
  --paths "/*"

# 상태 확인
aws cloudfront list-invalidations --distribution-id $DIST_ID --max-items 1
```
`InProgress` → `Completed` 대기 (보통 수 분).

**타이밍**: plan-04 배포 완료 직후, plan-05 실행 직전에 집행. 본 태스크에서 "준비 완료"만 선언하고 실제 호출은 plan-05 Step 1(Gate) 직전에 배치해도 무방.

### 5. 스냅샷 업데이트
`survey-snapshot.md`에 다음 항목을 기록:
- 본 태스크 수행 시각
- 위 1·2·3의 응답 원본 (JSON 블록)
- 기대와 일치 여부 (체크박스)
- 불일치 항목과 후속 조치 메모

## 담당 에이전트

- Implement: `devops-engineer` — AWS CLI 조회, invalidation 집행, 스냅샷 업데이트
- Review: `architect-reviewer` — CustomErrorResponses가 plan-06의 alias 제거 이후에도 보존될 전략인지 검증 (콘솔 편집 권장 vs CLI 부분 업데이트 위험)

## 검증

- [ ] CloudFront Aliases에 apex + dev 둘 다 확인됨
- [ ] ACM SAN에 apex + dev 둘 다 포함, `ISSUED`, `InUseBy`에 CloudFront 포함
- [ ] 가비아 DNS 권위 NS 기준 dev / apex / api-dev 전부 기대대로 해석
- [ ] CustomErrorResponses(403/404 → /index.html) 존재함을 스냅샷에 기록 (plan-06 대비)
- [ ] CloudFront invalidation `/*` 완료
- [ ] `survey-snapshot.md` 업데이트
- [ ] `progress.md` Task 2 → Completed
