# Task 2: S3 + EventBridge + Lambda + MediaConvert 인프라 구축

## Status: Not Started

## Issue: #79

## Why

새 파이프라인은 S3 이벤트 기반으로 분석/변환 Lambda가 자동 트리거됨.
인프라가 없으면 녹화 영상 저장, 분석 트리거, 스트리밍 변환이 모두 불가능.

## 의존성

- 선행: Task 0 (Legacy 정리)
- 후행: Task 6, 7 (Lambda 구현에 인프라 필요)

## 구현 계획

### PR 1: [Infra] AWS 리소스 구축

**S3:**
- 버킷: `rehearse-videos-{env}`
- 경로 규칙: `videos/{interviewId}/qs_{questionSetId}.webm` (원본), `.mp4` (변환)
- Lifecycle Rule: 30일 → Intelligent-Tiering, 90일 → WebM 삭제, 180일 → 전체 삭제
- CORS 설정 (Presigned URL PUT 업로드용)

**EventBridge:**
- S3 `PutObject` 이벤트 → 분석 Lambda + 변환 Lambda 동시 트리거
- 필터: `videos/*/qs_*.webm` 패턴만
- DLQ 설정

**Lambda:**
- 분석 Lambda 스켈레톤 (Python 3.12, 512MB, 15분 타임아웃)
- 변환 Lambda 스켈레톤 (Python 3.12, 256MB, 5분 타임아웃)
- Reserved Concurrency: 분석 3~5, 변환 10

**IAM:**
- Lambda 실행 역할 (S3 읽기/쓰기, MediaConvert, CloudWatch Logs)
- Internal API Key 생성 (Lambda 환경변수)

**MediaConvert:**
- Job Template: WebM → MP4 (H.264, AAC, faststart)

- Implement: `devops-engineer`
- Review: `architect-reviewer` — 보안, 비용, 확장성 검증

### PR 2: [BE] AWS S3 SDK + Presigned URL 유틸리티

**의존성 추가:**
- `software.amazon.awssdk:s3`
- `software.amazon.awssdk:sts` (선택)

**구현:**
- `S3Service.java`: Presigned URL 생성 (PUT, GET), 경로 생성 유틸리티
- `AwsConfig.java`: S3Client 빈 설정
- `application.yml`: S3 버킷명, 리전 설정

- Implement: `backend`
- Review: `architect-reviewer`

## Acceptance Criteria

- [ ] S3 버킷 생성 + CORS 설정 완료
- [ ] EventBridge 규칙이 S3 이벤트로 Lambda 2개 트리거
- [ ] Lambda 스켈레톤 배포 + 로그 출력 확인
- [ ] IAM 최소 권한 원칙 적용
- [ ] MediaConvert Job Template 동작 확인
- [ ] BE Presigned URL 발급 + S3 PUT 테스트 통과
