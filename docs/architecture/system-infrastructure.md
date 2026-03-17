# 시스템 인프라 다이어그램

## 전체 아키텍처

```mermaid
graph TB
    subgraph Client["🖥️ Client Layer"]
        FE["React + TypeScript<br/>(Vercel)"]
    end

    subgraph AWS_Compute["☁️ AWS Compute"]
        API["Spring Boot API Server<br/>(EC2 t3.small)<br/>Java 21"]
        AnalysisLambda["분석 Lambda<br/>(Python)<br/>FFmpeg + OpenAI"]
        ConvertLambda["변환 Lambda<br/>(Python)<br/>MediaConvert 트리거"]
    end

    subgraph AWS_Storage["📦 AWS Storage & DB"]
        S3["S3 Bucket<br/>WebM 원본 + MP4 변환"]
        RDS["RDS MySQL 8.0<br/>(프리티어)"]
    end

    subgraph AWS_Event["⚡ AWS Event"]
        EB["EventBridge<br/>S3 Event Notification"]
        MC["MediaConvert<br/>WebM → MP4 (faststart)"]
        DLQ["DLQ<br/>(Dead Letter Queue)"]
    end

    subgraph External["🌐 External APIs"]
        Claude["Claude API<br/>(claude-sonnet-4-20250514)<br/>질문 생성"]
        OpenAI["OpenAI API<br/>Whisper + GPT-4o Vision"]
    end

    %% Client → API Server
    FE -->|"REST API<br/>(면접 생성/상태 조회/피드백)"| API
    FE -->|"Presigned URL PUT<br/>(영상 직접 업로드)"| S3

    %% API Server connections
    API -->|"JPA + HikariCP"| RDS
    API -->|"질문 + 모범답변 생성"| Claude
    API -->|"Presigned URL 발급"| S3

    %% S3 Event trigger
    S3 -->|"S3 Event<br/>(영상 업로드 완료)"| EB
    EB -->|"동시 트리거"| AnalysisLambda
    EB -->|"동시 트리거"| ConvertLambda
    EB -.->|"전달 실패"| DLQ

    %% Analysis Lambda
    AnalysisLambda -->|"영상 다운로드<br/>+ FFmpeg 추출"| S3
    AnalysisLambda -->|"Whisper STT<br/>+ Vision 분석<br/>+ LLM 평가"| OpenAI
    AnalysisLambda -->|"Internal API<br/>(상태 변경 + 결과 저장)"| API

    %% Convert Lambda
    ConvertLambda -->|"변환 작업 생성"| MC
    MC -->|"MP4 저장"| S3
    ConvertLambda -->|"Internal API<br/>(변환 완료 알림)"| API

    %% Styling
    classDef client fill:#a5d8ff,stroke:#1971c2,stroke-width:2px,color:#000
    classDef compute fill:#d0bfff,stroke:#7048e8,stroke-width:2px,color:#000
    classDef storage fill:#b2f2bb,stroke:#2f9e44,stroke-width:2px,color:#000
    classDef event fill:#fff3bf,stroke:#fab005,stroke-width:2px,color:#000
    classDef external fill:#ffc9c9,stroke:#e03131,stroke-width:2px,color:#000
    classDef ai fill:#e599f7,stroke:#9c36b5,stroke-width:2px,color:#000

    class FE client
    class API,AnalysisLambda,ConvertLambda compute
    class S3,RDS storage
    class EB,MC,DLQ event
    class Claude,OpenAI ai
```

## 데이터 흐름 (질문 세트 단위)

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant API as API Server
    participant DB as MySQL
    participant S3 as S3
    participant EB as EventBridge
    participant AL as 분석 Lambda
    participant CL as 변환 Lambda
    participant MC as MediaConvert
    participant OAI as OpenAI API
    participant Claude as Claude API

    Note over C,Claude: 1단계 — 면접 시작
    C->>API: POST /api/interviews
    API->>Claude: 질문 + 모범답변 생성
    Claude-->>API: 질문세트 목록
    API->>DB: 면접 + 질문세트 저장
    API-->>C: 면접 데이터 (질문세트 포함)

    Note over C,Claude: 2단계 — 질문세트별 녹화 + 업로드 (반복)
    C->>API: POST /answers (답변 구간 메타데이터)
    API->>DB: 답변 구간 4개 저장
    C->>API: POST /upload-url
    API-->>C: Presigned URL
    C->>S3: PUT (WebM 영상 직접 업로드)

    Note over C,Claude: 3단계 — 분석 + 변환 병렬 시작
    S3->>EB: S3 Event (업로드 완료)
    par 분석 Lambda
        EB->>AL: 트리거
        AL->>API: PUT /progress (STARTED)
        AL->>S3: 영상 다운로드
        AL->>OAI: Whisper STT + Vision + LLM
        AL->>API: POST /feedback (결과 저장)
    and 변환 Lambda
        EB->>CL: 트리거
        CL->>MC: WebM→MP4 변환 요청
        MC->>S3: MP4 저장
        CL->>API: PUT /convert-status (완료)
    end

    Note over C,Claude: 4단계 — 상태 폴링 + 피드백 조회
    loop 폴링
        C->>API: GET /status
        API->>DB: 상태 조회
        API-->>C: 질문세트별 분석/변환 상태
    end
    C->>API: GET /feedback
    API->>DB: 피드백 조회
    API-->>C: 전체 피드백 데이터
```

## 인프라 컴포넌트 상세

| 컴포넌트 | 기술 | 역할 | 비고 |
|----------|------|------|------|
| 프론트엔드 | React 18 + TS (Vercel) | 면접 UI, 영상 녹화, 피드백 뷰어 | MediaRecorder, Web Speech API |
| API 서버 | EC2 t3.small + Spring Boot 3.4 | 세션 관리, Presigned URL, Internal API | JVM: -Xms512m -Xmx512m |
| DB | RDS MySQL 8.0 (프리티어) | 면접/답변/피드백 저장 | HikariCP 커넥션 풀 |
| 영상 저장 | S3 | WebM 원본 + MP4 변환 | Lifecycle Rule 적용 |
| 이벤트 | EventBridge | S3 이벤트 → Lambda 트리거 | DLQ 설정 |
| 분석 Lambda | Python | FFmpeg + Whisper + Vision + LLM | Reserved Concurrency 3~5 |
| 변환 Lambda | Python | MediaConvert 작업 생성 | 30초~1분 소요 |
| MediaConvert | AWS Elemental | WebM → MP4 (faststart) | 질문세트당 ~$0.12 |
| AI (질문) | Claude API | 질문 + 모범답변 생성 | Backend에서만 호출 |
| AI (분석) | OpenAI API | STT + 비언어 분석 + 언어 평가 | 질문세트당 ~$0.26 |

## 보안 아키텍처

```mermaid
graph LR
    subgraph Public["Public Zone"]
        Client["클라이언트"]
    end

    subgraph VPC["AWS VPC"]
        subgraph PublicSubnet["Public Subnet"]
            API["API Server<br/>(EC2)"]
        end
        subgraph PrivateSubnet["Private Subnet"]
            RDS["MySQL<br/>(RDS)"]
            Lambda1["분석 Lambda"]
            Lambda2["변환 Lambda"]
        end
    end

    Client -->|"HTTPS"| API
    Client -->|"Presigned URL"| S3["S3"]
    API -->|"JDBC"| RDS
    Lambda1 -->|"Internal-Api-Key<br/>Header 인증"| API
    Lambda2 -->|"Internal-Api-Key<br/>Header 인증"| API

    classDef public fill:#ffc9c9,stroke:#e03131,stroke-width:2px
    classDef vpc fill:#d0bfff,stroke:#7048e8,stroke-width:2px
    classDef private fill:#b2f2bb,stroke:#2f9e44,stroke-width:2px

    class Client public
    class API vpc
    class RDS,Lambda1,Lambda2 private
```
