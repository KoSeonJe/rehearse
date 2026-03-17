# Rehearse 인프라 현황 (2026-03-18)

## 1. 전체 시스템 아키텍처

```mermaid
graph TB
    subgraph Client["클라이언트"]
        Browser["Browser<br/>(React 18 + TS)"]
    end

    subgraph CDN["CDN / 호스팅"]
        CF["CloudFront<br/>d2n8xljv54hfw0"]
        S3_FE["S3 (Frontend)<br/>정적 파일"]
    end

    subgraph EC2_Box["EC2 t3.small<br/>54.180.188.135"]
        Docker["Docker Compose"]
        subgraph Containers["컨테이너"]
            BE["Spring Boot 3.4<br/>(Java 21)<br/>:8080 → :80"]
            DB["MySQL 8.0<br/>:3306"]
        end
    end

    subgraph AWS_Services["AWS 서비스"]
        S3["S3<br/>rehearse-videos-dev"]
        EB["EventBridge<br/>rehearse-video-uploaded-dev"]
        subgraph Lambdas["Lambda"]
            L_Convert["rehearse-convert-dev<br/>256MB / 300s"]
            L_Analysis["rehearse-analysis-dev<br/>2048MB / 900s"]
        end
        MC["MediaConvert"]
        ECR["ECR<br/>rehearse-backend"]
    end

    subgraph External["외부 API"]
        Claude["Claude API<br/>claude-sonnet-4-20250514"]
        OpenAI_W["OpenAI Whisper<br/>whisper-1"]
        OpenAI_V["OpenAI GPT-4o<br/>Vision + LLM"]
    end

    Browser -->|HTTPS| CF
    CF --> S3_FE
    Browser -->|HTTP :80| BE
    BE --> DB
    BE -->|Presigned URL 생성| S3
    Browser -->|직접 업로드| S3

    S3 -->|PutObject 이벤트| EB
    EB --> L_Convert
    EB --> L_Analysis

    L_Convert -->|CreateJob| MC
    MC -->|WebM→MP4| S3
    L_Convert -->|PUT /files/status| BE

    L_Analysis -->|다운로드| S3
    L_Analysis -->|STT| OpenAI_W
    L_Analysis -->|비언어 분석| OpenAI_V
    L_Analysis -->|언어 분석| OpenAI_V
    L_Analysis -->|PUT /progress<br/>POST /feedback| BE

    BE -->|질문 생성| Claude

    style EC2_Box fill:#f0f4ff,stroke:#4a6fa5
    style AWS_Services fill:#fff3e0,stroke:#e65100
    style External fill:#f3e5f5,stroke:#7b1fa2
    style CDN fill:#e8f5e9,stroke:#2e7d32
```

## 2. 녹화-분석-변환 파이프라인

```mermaid
sequenceDiagram
    participant B as Browser
    participant BE as Spring Boot
    participant S3 as S3
    participant EB as EventBridge
    participant LC as Convert Lambda
    participant LA as Analysis Lambda
    participant MC as MediaConvert
    participant OAI as OpenAI API

    Note over B,BE: 1단계: 녹화 + 업로드
    B->>BE: POST /upload-url
    BE->>S3: Presigned URL 생성
    BE-->>B: uploadUrl + s3Key
    B->>S3: PUT (WebM 직접 업로드)
    B->>BE: POST /answers (타임스탬프)

    Note over S3,EB: 2단계: 이벤트 트리거
    S3->>EB: PutObject (videos/*.webm)
    par 변환 + 분석 병렬 실행
        EB->>LC: Convert Lambda 트리거
        EB->>LA: Analysis Lambda 트리거
    end

    Note over LC,MC: 3단계: 변환 (Convert Lambda)
    LC->>BE: GET /files/by-s3-key (멱등성 체크)
    LC->>BE: PUT /files/status → CONVERTING
    LC->>MC: CreateJob (WebM→MP4, H.264+AAC)
    MC->>S3: MP4 저장 (faststart)
    LC->>BE: PUT /files/status → CONVERTED

    Note over LA,OAI: 4단계: 분석 (Analysis Lambda)
    LA->>BE: GET /answers (멱등성 체크)
    LA->>BE: PUT /progress → STARTED
    LA->>S3: WebM 다운로드
    LA->>LA: FFmpeg: WAV + 프레임 추출
    LA->>BE: PUT /progress → STT_PROCESSING
    LA->>OAI: Whisper STT (ko)
    LA->>BE: PUT /progress → NONVERBAL_ANALYZING
    LA->>OAI: GPT-4o Vision (프레임 분석)
    LA->>BE: PUT /progress → VERBAL_ANALYZING
    LA->>OAI: GPT-4o LLM (언어+말투 분석)
    LA->>BE: PUT /progress → FINALIZING
    LA->>BE: POST /feedback (전체 결과)

    Note over B,BE: 5단계: 결과 조회
    B->>BE: GET /status (폴링)
    B->>BE: GET /feedback
    BE-->>B: 타임스탬프 피드백 + Presigned URL
```

## 3. AWS 리소스 상세

### 3.1 EC2

| 항목 | 값 |
|------|-----|
| Instance ID | `i-0d1d65843d4c37f9b` |
| Type | t3.small (2 vCPU, 2GB RAM) |
| AMI | Ubuntu |
| Public IP | `54.180.188.135` |
| Private IP | `172.31.7.196` |
| Key Pair | `rehearse-key.pem` |
| Security Group | `sg-082751d93d0991dd3` |

**Security Group 인바운드 규칙:**

| 포트 | 프로토콜 | 소스 | 용도 |
|------|----------|------|------|
| 22 | TCP | 0.0.0.0/0 | SSH |
| 80 | TCP | 0.0.0.0/0 | BE API (Docker 80→8080) |
| 8080 | TCP | 0.0.0.0/0 | Lambda → BE 직접 접근 |

**Docker Compose 구성:**

| 컨테이너 | 이미지 | 포트 | 네트워크 |
|-----------|--------|------|----------|
| rehearse-backend | ECR/rehearse-backend:latest | 80→8080 | backend_default |
| rehearse-db | mysql:8.0 | 3306→3306 | backend_default |

### 3.2 S3

| 항목 | 값 |
|------|-----|
| Bucket | `rehearse-videos-dev` |
| Region | ap-northeast-2 |

**디렉토리 구조:**
```
rehearse-videos-dev/
├── videos/{interviewId}/qs_{questionSetId}.webm   ← 원본 녹화
├── videos/{interviewId}/qs_{questionSetId}.mp4    ← MediaConvert 변환
├── analysis-backup/{id}/qs_{qsId}.json            ← 분석 실패 시 백업
└── layers/ffmpeg-layer.zip                         ← Lambda Layer 소스
```

### 3.3 EventBridge

| 항목 | 값 |
|------|-----|
| Rule | `rehearse-video-uploaded-dev` |
| State | ENABLED |
| Pattern | S3 PutObject → `videos/*.webm` |
| Targets | rehearse-convert-dev, rehearse-analysis-dev |

### 3.4 Lambda

#### rehearse-convert-dev (WebM → MP4 변환)

| 항목 | 값 |
|------|-----|
| Runtime | Python 3.12 |
| Memory | 256 MB |
| Timeout | 300초 (5분) |
| Handler | handler.lambda_handler |

**환경변수:**

| 변수 | 값 |
|------|-----|
| API_SERVER_URL | `http://54.180.188.135:80` |
| INTERNAL_API_KEY | `rehearse-internal-dev-key-2026` |
| S3_BUCKET | `rehearse-videos-dev` |
| MEDIACONVERT_ENDPOINT | (계정별 엔드포인트) |
| MEDIACONVERT_ROLE | `arn:aws:iam::776735194358:role/rehearse-mediaconvert-role` |

#### rehearse-analysis-dev (AI 분석 파이프라인)

| 항목 | 값 |
|------|-----|
| Runtime | Python 3.12 |
| Memory | 2048 MB |
| Timeout | 900초 (15분, Lambda 최대) |
| Handler | handler.lambda_handler |
| Layer | `ffmpeg-static:1` (FFmpeg 7.x, 56MB) |

**환경변수:**

| 변수 | 값 |
|------|-----|
| API_SERVER_URL | `http://54.180.188.135:80` |
| INTERNAL_API_KEY | `rehearse-internal-dev-key-2026` |
| S3_BUCKET | `rehearse-videos-dev` |
| OPENAI_API_KEY | (설정됨) |

### 3.5 IAM

#### rehearse-lambda-execution (Lambda 실행 역할)

| 정책 | 권한 |
|------|------|
| AWSLambdaBasicExecutionRole | CloudWatch Logs |
| rehearse-lambda-s3-mediaconvert | S3 Get/Put/Head, MediaConvert Create/Get/Describe, iam:PassRole |

#### rehearse-mediaconvert-role (MediaConvert 서비스 역할)

| 정책 | 권한 |
|------|------|
| S3Access | S3 Get/Put (`rehearse-videos-dev/*`) |

### 3.6 MediaConvert

| 항목 | 값 |
|------|-----|
| Input | WebM (VP9 + Opus/Vorbis) |
| Output | MP4 (H.264 HIGH + AAC 128kbps) |
| MOOV Placement | PROGRESSIVE_DOWNLOAD (faststart) |
| Rate Control | QVBR (Quality Level 7, MaxBitrate 5Mbps) |

### 3.7 ECR

| 항목 | 값 |
|------|-----|
| Repository | `776735194358.dkr.ecr.ap-northeast-2.amazonaws.com/rehearse-backend` |
| Tag | `latest` |
| Base Image | `eclipse-temurin:21-jre` |

### 3.8 CloudFront + S3 (Frontend)

| 항목 | 값 |
|------|-----|
| Distribution | `d2n8xljv54hfw0.cloudfront.net` |
| Origin | S3 (정적 빌드 파일) |
| Framework | React 18 + Vite |

### 3.9 Lambda Layer

| Layer | 버전 | 내용 | 크기 |
|-------|------|------|------|
| ffmpeg-static | 1 | FFmpeg 7.x + FFprobe (Linux x86_64 static) | 56MB |

## 4. 네트워크 흐름

```mermaid
graph LR
    subgraph Internet
        User["사용자"]
    end

    subgraph AWS_VPC["AWS VPC (ap-northeast-2)"]
        subgraph Public_Subnet["Public Subnet"]
            EC2["EC2<br/>54.180.188.135:80"]
        end

        subgraph AWS_Managed["AWS Managed"]
            Lambda1["Convert Lambda"]
            Lambda2["Analysis Lambda"]
            S3["S3"]
            MC["MediaConvert"]
            EB["EventBridge"]
        end
    end

    subgraph External_API["External APIs"]
        CF_API["CloudFront"]
        Claude["Claude API"]
        OpenAI["OpenAI API"]
    end

    User -->|HTTPS| CF_API
    User -->|HTTP :80| EC2
    User -->|Presigned URL| S3

    S3 -->|Event| EB
    EB --> Lambda1
    EB --> Lambda2

    Lambda1 -->|HTTP :80| EC2
    Lambda2 -->|HTTP :80| EC2
    Lambda1 --> MC
    Lambda2 --> S3
    MC --> S3

    EC2 --> Claude
    Lambda2 --> OpenAI

    style AWS_VPC fill:#f0f8ff,stroke:#1565c0
    style AWS_Managed fill:#fff8e1,stroke:#f57f17
```

## 5. CI/CD 파이프라인

```mermaid
graph LR
    subgraph Trigger["트리거"]
        Push["Push to develop"]
    end

    subgraph Detection["변경 감지"]
        CD["Change Detection"]
    end

    subgraph Test["테스트"]
        BT["BE Test<br/>(Gradle + H2)"]
        FT["FE Build<br/>(Node + Vite)"]
    end

    subgraph Deploy["배포"]
        FE_Deploy["FE → S3<br/>→ CloudFront 캐시 무효화"]
        BE_Deploy["BE → Docker Build<br/>→ ECR Push<br/>→ EC2 SSH Deploy"]
    end

    Push --> CD
    CD -->|backend/**| BT
    CD -->|frontend/**| FT
    BT --> BE_Deploy
    FT --> FE_Deploy

    style Trigger fill:#e8f5e9
    style Deploy fill:#fff3e0
```

## 6. 환경변수 관리

### EC2 (`~/rehearse/backend/.env`)
```
DB_URL, DB_USERNAME, DB_PASSWORD, DB_ROOT_PASSWORD
SPRING_PROFILES_ACTIVE=dev
CLAUDE_API_KEY
OPENAI_API_KEY
INTERNAL_API_KEY
CORS_ALLOWED_ORIGINS
ECR_REGISTRY
AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION
```

### Lambda 환경변수
```
API_SERVER_URL=http://54.180.188.135:80
INTERNAL_API_KEY
S3_BUCKET=rehearse-videos-dev
OPENAI_API_KEY          (analysis만)
MEDIACONVERT_ENDPOINT   (convert만)
MEDIACONVERT_ROLE       (convert만)
```

## 7. 비용 추정 (월간, dev 환경)

| 서비스 | 예상 비용 |
|--------|-----------|
| EC2 t3.small | ~$15/월 |
| RDS-free / Docker MySQL | $0 |
| S3 (10GB 이하) | ~$0.25 |
| Lambda (100회 분석) | ~$1 |
| MediaConvert (100건) | ~$2 |
| CloudFront | ~$1 |
| **합계** | **~$20/월** |
