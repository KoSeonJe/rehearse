# Rehearse 프로젝트 — 테이블 구조 & 프롬프트 전체 정리

## 목차
1. [데이터베이스 테이블 구조](#1-데이터베이스-테이블-구조)
2. [Spring Boot 프롬프트 (Claude API)](#2-spring-boot-프롬프트-claude-api)
3. [Lambda 프롬프트 (OpenAI API)](#3-lambda-프롬프트-openai-api)

---

## 1. 데이터베이스 테이블 구조

### 1.1 전체 테이블 목록

| 테이블 | 용도 | FK |
|--------|------|-----|
| `interview` | 면접 세션 (루트) | — |
| `interview_interview_types` | 면접 유형 (다중값) | interview(id) |
| `interview_cs_sub_topics` | CS 세부 주제 (다중값) | interview(id) |
| `question_set` | 질문세트 = 녹화/분석 단위 | interview(id), file_metadata(id) |
| `question` | 개별 질문 | question_set(id) |
| `question_answer` | 답변 타임스탬프 구간 | question(id) |
| `question_set_feedback` | 질문세트별 종합 피드백 | question_set(id) [1:1] |
| `timestamp_feedback` | 타임스탬프별 상세 피드백 | question_set_feedback(id), question(id) |
| `file_metadata` | S3 파일 라이프사이클 | — |
| `interview_report` | 최종 종합 리포트 | interview(id) [1:1] |
| `report_strengths` | 리포트 강점 목록 | interview_report(id) |
| `report_improvements` | 리포트 개선점 목록 | interview_report(id) |

### 1.2 ER 관계도 (텍스트)

```
Interview (1) ──< (N) QuestionSet (1) ── (1) QuestionSetFeedback (1) ──< (N) TimestampFeedback
    │                    │                                                        │
    │                    ├──< (N) Question ──< (N) QuestionAnswer                 │
    │                    │                                                        │
    │                    └── (1) FileMetadata                       question(id) ──┘
    │
    └── (1) InterviewReport
```

---

### 1.3 엔티티별 상세

#### `interview`
> 파일: `backend/.../domain/interview/entity/Interview.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| position | VARCHAR(20) | NOT NULL, Enum: `BACKEND`, `FRONTEND`, `DEVOPS`, `DATA_ENGINEER`, `FULLSTACK` |
| position_detail | VARCHAR(100) | nullable |
| level | VARCHAR(20) | NOT NULL, Enum: `JUNIOR`, `MID`, `SENIOR` |
| duration_minutes | INTEGER | NOT NULL |
| status | VARCHAR(20) | NOT NULL, Enum: `READY`, `IN_PROGRESS`, `COMPLETED` |
| question_generation_status | VARCHAR(20) | NOT NULL, Enum: `PENDING`, `GENERATING`, `COMPLETED`, `FAILED` |
| failure_reason | TEXT | nullable |
| overall_score | INTEGER | nullable |
| overall_comment | TEXT | nullable |
| created_at | DATETIME | NOT NULL, auto |
| updated_at | DATETIME | NOT NULL, auto |

**상태 전이:**
- `READY` → `IN_PROGRESS` → `COMPLETED`
- `QuestionGenerationStatus`: `PENDING` → `GENERATING` → `COMPLETED` / `FAILED`

#### `interview_interview_types` (ElementCollection)

| 컬럼 | 타입 |
|------|------|
| interview_id | Long (FK) |
| interview_types | VARCHAR(30), Enum |

**InterviewType 값:** `CS_FUNDAMENTAL`, `BEHAVIORAL`, `RESUME_BASED`, `JAVA_SPRING`, `SYSTEM_DESIGN`, `FULLSTACK_JS`, `REACT_COMPONENT`, `BROWSER_PERFORMANCE`, `INFRA_CICD`, `CLOUD`, `DATA_PIPELINE`, `SQL_MODELING`

#### `interview_cs_sub_topics` (ElementCollection)

| 컬럼 | 타입 |
|------|------|
| interview_id | Long (FK) |
| cs_sub_topics | VARCHAR(50) |

---

#### `question_set`
> 파일: `backend/.../domain/questionset/entity/QuestionSet.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| interview_id | Long | FK, NOT NULL |
| category | VARCHAR(20) | NOT NULL, Enum: `RESUME`, `CS` |
| order_index | INT | NOT NULL |
| file_metadata_id | Long | FK, nullable |
| analysis_status | VARCHAR(30) | NOT NULL, Enum |
| analysis_progress | VARCHAR(30) | nullable, Enum |
| failure_reason | VARCHAR(500) | nullable |
| failure_detail | TEXT | nullable |
| created_at | DATETIME | NOT NULL, auto |
| updated_at | DATETIME | NOT NULL, auto |
| version | Long | Optimistic Lock |

**UNIQUE:** `(interview_id, order_index)`

**AnalysisStatus 전이:**
- `PENDING` → `PENDING_UPLOAD`, `SKIPPED`, `FAILED`
- `PENDING_UPLOAD` → `ANALYZING`, `FAILED`
- `ANALYZING` → `COMPLETED`, `FAILED`
- `FAILED` → `PENDING_UPLOAD`, `ANALYZING`, `COMPLETED` (재시도)

**AnalysisProgress 값:** `STARTED`, `EXTRACTING`, `STT_PROCESSING`, `VERBAL_ANALYZING`, `NONVERBAL_ANALYZING`, `FINALIZING`, `FAILED`

---

#### `question`
> 파일: `backend/.../domain/questionset/entity/Question.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| question_set_id | Long | FK, NOT NULL |
| question_type | VARCHAR(20) | NOT NULL, Enum: `MAIN`, `FOLLOWUP` |
| question_text | TEXT | NOT NULL |
| model_answer | TEXT | nullable |
| reference_type | VARCHAR(20) | nullable, Enum: `MODEL_ANSWER`, `GUIDE` |
| order_index | INT | NOT NULL |

**UNIQUE:** `(question_set_id, order_index)`

---

#### `question_answer`
> 파일: `backend/.../domain/questionset/entity/QuestionAnswer.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| question_id | Long | FK, NOT NULL |
| start_ms | BIGINT | NOT NULL |
| end_ms | BIGINT | NOT NULL |

---

#### `question_set_feedback`
> 파일: `backend/.../domain/questionset/entity/QuestionSetFeedback.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| question_set_id | Long | FK, NOT NULL, UNIQUE (1:1) |
| question_set_score | INT | NOT NULL |
| question_set_comment | TEXT | NOT NULL |
| created_at | DATETIME | NOT NULL, auto |

---

#### `timestamp_feedback`
> 파일: `backend/.../domain/questionset/entity/TimestampFeedback.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| question_set_feedback_id | Long | FK, NOT NULL |
| question_id | Long | FK, nullable |
| start_ms | BIGINT | NOT NULL |
| end_ms | BIGINT | NOT NULL |
| transcript | TEXT | nullable |
| verbal_score | INTEGER | nullable |
| verbal_comment | TEXT | nullable |
| filler_word_count | INTEGER | nullable |
| eye_contact_score | INTEGER | nullable |
| posture_score | INTEGER | nullable |
| expression_label | VARCHAR(50) | nullable |
| nonverbal_comment | TEXT | nullable |
| overall_comment | TEXT | nullable |
| is_analyzed | BOOLEAN | NOT NULL |

---

#### `file_metadata`
> 파일: `backend/.../domain/file/entity/FileMetadata.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| file_type | VARCHAR(20) | NOT NULL, Enum: `VIDEO`, `RESUME` |
| status | VARCHAR(20) | NOT NULL, Enum: `PENDING`, `UPLOADED`, `CONVERTING`, `CONVERTED`, `FAILED` |
| s3_key | VARCHAR(500) | NOT NULL, UNIQUE |
| streaming_s3_key | VARCHAR(500) | nullable |
| bucket | VARCHAR(100) | nullable |
| content_type | VARCHAR(100) | nullable |
| file_size_bytes | BIGINT | nullable |
| failure_reason | VARCHAR(500) | nullable |
| failure_detail | TEXT | nullable |
| created_at | DATETIME | NOT NULL, auto |
| updated_at | DATETIME | NOT NULL, auto |
| version | Long | Optimistic Lock |

**FileStatus 전이:**
- `PENDING` → `UPLOADED` → `CONVERTING` → `CONVERTED`
- 모든 상태 → `FAILED`, `FAILED` → `UPLOADED` (재시도)

---

#### `interview_report`
> 파일: `backend/.../domain/report/entity/InterviewReport.java`

| 컬럼 | 타입 | 제약조건 |
|------|------|----------|
| id | Long | PK, AUTO_INCREMENT |
| interview_id | Long | FK, NOT NULL, UNIQUE (1:1) |
| overall_score | INT | NOT NULL |
| summary | TEXT | NOT NULL |
| feedback_count | INT | NOT NULL |
| created_at | DATETIME | NOT NULL, auto |

**report_strengths / report_improvements** (ElementCollection): 각각 TEXT 컬럼

---

### 1.4 마이그레이션 이력

| 버전 | 내용 |
|------|------|
| V1 | 초기 스키마 (interview, feedback, report) |
| V3 | 레거시 feedback/interview_answer 테이블 삭제 |
| V4 | question_set 파이프라인 (file_metadata, question, question_set_feedback, timestamp_feedback) |
| V5 | question_set_answer → question_answer 리네임 + timestamp_feedback에 question_id FK 추가 |
| V6 | interview에 question_generation_status, failure_reason 추가; interview_question 삭제 |
| V7 | question_set, file_metadata에 version 컬럼 (Optimistic Locking) |

---

## 2. Spring Boot 프롬프트 (Claude API)

> 파일: `backend/.../infra/ai/ClaudePromptBuilder.java`
> 모델: `claude-sonnet-4-20250514`

### 2.1 질문 생성 프롬프트

**Temperature:** `0.9` | **Max Tokens:** `4096`

#### System Prompt

```
당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
주어진 직무, 레벨, 면접 유형에 맞는 면접 질문을 생성해야 합니다.

면접 유형별 출제 가이드:
- CS_FUNDAMENTAL: CS 기초 (자료구조, 알고리즘, 운영체제, 네트워크, 데이터베이스)
- BEHAVIORAL: STAR 기법 기반 경험 질문 (상황, 과제, 행동, 결과)
- RESUME_BASED: 이력서/포트폴리오 기반 맞춤 질문
- JAVA_SPRING: Java/Spring 프레임워크 심화 (JVM, Spring IoC/AOP, JPA, 트랜잭션)
- SYSTEM_DESIGN: 시스템 아키텍처 설계, 스케일링, 트레이드오프 분석
- FULLSTACK_JS: Node.js + React 풀스택, API 설계, DB 연동, 배포
- REACT_COMPONENT: React 컴포넌트 설계, 상태 관리, 렌더링 최적화
- BROWSER_PERFORMANCE: 브라우저 렌더링, 웹 성능 최적화, 번들 최적화
- INFRA_CICD: 인프라 구성, CI/CD 파이프라인, 컨테이너 오케스트레이션
- CLOUD: 클라우드 아키텍처 (AWS/GCP/Azure), 서버리스, IaC
- DATA_PIPELINE: 데이터 수집/처리/적재 파이프라인, ETL/ELT, 스트리밍
- SQL_MODELING: SQL 쿼리 최적화, 데이터 모델링, 정규화/반정규화

CS 세부 주제가 지정된 경우 해당 주제에서만 출제하세요:
- DATA_STRUCTURE: 자료구조와 알고리즘
- OS: 운영체제 (프로세스, 스레드, 메모리, 스케줄링)
- NETWORK: 네트워크 (TCP/IP, HTTP, DNS, 보안)
- DATABASE: 데이터베이스 (인덱스, 트랜잭션, 정규화, 쿼리 최적화)

질문 수 규칙:
- 면접 시간이 설정된 경우: (면접 시간(분) / 3) 반올림 (최소 2개, 최대 24개)
- 유형별로 균등 배분

이력서가 제공된 경우 RESUME_BASED 유형의 질문은 이력서 내용을 기반으로 맞춤 생성하세요.

레벨별 난이도:
- JUNIOR: 기본 개념 이해도 확인, 실무 경험보다 학습 의지
- MID: 실무 적용 능력, 문제 해결 경험, 기술적 깊이
- SENIOR: 아키텍처 판단력, 리더십, 기술 의사결정 능력

모범답변 생성 규칙:
- 각 질문에 대한 모범답변(modelAnswer)을 반드시 포함하세요.
- CS 카테고리(기술/CS) 질문: referenceType을 "MODEL_ANSWER"로, 구체적 모범답변 제공
- RESUME 카테고리(이력서 기반) 질문: referenceType을 "GUIDE"로, 답변 방향 가이드 제공
- questionCategory는 이력서/경험 기반이면 "RESUME", 기술/CS이면 "CS"로 지정

반드시 아래 JSON 형식으로만 응답하세요:
{
  "questions": [
    {
      "content": "질문 내용",
      "category": "세부 카테고리명",
      "order": 1,
      "evaluationCriteria": "평가할 핵심 포인트",
      "questionCategory": "RESUME 또는 CS",
      "modelAnswer": "모범답변 또는 답변 가이드",
      "referenceType": "MODEL_ANSWER 또는 GUIDE"
    }
  ]
}
```

#### User Prompt

```
직무: {positionKorean}
레벨: {levelKorean}
면접 유형: {typesKorean}
질문 수: {questionCount}개

[CS 세부 주제가 있는 경우:]
CS 세부 주제: {subTopicsKorean}

[이력서가 있는 경우:]
이력서/포트폴리오:
{resumeText}

세션 ID: {UUID}
이전 면접과 중복되지 않는 새로운 관점의 질문을 생성해주세요.
위 조건에 맞는 면접 질문과 각 질문별 평가 기준을 생성해주세요.
각 질문의 카테고리는 면접 유형의 세부 분야로 지정해주세요.
```

---

### 2.2 후속 질문 생성 프롬프트

**Temperature:** `1.0` | **Max Tokens:** `1024`

#### System Prompt

```
당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
면접자의 답변을 바탕으로 더 깊이 있는 후속 질문을 생성합니다.

후속 질문 유형:
- DEEP_DIVE: 답변의 특정 부분을 더 깊이 파고드는 질문
- CLARIFICATION: 모호한 답변을 명확히 하기 위한 질문
- CHALLENGE: 답변의 논리적 허점이나 대안을 탐색하는 질문
- APPLICATION: 답변 내용을 다른 상황에 적용해보는 질문

규칙:
- 반드시 하나의 후속 질문만 생성하세요. 복합 질문은 금지합니다.
- 이전 후속 대화가 제공된 경우, 중복되지 않는 새로운 관점의 질문을 생성하세요.
- 매 라운드마다 다른 후속 질문 유형을 사용하여 다양한 각도에서 평가하세요.

모범답변 생성 규칙:
- 각 후속 질문에 대한 모범답변(modelAnswer)을 반드시 포함하세요.
- 핵심 개념과 실무 적용 관점에서 2-4문장의 구체적인 답변 가이드를 제공하세요.

반드시 아래 JSON 형식으로만 응답하세요:
{
  "question": "후속 질문 내용",
  "reason": "이 질문을 하는 이유",
  "type": "DEEP_DIVE|CLARIFICATION|CHALLENGE|APPLICATION",
  "modelAnswer": "모범답변 또는 답변 가이드"
}
```

#### User Prompt

```
원래 질문: {questionContent}
면접자 답변: {answerText}
비언어적 관찰: {nonVerbalSummary 또는 "관찰 데이터 없음"}

[이전 후속 대화가 있는 경우:]
이전 후속 대화:
[후속1] Q: {question1}
[후속1] A: {answer1}
...

위 대화를 바탕으로 새로운 후속 질문을 생성해주세요.
이전에 했던 질문과 중복되지 않는 새로운 관점의 질문이어야 합니다.

[이전 후속 대화가 없는 경우:]
위 답변에 대한 후속 질문을 생성해주세요.
```

---

### 2.3 종합 리포트 생성 프롬프트

**Temperature:** `0.3` | **Max Tokens:** `2048`

#### System Prompt

```
당신은 면접 코치입니다. 면접 피드백을 종합 분석하여 리포트를 생성합니다.

반드시 아래 JSON 형식으로만 응답하세요:
{
  "overallScore": 75,
  "summary": "종합 평가 요약 (2-3문장)",
  "strengths": ["강점1", "강점2", "강점3"],
  "improvements": ["개선점1", "개선점2", "개선점3"]
}

overallScore는 0-100 사이의 정수입니다.
strengths와 improvements는 각각 최소 2개, 최대 5개 항목입니다.
```

#### User Prompt

```
아래는 면접 피드백 데이터입니다. 종합 리포트를 생성해주세요.

면접 질문세트 수: {total}개 (분석 완료: {completed}개, 건너뜀: {skipped}개)

## 질문세트 (카테고리: {category})
- 점수: {score}/100
- 평가: {comment}
- 질문 [MAIN]: {question_text}
- 질문 [FOLLOWUP]: {followup_text}
...
```

---

## 3. Lambda 프롬프트 (OpenAI API)

> 모델: `gpt-4o` | Lambda 런타임: Python 3.12

### 3.1 비언어 분석 프롬프트 (GPT-4o Vision)

> 파일: `lambda/analysis/analyzers/vision_analyzer.py`
> **Temperature:** `0.3` | **Max Tokens:** `500`

#### System Prompt

```
당신은 면접 비언어 분석 전문가입니다.
면접 영상의 프레임 이미지들을 분석하여 면접자의 비언어적 커뮤니케이션을 평가합니다.

평가 기준:
1. 시선 처리 (eye_contact_score: 0-100)
   - 카메라를 적절히 응시하는지
   - 시선이 불안정하거나 자주 돌리는지
2. 자세 (posture_score: 0-100)
   - 바른 자세를 유지하는지
   - 어깨가 처지거나 몸을 흔드는지
3. 표정 (expression_label)
   - CONFIDENT: 자신감 있는 표정
   - NERVOUS: 긴장된 표정
   - NEUTRAL: 무표정
   - ENGAGED: 몰입된 표정
   - UNCERTAIN: 불확실한 표정

이미지에 사람이 보이지 않거나 분석이 어려운 경우에도 반드시 JSON으로 응답하되,
점수를 50으로 설정하고 comment에 상황을 설명하세요.

반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트 없이 JSON만 출력하세요:
{"eye_contact_score": 0, "posture_score": 0, "expression_label": "NEUTRAL", "comment": ""}
```

#### User Prompt

```
다음은 면접 영상에서 3초 간격으로 추출한 프레임입니다.
면접자의 비언어적 커뮤니케이션을 분석해주세요.
```

(이후 base64 인코딩된 프레임 이미지 첨부, 최대 10장)

---

### 3.2 언어 분석 프롬프트 (GPT-4o LLM)

> 파일: `lambda/analysis/analyzers/verbal_analyzer.py`
> **Temperature:** `0.3` | **Max Tokens:** `500`

#### System Prompt

```
당신은 면접 언어 분석 전문가입니다.
면접자의 답변 텍스트를 분석하여 언어적 커뮤니케이션을 평가합니다.

평가 기준:
1. 답변 논리성 (verbal_score: 0-100)
   - STAR 기법 등 구조화된 답변인지
   - 질문에 대한 핵심 답변이 포함되었는지
   - 논리적 흐름이 자연스러운지
2. 필러워드 개수 (filler_word_count)
   - "음", "어", "그", "아", "뭐", "이제", "약간" 등 불필요한 습관어
3. 핵심 키워드 활용
4. 말투/어조 적절성
5. 말투 분석 (tone_label)
   - PROFESSIONAL: 격식체, 면접에 적합한 어조
   - CASUAL: 반말이나 구어체 섞임
   - HESITANT: 자신감 없는 어조, "~인 것 같아요", "아마~" 등
   - CONFIDENT: 단정적이고 확신 있는 어조
   - VERBOSE: 불필요하게 장황한 설명

반드시 아래 JSON 형식으로만 응답하세요:
{
  "verbal_score": <0-100>,
  "filler_word_count": <정수>,
  "tone_label": "<PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE>",
  "tone_comment": "<한국어로 1-2문장의 말투 피드백>",
  "comment": "<한국어로 3-4문장의 언어 분석 피드백>"
}
```

#### User Prompt

```
## 질문
{question_text}

## 면접자 답변 (STT 전사)
{transcript}

위 답변을 분석해주세요.
```

---

### 3.3 STT — Whisper

> 파일: `lambda/analysis/analyzers/stt_analyzer.py`

| 설정 | 값 |
|------|-----|
| 모델 | `whisper-1` |
| 언어 | `ko` (한국어) |
| 응답 포맷 | `verbose_json` (세그먼트 타임스탬프 포함) |
| 타임스탬프 단위 | `segment` |
| 재시도 | 3회, 지수 백오프 |

> Whisper는 별도 프롬프트 템플릿 없이 오디오를 직접 전사합니다.

---

### 3.4 분석 파이프라인 전체 흐름

```
S3 업로드 → EventBridge 트리거 → Lambda 실행
                                     │
                                     ├─ 1. FFmpeg: 오디오 + 프레임 추출
                                     ├─ 2. Whisper STT: 전체 전사 + 세그먼트 타임스탬프
                                     ├─ 3. GPT-4o Vision: 비언어 분석 (시선/자세/표정)
                                     ├─ 4. GPT-4o LLM: 언어 분석 (논리성/필러워드/말투) — 질문별
                                     └─ 5. 종합 점수: 가중 평균 (언어 60% + 비언어 40%)
                                              │
                                              └─ Backend API로 피드백 저장
```

---

## 4. 프롬프트 설정 요약

| 용도 | 모델 | Temperature | Max Tokens | 위치 |
|------|------|-------------|------------|------|
| 질문 생성 | claude-sonnet-4-20250514 | 0.9 | 4096 | Backend (ClaudePromptBuilder) |
| 후속 질문 | claude-sonnet-4-20250514 | 1.0 | 1024 | Backend (ClaudePromptBuilder) |
| 종합 리포트 | claude-sonnet-4-20250514 | 0.3 | 2048 | Backend (ClaudePromptBuilder) |
| 비언어 분석 | gpt-4o | 0.3 | 500 | Lambda (vision_analyzer.py) |
| 언어 분석 | gpt-4o | 0.3 | 500 | Lambda (verbal_analyzer.py) |
| STT 전사 | whisper-1 | — | — | Lambda (stt_analyzer.py) |
