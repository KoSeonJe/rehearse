# 면접 녹화-분석-피드백 파이프라인 설계

## 인프라 구성

| 컴포넌트 | 기술 | 역할 |
|----------|------|------|
| API 서버 | EC2 t3.small + Spring Boot | 면접 세션 관리, Presigned URL 발급, 분석 진행 상태 관리, 분석 결과 저장, 피드백 조회 API |
| 분석 Lambda | AWS Lambda (Python) | 영상에서 오디오/프레임 추출(FFmpeg), OpenAI API 호출, 분석 결과를 API 서버에 전달 |
| 변환 Lambda | AWS Lambda (Python) | S3 이벤트 수신 후 MediaConvert 작업 생성 |
| MediaConvert | AWS Elemental MediaConvert | WebM → 스트리밍 최적화 MP4(faststart) 변환 |
| 프론트엔드 | Vercel (React) | 면접 진행 UI, 영상 녹화, 피드백 뷰어 |
| 영상 저장 | S3 | 녹화 영상(WebM) + 변환 영상(MP4) 저장 |
| DB | RDS MySQL (프리티어) | 면접 데이터, 답변 구간, 분석 결과 저장 |
| AI | OpenAI API (Whisper, GPT-4o Vision, LLM) | STT, 비언어 분석, 언어 분석 |

### 아키텍처 원칙

- **DB 접근은 API 서버만 한다.** Lambda는 DB에 직접 접근하지 않는다. 커넥션 풀 관리를 API 서버에 일원화하기 위함.
- **Lambda는 API 서버의 내부 엔드포인트를 호출하여 상태 변경 및 결과 저장을 수행한다.**
- **S3 이벤트 기반으로 분석과 변환이 자동 트리거된다.** API 서버가 Lambda를 직접 호출하지 않는다.

### 인프라 운영 주의사항

**API 서버 메모리 관리 (t3.small, RAM 2GB):**
- JVM 옵션을 동일하게 설정: `-Xms512m -Xmx512m` (동일하게 설정해야 런타임 힙 리사이징으로 인한 GC 오버헤드를 방지)
- Swap 메모리를 1~2GB 설정하여 OOM 방어
- t3.micro (1GB)에서는 Spring Boot + HikariCP 커넥션 풀 유지만으로도 메모리가 부족하여 OOM 위험이 높으므로 t3.small을 최소 사양으로 권장

**내부 API 보안 (Lambda → API 서버):**
- `/api/internal/*` 엔드포인트는 외부 접근 차단
- 방법 1: API 서버를 VPC 내에 두고, Lambda도 같은 VPC에 배치하여 Security Group으로 Lambda의 접근만 허용
- 방법 2: 헤더에 `Internal-Api-Key`를 발급하여 API 서버에서 검증. Lambda 환경변수에 키를 저장
- 두 방법을 병행하면 보안 계층이 이중화됨

**S3 이벤트 멱등성 (Idempotency):**
- 영상이 재업로드되거나 덮어씌워질 때 S3 이벤트가 중복 발생할 수 있음
- 분석 Lambda는 시작 시 해당 질문 세트의 analysis_status를 확인하여, PENDING_UPLOAD가 아닌 경우(이미 ANALYZING이거나 COMPLETED) 분석을 스킵하고 즉시 종료해야 함
- 변환 Lambda도 동일하게 convert_status가 PENDING이 아닌 경우 스킵

**OpenAI API Rate Limit 제어:**
- 동시 사용자 5명이 각각 1개 질문 세트를 끝내면 ~800장의 이미지가 동시에 Vision API로 쏟아질 수 있음
- Lambda Reserved Concurrency를 설정하여 분석 Lambda가 동시에 3~5개 이상 뜨지 않도록 AWS 단에서 제어
- Lambda 내부에서 Vision API 배치 호출 간에 0.5~1초 딜레이를 삽입하여 RPM/TPM 제한 방어

**S3 스토리지 수명 주기 관리:**
- 녹화 원본 WebM과 변환 MP4가 쌓이면 S3 비용 증가
- S3 Lifecycle Rule 설정: 피드백 조회 완료 후 30일 경과 시 S3 Intelligent-Tiering 또는 Glacier로 전환
- 90일 경과 시 원본 WebM 삭제 (변환 MP4만 보존), 180일 경과 시 전체 삭제 (정책은 서비스 요구사항에 따라 조정)

**좀비 상태 감지 스케줄러 (API 서버):**
- Lambda가 progress를 STARTED나 ANALYZING으로 바꾼 뒤 크래시하면, 해당 질문 세트가 ANALYZING 상태에 영원히 머무르는 좀비 상태가 됨
- API 서버에서 Spring `@Scheduled`로 1분마다 좀비 감지 쿼리 실행:
```sql
SELECT id FROM question_set
WHERE analysis_status = 'ANALYZING'
AND updated_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)
```
- 조회된 질문 세트의 analysis_status를 FAILED로 변경, failure_reason = "ANALYSIS_TIMEOUT"
- 클라이언트가 다음 폴링 시 FAILED 상태를 받고 "분석에 실패했습니다. 다시 시도하시겠습니까?" + 재시도 버튼 표시
- 이를 위해 question_set 테이블에 `updated_at` 컬럼 필요 (progress가 업데이트될 때마다 갱신)

### 핵심 설계: 질문 세트 단위 녹화

60분 전체를 하나의 영상으로 녹화하지 않는다.
**질문 세트(원본 질문 1개 + 후속 질문 3개 = 답변 4개) 단위로 영상을 나누어 녹화**하고, 각 영상이 완료될 때마다 즉시 업로드 + 분석을 시작한다.

후속 질문은 항상 3개로 고정이다.

장점:
- 비용 절감: 비답변 구간(질문 읽기, 생각 시간) 녹화/분석이 사라짐
- UX 개선: 면접 진행 중에 앞쪽 질문의 분석이 이미 완료될 수 있음. 면접 종료 후 대기 시간 대폭 단축
- 스트리밍 변환 부담 감소: 짧은 영상 여러 개 → MediaConvert 비용 절감

녹화 단위 (1개 질문 세트):
```
원본 질문 출제 → "답변 시작" 클릭(녹화 시작) → 답변 → "답변 종료" 클릭
→ 후속 질문 1 출제 → "답변 시작" → 답변 → "답변 종료"
→ 후속 질문 2 출제 → "답변 시작" → 답변 → "답변 종료"
→ 후속 질문 3 출제 → "답변 시작" → 답변 → "답변 종료"(녹화 종료)
→ 즉시 S3 업로드 + 분석 시작
```

하나의 영상에 원본 답변 1개 + 후속 답변 3개 = 총 4개 답변이 포함되며, "답변 시작/종료" 버튼의 타임스탬프로 영상 내 각 구간을 구분한다.

---

## 전체 시스템 흐름

### 1단계 — 면접 시작

**[클라이언트 → API 서버]**

면접 세션 생성

```
POST /api/interviews
Response: {
  "interviewId": 42,
  "status": "IN_PROGRESS",
  "questionSets": [
    {
      "questionSetId": 1,
      "category": "RESUME",
      "mainQuestion": "자기소개를 해주세요",
      "followUpQuestions": [
        "그 프로젝트에서 본인의 역할은?",
        "팀원과 갈등이 있었다면 어떻게 해결했나요?",
        "그 경험에서 배운 점은?"
      ],
      "mainModelAnswer": "저는 OO대학교에서...",
      "followUpReferences": [
        { "type": "GUIDE", "content": "프로젝트 내 구체적 기여와 협업 방식을 설명하세요." },
        { "type": "GUIDE", "content": "갈등 상황을 객관적으로 설명하고 해결 과정을 단계적으로 서술하세요." },
        { "type": "GUIDE", "content": "기술적/비기술적 배움을 구분하여 구체적으로 설명하세요." }
      ]
    },
    {
      "questionSetId": 2,
      "category": "CS",
      "mainQuestion": "해시맵의 동작 원리를 설명해주세요",
      "followUpQuestions": [
        "해시 충돌은 어떻게 해결하나요?",
        "Java HashMap의 시간 복잡도는?",
        "ConcurrentHashMap과의 차이는?"
      ],
      "mainModelAnswer": "해시맵은 키-값 쌍을 저장하는 자료구조로...",
      "followUpReferences": [
        { "type": "MODEL_ANSWER", "content": "해시 충돌 해결 방식은 크게 체이닝과 개방 주소법이 있습니다..." },
        { "type": "MODEL_ANSWER", "content": "평균 O(1), 최악 O(n)이며 Java 8부터 트리화로..." },
        { "type": "MODEL_ANSWER", "content": "ConcurrentHashMap은 세그먼트 락 방식으로..." }
      ]
    },
    ...
  ]
}
```

---

### 2단계 — 질문 세트별 녹화 + 즉시 업로드 (면접 진행 중 반복)

질문 세트마다 아래 과정이 반복된다.

**[클라이언트]**

1. 원본 질문이 화면에 표시됨
2. 사용자가 "답변 시작" 클릭 → MediaRecorder 녹화 시작 + `mainAnswerStartMs = 0` 기록
3. 사용자가 "답변 종료" 클릭 → `mainAnswerEndMs` 기록
4. 후속 질문 1이 화면에 표시됨
5. "답변 시작" 클릭 → `followUp1AnswerStartMs` 기록
6. "답변 종료" 클릭 → `followUp1AnswerEndMs` 기록
7. 후속 질문 2가 화면에 표시됨
8. "답변 시작" → `followUp2AnswerStartMs` / "답변 종료" → `followUp2AnswerEndMs`
9. 후속 질문 3이 화면에 표시됨
10. "답변 시작" → `followUp3AnswerStartMs` / "답변 종료" → `followUp3AnswerEndMs` + MediaRecorder 녹화 종료

**[클라이언트 → API 서버]**

11. 해당 질문 세트의 메타데이터를 먼저 전송 (영상 업로드 전)

```
POST /api/interviews/42/question-sets/1/answers
Body: {
  "answers": [
    { "type": "MAIN", "startMs": 0, "endMs": 120000 },
    { "type": "FOLLOWUP_1", "startMs": 125000, "endMs": 200000 },
    { "type": "FOLLOWUP_2", "startMs": 205000, "endMs": 280000 },
    { "type": "FOLLOWUP_3", "startMs": 285000, "endMs": 360000 }
  ]
}

→ API 서버: DB에 답변 구간 4개 저장
→ API 서버: question_set.analysis_status = "PENDING_UPLOAD"
```

**[클라이언트 → API 서버 → S3]**

12. Presigned URL 요청 + S3에 영상 직접 업로드

```
POST /api/interviews/42/question-sets/1/upload-url
Response: { presignedUrl: "https://s3.../videos/42/qs_1.webm" }
```

클라이언트가 Presigned URL로 S3에 직접 PUT 업로드.
업로드 완료 → S3 이벤트 자동 발생 → 분석 + 변환 병렬 시작.

**이 시점에서 사용자는 다음 질문 세트로 넘어간다. 업로드와 분석은 백그라운드에서 진행.**

**실패 대응 — 녹화 + 업로드 단계:**
- 녹화 중 브라우저 크래시: MediaRecorder `ondataavailable`로 5초마다 Blob 청크를 IndexedDB에 임시 저장. 페이지 재진입 시 복구 여부 확인.
- 타임스탬프 드리프트(Drift): 사용자가 "답변 시작" 버튼을 누른 시간과 MediaRecorder가 실제로 인코딩을 시작하는 시간 사이에 수백ms~1초의 딜레이가 발생할 수 있음. 답변 구간 타임스탬프에 전후 1~2초의 패딩(버퍼)을 두고 저장하여 앞말이 잘리거나 엉뚱한 프레임이 지정되는 것을 방지.
- 업로드 중 브라우저 종료/새로고침: `window.beforeunload` 이벤트를 걸어 "아직 영상이 업로드 중입니다. 창을 닫으시겠습니까?" 경고창을 띄워 실수로 인한 업로드 중단을 방지.
- 메타데이터 전송 실패: 최대 3회 재시도. 실패 시 IndexedDB에 저장하고 다음 질문 진행. 면접 종료 후 일괄 재전송 시도.
- S3 업로드 실패: 질문별 영상은 짧아서(4~8분) Multipart 불필요. 3회 재시도. 실패 시 IndexedDB에 Blob 보관 + 면접 종료 후 재업로드 시도.
- Presigned URL 만료: 유효기간 15분. 만료 시 자동 재발급 후 재업로드.

---

### 3단계 — S3 이벤트로 분석 + 스트리밍 변환 병렬 시작

**[S3 → EventBridge → Lambda 2개 동시 트리거]**

S3에 질문 세트 영상이 업로드 완료되면 S3 Event Notification → EventBridge를 통해 두 Lambda가 동시에 트리거된다.
질문 세트마다 독립적으로 트리거되므로, 여러 질문 세트의 분석이 동시에 진행될 수 있다.

**이벤트 트리거 실패 대응:**
- EventBridge 전달 실패: DLQ(Dead Letter Queue) 설정 + CloudWatch 알람.
- Lambda 동시 호출 제한 도달: reserved concurrency 설정으로 최소 동시 실행 수 보장.
- 수동 재트리거:
```
POST /api/internal/interviews/{id}/question-sets/{qsId}/retry-analysis
→ API 서버에서 분석 Lambda를 AWS SDK로 수동 invoke
```

#### A. 분석 Lambda (질문 세트 1개당 약 2~5분)

원본 답변 1개 + 후속 답변 3개 = 4~8분 영상 기준.

**[분석 Lambda → API 서버]** 상태 변경 + 답변 구간 조회

멱등성 체크: Lambda는 시작 시 해당 질문 세트의 analysis_status를 확인한다. PENDING_UPLOAD가 아닌 경우(이미 ANALYZING이거나 COMPLETED) 중복 실행으로 판단하고 즉시 종료한다.

```
PUT /api/internal/interviews/42/question-sets/1/progress
Body: { "analysisProgress": "STARTED" }

GET /api/internal/interviews/42/question-sets/1/answers
Response: {
  "answers": [
    { "type": "MAIN", "startMs": 0, "endMs": 120000 },
    { "type": "FOLLOWUP_1", "startMs": 125000, "endMs": 200000 },
    { "type": "FOLLOWUP_2", "startMs": 205000, "endMs": 280000 },
    { "type": "FOLLOWUP_3", "startMs": 285000, "endMs": 360000 }
  ]
}
```

**[분석 Lambda → S3]** 영상 다운로드 + FFmpeg으로 추출

```
PUT /api/internal/interviews/42/question-sets/1/progress
Body: { "analysisProgress": "EXTRACTING" }
```

- 전체가 답변 구간이므로 밀도 차등화 불필요
- 모든 구간을 동일하게 3초 간격으로 프레임 추출 + 오디오 추출

**[분석 Lambda → OpenAI API]** 언어 + 비언어 분석 병렬 실행 (asyncio.gather)

음성 변환:
```
PUT /api/internal/interviews/42/question-sets/1/progress
Body: { "analysisProgress": "STT_PROCESSING" }
```
- 오디오 → Whisper API → 타임스탬프 포함 텍스트 변환
- 주의: 필러워드가 많거나 말이 빠르면 Whisper가 텍스트를 누락하거나 병합할 수 있음. 클라이언트가 제공한 startMs/endMs와 Whisper 반환 타임스탬프 간 오차가 발생할 수 있으므로, LLM 평가 프롬프트에 "타임스탬프는 근사값이며 전후 1~2초 오차가 있을 수 있다"고 명시하여 유연하게 평가하도록 유도

언어 분석:
```
PUT /api/internal/interviews/42/question-sets/1/progress
Body: { "analysisProgress": "VERBAL_ANALYZING" }
```
- 텍스트 → LLM(GPT-4o)에 전달하여 평가:
  - 답변 논리성 (두괄식 구조, STAR 기법 활용 여부)
  - 키워드 적절성 (직무 관련 핵심 용어 사용 빈도)
  - 필러워드 빈도 ("어...", "음...", "그니까..." 카운트)
  - 말투 평가 (존댓말/반말 혼용, 불확실한 표현 빈도, 문장 완결성)
  - 발화 속도 (구간별 분당 단어 수)
  - 답변 완성도 (질문에 대한 응답 충분성)
  - 원본-후속 답변 간 맥락 일관성 (4개 답변의 흐름을 함께 평가)
  - 후속 질문 대응력 (꼬리 질문에 대한 답변 깊이 변화)

비언어 분석:
```
PUT /api/internal/interviews/42/question-sets/1/progress
Body: { "analysisProgress": "NONVERBAL_ANALYZING" }
```
- 프레임 이미지 → GPT-4o Vision API (low detail 모드, 85토큰/장)
- 5~10장씩 배치로 묶어서 호출
- 평가 항목:
  - 시선 (Eye Contact): 카메라 응시 여부, 시선 회피 빈도
  - 표정 (Facial Expression): 자신감, 긴장, 평온함 등 감정 상태
  - 자세 (Posture): 어깨 기울어짐, 몸 흔들림, 손 제스처

종합 평가:
```
PUT /api/internal/interviews/42/question-sets/1/progress
Body: { "analysisProgress": "FINALIZING" }
```

**[분석 Lambda → API 서버]** 분석 결과 저장

```
POST /api/internal/interviews/42/question-sets/1/feedback
Body: {
  "questionSetScore": 75,
  "questionSetComment": "원본 질문에는 논리적으로 답변했으나, 후속 질문으로 갈수록 근거가 약해졌습니다.",
  "timestamps": [
    {
      "answerType": "MAIN",
      "startMs": 0,
      "endMs": 30000,
      "transcript": "저는...",
      "verbalScore": 80,
      "verbalComment": "두괄식 구조로 잘 시작했습니다.",
      "fillerWordCount": 1,
      "eyeContactScore": 75,
      "postureScore": 85,
      "expressionLabel": "자신감",
      "nonverbalComment": "카메라를 잘 응시하고 있습니다.",
      "overallComment": "..."
    },
    {
      "answerType": "FOLLOWUP_1",
      "startMs": 125000,
      "endMs": 155000,
      "transcript": "그때 제가...",
      "verbalScore": 70,
      "verbalComment": "...",
      "fillerWordCount": 2,
      "eyeContactScore": 65,
      "postureScore": 80,
      "expressionLabel": "평온",
      "nonverbalComment": "...",
      "overallComment": "..."
    },
    {
      "answerType": "FOLLOWUP_2",
      "startMs": 205000,
      "endMs": 240000,
      "transcript": "...",
      "verbalScore": 60,
      "verbalComment": "...",
      ...
    },
    {
      "answerType": "FOLLOWUP_3",
      "startMs": 285000,
      "endMs": 320000,
      "transcript": "...",
      "verbalScore": 55,
      "verbalComment": "후속 질문이 깊어질수록 답변이 추상적으로 변하고 있습니다.",
      ...
    },
    ...
  ]
}

→ API 서버: question_set_feedback + timestamp_feedback 저장
→ API 서버: question_set.analysis_status = "COMPLETED"
```

**실패 대응 — 분석 단계:**
- S3 다운로드 실패: 3회 재시도 (exponential backoff). 실패 시 analysis_status = "FAILED", failure_reason = "VIDEO_DOWNLOAD_FAILED".
- FFmpeg 추출 실패 (영상 손상): analysis_status = "FAILED", failure_reason = "VIDEO_CORRUPTED".
- Whisper API 실패: 3회 재시도. 전체 실패 시 언어 분석 스킵, 비언어 결과만으로 부분 피드백 생성.
- Vision API 실패: 배치 단위 3회 재시도. 실패 배치 스킵, 나머지 결과로 피드백 생성.
- LLM 응답 파싱 실패: 1회 재요청. 재실패 시 raw text 저장.
- API 키 만료/잔액 부족: Lambda 시작 시 헬스체크. 실패 시 즉시 FAILED + CloudWatch 알람.
- API 서버 호출 실패 (결과 저장): S3에 JSON 백업 저장 후 재시도. 3회 실패 시 FAILED + S3 백업으로 수동 복구 가능.
- Lambda 타임아웃: 4~8분 영상 기준 분석 2~5분. 타임아웃 가능성 매우 낮음.
- Lambda 크래시: AWS 비동기 호출 자동 2회 재시도 + DLQ. 10분 이상 ANALYZING → API 서버 스케줄러가 FAILED로 변경.
- 전체 실패 처리 원칙: 어떤 단계에서 실패하든 반드시 API 서버에 실패 상태 기록.
```
PUT /api/internal/interviews/42/question-sets/1/progress
Body: {
  "analysisProgress": "FAILED",
  "failureReason": "WHISPER_API_FAILED",
  "failureDetail": "429 Rate Limit Exceeded after 3 retries"
}
```

#### B. 변환 Lambda (질문 세트 1개당 약 30초~1분)

**[변환 Lambda → MediaConvert]**

- WebM → 스트리밍 최적화 MP4(faststart) 변환
- 4~8분 영상이므로 변환 매우 빠름

**[변환 Lambda → API 서버]** 변환 완료 알림

```
PUT /api/internal/interviews/42/question-sets/1/convert-status
Body: {
  "convertStatus": "COMPLETED",
  "streamingUrl": "s3://.../videos/42/qs_1.mp4"
}
```

**실패 대응 — 변환 단계:**
- MediaConvert 실패: 1회 재시도. 재실패 시 convert_status = "FAILED". 원본 WebM으로 재생 폴백.
- 변환 Lambda 크래시: 비동기 2회 자동 재시도 + DLQ.

---

### 4단계 — 면접 진행 중 + 종료 후 대기 UX

**[클라이언트]**

면접 진행 중에도 이전 질문의 분석이 백그라운드에서 돌고 있다.
면접이 끝나면 마지막 질문 세트의 분석만 기다리면 된다 (2~5분).

**a) 면접 진행 중 — 백그라운드 상태 추적**

```
GET /api/interviews/42/status
Response: {
  "interviewStatus": "IN_PROGRESS",
  "questionSets": [
    { "questionSetId": 1, "analysisStatus": "COMPLETED", "convertStatus": "COMPLETED" },
    { "questionSetId": 2, "analysisStatus": "ANALYZING", "analysisProgress": "NONVERBAL_ANALYZING", "convertStatus": "COMPLETED" },
    { "questionSetId": 3, "analysisStatus": "PENDING_UPLOAD", "convertStatus": "PENDING" }
  ]
}
```

**b) 면접 종료 후 — 분석 대기 페이지**
- 상단에 "AI가 면접 영상을 분석하고 있습니다. 약 2~5분 정도 소요됩니다." (마지막 질문 세트 기준)
- 질문 세트별 분석 상태를 개별 표시:
  - STARTED → "분석 준비 중..."
  - EXTRACTING → "음성/영상 추출 중..."
  - STT_PROCESSING → "음성을 텍스트로 변환 중..."
  - VERBAL_ANALYZING → "답변 내용을 분석 중..."
  - NONVERBAL_ANALYZING → "표정과 자세를 분석 중..."
  - FINALIZING → "종합 평가를 생성 중..."
- 이미 완료된 질문 세트는 결과 미리보기 가능

**c) 모범답변 확인**
- 질문 생성 시점에 이미 DB에 저장되어 있으므로 즉시 조회 가능

```
GET /api/interviews/42/questions-with-answers
Response: {
  "questionSets": [
    {
      "questionSetId": 1,
      "category": "RESUME",
      "mainQuestion": "자기소개를 해주세요",
      "followUpQuestions": [
        "그 프로젝트에서 본인의 역할은?",
        "팀원과 갈등이 있었다면 어떻게 해결했나요?",
        "그 경험에서 배운 점은?"
      ],
      "mainModelAnswer": "저는 OO대학교에서 컴퓨터공학을 전공하며...",
      "followUpReferences": [
        { "type": "GUIDE", "content": "프로젝트 내 구체적 기여와 협업 방식을 설명하세요." },
        { "type": "GUIDE", "content": "갈등 상황을 객관적으로 설명하고 해결 과정을 단계적으로 서술하세요." },
        { "type": "GUIDE", "content": "기술적/비기술적 배움을 구분하여 구체적으로 설명하세요." }
      ]
    },
    {
      "questionSetId": 2,
      "category": "CS",
      "mainQuestion": "해시맵의 동작 원리를 설명해주세요",
      "followUpQuestions": [
        "해시 충돌은 어떻게 해결하나요?",
        "Java HashMap의 시간 복잡도는?",
        "ConcurrentHashMap과의 차이는?"
      ],
      "mainModelAnswer": "해시맵은 키-값 쌍을 저장하는 자료구조로...",
      "followUpReferences": [
        { "type": "MODEL_ANSWER", "content": "해시 충돌 해결 방식은 크게 체이닝과 개방 주소법이 있습니다..." },
        { "type": "MODEL_ANSWER", "content": "평균 O(1), 최악 O(n)이며 Java 8부터 트리화로..." },
        { "type": "MODEL_ANSWER", "content": "ConcurrentHashMap은 세그먼트 락 방식으로..." }
      ]
    },
    ...
  ]
}
```

**d) 분석 완료 알림**
- 모든 질문 세트 완료 시 피드백 페이지로 자동 전환
- 일부 변환 실패 시: 분석만 완료되면 전환 (원본 WebM 폴백)
- 페이지 이탈 시: 브라우저 Web Notification API로 알림

**실패 시 UX:**
- 특정 질문 세트만 실패: "질문 3의 분석에 실패했습니다. 나머지 결과를 먼저 확인하시겠습니까?" + 해당 질문만 재시도 버튼
- 부분 완료: 완료된 질문 피드백 즉시 제공, 미완료 질문은 "분석 중" 또는 "실패" 표시

---

### 5단계 — 피드백 페이지 (질문 세트별 영상 나열)

**[클라이언트 → API 서버]**

```
GET /api/interviews/42/feedback
Response: {
  "overallScore": 72,
  "overallComment": "전반적으로 논리적인 답변이었으나...",
  "questionSets": [
    {
      "questionSetId": 1,
      "mainQuestion": "자기소개를 해주세요",
      "followUpQuestions": [
        "그 프로젝트에서 본인의 역할은?",
        "팀원과 갈등이 있었다면 어떻게 해결했나요?",
        "그 경험에서 배운 점은?"
      ],
      "questionSetScore": 75,
      "questionSetComment": "...",
      "streamingUrl": "https://s3.../videos/42/qs_1.mp4",
      "fallbackUrl": "https://s3.../videos/42/qs_1.webm",
      "mainAnswer": {
        "timestamps": [
          {
            "startMs": 0, "endMs": 30000,
            "transcript": "저는...",
            "verbalScore": 80, "verbalComment": "...",
            "fillerWordCount": 1,
            "eyeContactScore": 75, "postureScore": 85,
            "expressionLabel": "자신감",
            "nonverbalComment": "...",
            "overallComment": "..."
          },
          ...
        ]
      },
      "followUpAnswers": [
        {
          "questionIndex": 1,
          "questionText": "그 프로젝트에서 본인의 역할은?",
          "timestamps": [
            {
              "startMs": 125000, "endMs": 155000,
              "transcript": "그때 제가...",
              "verbalScore": 70, "verbalComment": "...",
              "fillerWordCount": 2,
              "eyeContactScore": 65, "postureScore": 80,
              "expressionLabel": "평온",
              "nonverbalComment": "...",
              "overallComment": "..."
            },
            ...
          ]
        },
        {
          "questionIndex": 2,
          "questionText": "팀원과 갈등이 있었다면 어떻게 해결했나요?",
          "timestamps": [...]
        },
        {
          "questionIndex": 3,
          "questionText": "그 경험에서 배운 점은?",
          "timestamps": [...]
        }
      ]
    },
    ...
  ]
}
```

**피드백 뷰어 화면 구성:**

레이아웃: 질문 세트별로 섹션 나열. 각 섹션 안에 영상 + 피드백.

각 질문 세트 섹션:
- 상단: 원본 질문 텍스트 + 질문 세트 점수
- 왼쪽: 해당 질문 세트의 영상 플레이어 (변환된 MP4 Presigned URL)
  - 하단 커스텀 타임라인 바
  - 원본 답변 / 후속 답변 1 / 후속 답변 2 / 후속 답변 3 구간을 색상으로 구분
  - 타임스탬프 구간별 점수에 따라 색상 표시 (초록=좋음, 노랑=보통, 빨강=개선필요)
  - 구간 클릭 시 해당 시점으로 점프
- 오른쪽: 피드백 패널
  - 원본 답변 / 후속 답변 1~3을 탭 또는 구분선으로 분리
  - 각 답변 안에서 타임스탬프 구간별 언어/비언어 피드백 카드
  - STT 텍스트 (필러워드 하이라이트)
  - 모범답변/답변 가이드 비교 보기 버튼
- 양방향 동기화:
  - 영상 재생 → 현재 구간 피드백 하이라이트 + 자동 스크롤
  - 피드백 클릭 → 영상 해당 시점 점프

**실패 대응 — 재생 단계:**
- MP4 Presigned URL 만료: 페이지 진입 시 발급 (1시간). 만료 시 자동 재발급.
- MP4 재생 실패: fallbackUrl(WebM)로 자동 전환.
- 미분석 질문 세트: "이 질문은 분석되지 않았습니다" + 재분석 요청 버튼.

---

## 모범답변 생성 방안

질문 세트는 두 가지 카테고리로 나뉜다: **이력서 기반 질문**과 **CS 기반 질문**.
카테고리에 따라 원본 질문과 후속 질문의 모범답변/가이드 생성 방식이 다르다.

### 이력서 기반 질문 (category = "RESUME")

경험, 프로젝트, 지원 동기 등 개인 맥락에 따라 답이 달라지는 질문.

**원본 질문:**
- 질문 생성 시점에 이력서 내용을 기반으로 맞춤형 모범답변 생성 → DB 저장
- LLM 프롬프트: "면접 질문 '{질문}'에 대해 다음 이력서를 기반으로 1분 분량의 모범답변을 작성해줘. 이력서: {이력서 내용}"

**후속 질문 3개:**
- 의도를 묻는 형태 ("왜 이렇게 했는지", "왜 이 기술을 선택했는지")이므로 정답 없음 → 답변 가이드 제공
- 질문 생성 시점에 이력서 맥락을 반영한 가이드를 각 후속 질문마다 생성 → DB 저장
- LLM 프롬프트: "면접 후속 질문 '{질문}'에 대한 답변 가이드를 작성해줘. 이력서 맥락: {관련 내용}. 방향성과 포함하면 좋은 요소를 안내해줘."

### CS 기반 질문 (category = "CS")

자료구조, 알고리즘, 네트워크, OS, 데이터베이스 등 기술 지식을 묻는 질문. 정답이 있다.

**원본 질문:**
- 질문 생성 시점에 LLM으로 모범답변 생성 → DB 저장
- LLM 프롬프트: "CS 면접 질문 '{질문}'에 대해 핵심 개념을 포함한 모범답변을 작성해줘. 면접관이 기대하는 키워드와 설명 깊이를 반영해줘."

**후속 질문 3개:**
- CS 후속 질문도 정답이 있음 (예: "그러면 해시 충돌은 어떻게 해결하나요?", "TCP와 UDP의 차이는?")
- 질문 생성 시점에 각 후속 질문마다 모범답변 생성 → DB 저장
- LLM 프롬프트: "CS 면접 후속 질문 '{질문}'에 대한 모범답변을 작성해줘. 핵심 개념과 예시를 포함해줘."

### 생성 방식 정리

| 카테고리 | 질문 유형 | 개수 | 생성 시점 | 제공 형태 | 저장 컬럼 |
|----------|-----------|------|-----------|-----------|-----------|
| 이력서 기반 | 원본 질문 | 1개 | 질문 생성 시 | 모범답변 (이력서 맞춤형) | main_model_answer |
| 이력서 기반 | 후속 질문 | 3개 | 질문 생성 시 | 답변 가이드 (방향성) | followup_references (JSON) |
| CS 기반 | 원본 질문 | 1개 | 질문 생성 시 | 모범답변 (정답) | main_model_answer |
| CS 기반 | 후속 질문 | 3개 | 질문 생성 시 | 모범답변 (정답) | followup_references (JSON) |

followup_references는 JSON 배열로, 카테고리에 따라 내용이 달라진다:
- 이력서 기반: `[{"type": "GUIDE", "content": "답변 방향성..."}, ...]`
- CS 기반: `[{"type": "MODEL_ANSWER", "content": "모범답변 내용..."}, ...]`

---

## API 서버 엔드포인트

### 외부 API (클라이언트 → API 서버)

| 엔드포인트 | 메서드 | 용도 |
|------------|--------|------|
| `/api/interviews` | POST | 면접 세션 생성 (질문 세트 목록 포함) |
| `/api/interviews/{id}/question-sets/{qsId}/upload-url` | POST | 질문 세트별 S3 Presigned URL 발급 |
| `/api/interviews/{id}/question-sets/{qsId}/answers` | POST | 질문 세트별 답변 구간 메타데이터 저장 (4개 구간) |
| `/api/interviews/{id}/status` | GET | 전체 면접 + 질문 세트별 분석/변환 상태 조회 |
| `/api/interviews/{id}/feedback` | GET | 전체 피드백 조회 |
| `/api/interviews/{id}/questions-with-answers` | GET | 모범답변/답변 가이드 조회 |

### 내부 API (Lambda → API 서버)

외부 비노출. Lambda 보안 그룹 또는 IAM 인증으로 접근 제한.

| 엔드포인트 | 메서드 | 용도 |
|------------|--------|------|
| `/api/internal/interviews/{id}/question-sets/{qsId}/answers` | GET | 답변 구간 메타데이터 조회 (4개 구간) |
| `/api/internal/interviews/{id}/question-sets/{qsId}/progress` | PUT | 분석 진행 상태 업데이트 |
| `/api/internal/interviews/{id}/question-sets/{qsId}/feedback` | POST | 분석 결과 저장 |
| `/api/internal/interviews/{id}/question-sets/{qsId}/convert-status` | PUT | 변환 상태 + URL 업데이트 |
| `/api/internal/interviews/{id}/question-sets/{qsId}/retry-analysis` | POST | 수동 분석 재트리거 |

---

## DB 스키마

### interview

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT PK | 면접 세션 ID |
| user_id | BIGINT FK | 사용자 ID |
| status | VARCHAR(20) | IN_PROGRESS / COMPLETED / FAILED |
| overall_score | INT | 전체 종합 점수 (0~100, nullable) |
| overall_comment | TEXT | 전체 종합 피드백 (nullable) |
| created_at | TIMESTAMP | 생성 시각 |

### question_set

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT PK | 질문 세트 ID |
| interview_id | BIGINT FK | 면접 ID |
| category | VARCHAR(10) | RESUME / CS |
| main_question_text | TEXT | 원본 질문 |
| followup_question_texts | JSON | 후속 질문 3개 배열 (예: ["질문1", "질문2", "질문3"]) |
| main_model_answer | TEXT | 원본 질문 모범답변 (nullable) |
| followup_references | JSON | 후속 질문별 모범답변 또는 가이드 3개 배열 (nullable). CS: [{"type":"MODEL_ANSWER","content":"..."}], 이력서: [{"type":"GUIDE","content":"..."}] |
| order_index | INT | 질문 순서 |
| video_url | TEXT | S3 원본 영상 경로 (WebM, nullable) |
| streaming_url | TEXT | S3 변환 영상 경로 (MP4, nullable) |
| analysis_status | VARCHAR(20) | PENDING / PENDING_UPLOAD / ANALYZING / COMPLETED / FAILED |
| analysis_progress | VARCHAR(30) | STARTED / EXTRACTING / STT_PROCESSING / VERBAL_ANALYZING / NONVERBAL_ANALYZING / FINALIZING / FAILED |
| convert_status | VARCHAR(20) | PENDING / CONVERTING / COMPLETED / FAILED |
| failure_reason | VARCHAR(50) | 실패 사유 코드 (nullable) |
| failure_detail | TEXT | 실패 상세 메시지 (nullable) |
| updated_at | TIMESTAMP | 마지막 상태 변경 시각 (좀비 감지용, progress 업데이트 시 갱신) |

### question_set_answer

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT PK | 답변 구간 ID |
| question_set_id | BIGINT FK | 질문 세트 ID |
| answer_type | VARCHAR(15) | MAIN / FOLLOWUP_1 / FOLLOWUP_2 / FOLLOWUP_3 |
| start_ms | BIGINT | 답변 시작 (ms) |
| end_ms | BIGINT | 답변 종료 (ms) |

### question_set_feedback

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT PK | 질문 세트 피드백 ID |
| question_set_id | BIGINT FK (UNIQUE) | 질문 세트 ID (1:1) |
| question_set_score | INT | 질문 세트 종합 점수 (0~100) |
| question_set_comment | TEXT | 질문 세트 종합 피드백 |
| created_at | TIMESTAMP | 분석 완료 시각 |

### timestamp_feedback

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT PK | 피드백 ID |
| question_set_feedback_id | BIGINT FK | 질문 세트 피드백 ID |
| question_set_answer_id | BIGINT FK | 답변 구간 ID (question_set_answer 참조, 데이터 무결성 보장) |
| answer_type | VARCHAR(15) | MAIN / FOLLOWUP_1 / FOLLOWUP_2 / FOLLOWUP_3 (비정규화, 조회 성능용) |
| start_ms | BIGINT | 피드백 구간 시작 (영상 내 타임스탬프) |
| end_ms | BIGINT | 피드백 구간 끝 |
| transcript | TEXT | STT 결과 텍스트 |
| verbal_score | INT | 언어 점수 (0~100, nullable) |
| verbal_comment | TEXT | 언어 피드백 |
| filler_word_count | INT | 필러워드 횟수 |
| eye_contact_score | INT | 시선 점수 (0~100, nullable) |
| posture_score | INT | 자세 점수 (0~100, nullable) |
| expression_label | VARCHAR(30) | 표정 라벨 (자신감/긴장/평온) |
| nonverbal_comment | TEXT | 비언어 피드백 |
| overall_comment | TEXT | 구간 종합 피드백 |
| is_analyzed | BOOLEAN | 분석 완료 여부 (부분 실패 시 false) |

---

## 비용 추정 (질문 세트 단위)

질문 세트 1개 (원본 2분 + 후속 3개 × 2분 = 8분 영상 기준):

| 항목 | 비용 |
|------|------|
| 프레임 추출 (3초 간격, 160장, low detail) | ~$0.04 |
| Whisper STT (8분 오디오) | ~$0.048 |
| Vision API 프롬프트 + 응답 토큰 | ~$0.10 |
| LLM 언어 분석 (4개 답변) | ~$0.06 |
| LLM 종합 평가 | ~$0.01 |
| MediaConvert 변환 (8분) | ~$0.12 |
| **질문 세트 1개 합계** | **~$0.38** |

5개 질문 세트 면접 기준: **~$1.9/면접**
10개 질문 세트 면접 기준: **~$3.8/면접**
