# Rehearse 프롬프트 A/B 테스트 실행 가이드

> Claude Code에게 시킬 때 이 문서를 컨텍스트로 함께 넘겨주세요.

---

## 1. 테스트 목적

레이어링(직무별 페르소나 + 기술스택 Overlay) 적용 전후로 질문 생성과 언어 분석의 품질 차이를 수치로 측정한다.

**검증 가설**: "레이어링을 적용한 프롬프트가 단일 페르소나 프롬프트보다 직무 전문성이 높은 질문과 피드백을 생성한다."

---

## 2. 테스트 구조

```
┌──────────────────────────────────────────────┐
│              동일한 테스트 조건                 │
│  position: BACKEND                            │
│  techStack: JAVA_SPRING                       │
│  level: JUNIOR                                │
│  interviewTypes: [JAVA_SPRING, CS_FUNDAMENTAL]│
│  csSubTopics: [OS, NETWORK]                   │
└──────────────┬───────────────────────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
   그룹 A (기존)     그룹 B (레이어링)
   단일 페르소나      Position×TechStack
   12개 유형 전부     선택 유형만 필터링
   범용 키워드        직무별 키워드 사전
       │               │
       ▼               ▼
   질문 생성 ×5회    질문 생성 ×5회
   언어 분석 ×5회    언어 분석 ×5회
       │               │
       └───────┬───────┘
               ▼
         품질 비교 평가
```

---

## 3. 사전 준비

### 3.1 프로젝트 구조 생성

Claude Code에게 첫 번째로 시킬 작업이다.

```
프롬프트:
"다음 디렉토리 구조를 만들어줘.

prompt-ab-test/
├── prompts/
│   ├── group_a/          ← 기존 프롬프트 (단일 페르소나)
│   │   ├── question_gen_system.txt
│   │   ├── question_gen_user.txt
│   │   ├── verbal_system.txt
│   │   └── verbal_user.txt
│   └── group_b/          ← 레이어링 프롬프트
│       ├── question_gen_system.txt
│       ├── question_gen_user.txt
│       ├── verbal_system.txt
│       └── verbal_user.txt
├── fixtures/
│   └── sample_answers.json   ← 언어 분석용 고정 답변 데이터
├── results/
│   └── (테스트 결과 저장)
├── scripts/
│   ├── run_question_gen_test.py
│   ├── run_verbal_test.py
│   └── evaluate_results.py
└── README.md"
```

### 3.2 프롬프트 파일 작성

Claude Code에게 두 번째로 시킬 작업이다. 아래 내용을 그대로 전달한다.

```
프롬프트:
"prompts/ 디렉토리에 A/B 그룹 프롬프트를 작성해줘.

## 그룹 A (기존) — question_gen_system.txt

기존 Rehearse 프로젝트의 단일 페르소나 프롬프트를 사용한다.
아래 원본을 그대로 넣어줘:

---
당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
주어진 직무, 레벨, 면접 유형에 맞는 면접 질문을 생성해야 합니다.

면접 유형별 출제 가이드:
- CS_FUNDAMENTAL: CS 기초 (자료구조, 알고리즘, 운영체제, 네트워크, 데이터베이스)
- BEHAVIORAL: STAR 기법 기반 경험 질문
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
- OS: 운영체제
- NETWORK: 네트워크
- DATABASE: 데이터베이스

질문 수 규칙:
- 면접 시간이 설정된 경우: (면접 시간(분) / 3) 반올림 (최소 2개, 최대 24개)
- 유형별로 균등 배분

레벨별 난이도:
- JUNIOR: 기본 개념 이해도 확인, 실무 경험보다 학습 의지
- MID: 실무 적용 능력, 문제 해결 경험, 기술적 깊이
- SENIOR: 아키텍처 판단력, 리더십, 기술 의사결정 능력

모범답변 생성 규칙:
- 각 질문에 대한 모범답변(modelAnswer)을 반드시 포함하세요.
- CS 카테고리 질문: referenceType을 "MODEL_ANSWER"로
- RESUME 카테고리 질문: referenceType을 "GUIDE"로
- questionCategory는 이력서/경험 기반이면 "RESUME", 기술/CS이면 "CS"

반드시 아래 JSON 형식으로만 응답하세요:
{
  \"questions\": [
    {
      \"content\": \"질문 내용\",
      \"category\": \"세부 카테고리명\",
      \"order\": 1,
      \"evaluationCriteria\": \"평가할 핵심 포인트\",
      \"questionCategory\": \"RESUME 또는 CS\",
      \"modelAnswer\": \"모범답변 또는 답변 가이드\",
      \"referenceType\": \"MODEL_ANSWER 또는 GUIDE\"
    }
  ]
}
---

## 그룹 B (레이어링) — question_gen_system.txt

레이어링이 적용된 프롬프트. Base(BACKEND) + Overlay(JAVA_SPRING) merge 결과:

---
당신은 한국 IT 기업에서 10년 이상 경력의 백엔드 시니어 개발자 면접관입니다.
서버 사이드 아키텍처 설계, 대규모 트래픽 처리, 데이터 정합성 보장에 대한 깊은 이해를 가지고 있습니다.
기술 스택에 관계없이 다음 역량을 중요하게 평가합니다:
- API 설계의 일관성과 확장성
- 동시성 제어와 데이터 정합성 보장 전략
- 장애 대응 경험과 운영 안정성에 대한 감각
- 성능 병목을 진단하고 해결하는 체계적 접근
특히 Java/Kotlin 언어와 Spring Boot 에코시스템에 깊은 전문성을 가지고 있습니다.
JVM 내부 동작, Spring IoC/AOP/트랜잭션 관리, JPA/Hibernate ORM에 대한 실무 경험이 풍부하며,
Spring Security, Spring Cloud 기반의 MSA 설계와 운영 경험이 있습니다.

면접 질문을 생성합니다.

## 평가 관점
- 코드 레벨: 동시성 제어, 트랜잭션 관리, 예외 처리 전략, 테스트 작성 습관
- 아키텍처 레벨: API 설계 원칙, 서비스 간 통신, 데이터 일관성 전략
- 운영 레벨: 장애 대응 경험, 성능 병목 진단, 모니터링/로깅 전략
- 성장 레벨: 기술 선택의 근거, 레거시 개선 경험, 코드 리뷰 문화

## 출제 가이드
- CS_FUNDAMENTAL: OS, 네트워크, 자료구조. Java 백엔드 관점에서 실무와 연결짓는 질문을 포함하세요. (예: \"TCP 3-way handshake가 Spring의 HikariCP 커넥션 풀 설계에 어떤 영향을 줄까요?\")
- JAVA_SPRING: JVM 메모리 구조 (힙/메타스페이스/스택), GC 알고리즘 (G1/ZGC/Shenandoah), Spring IoC 컨테이너 동작 원리, AOP 프록시 메커니즘, @Transactional 전파 속성, JPA 영속성 컨텍스트와 1차 캐시, N+1 문제 해결, 동시성 제어

## CS 세부 주제
OS(프로세스, 스레드, 메모리 관리, 스케줄링), NETWORK(TCP/IP, HTTP, DNS, 보안)에서만 출제.

## 난이도
JUNIOR: 기본 개념의 정확한 이해. 원리 중심 질문, CS 기초와 논리적 사고력 평가. 실무 경험보다 학습 의지.

## 모범답변
- CS 질문: referenceType=\"MODEL_ANSWER\", 핵심개념+실무예시 포함
- questionCategory: 기술/CS→\"CS\"

JSON만 응답. 형식:
{\"questions\":[{\"content\":\"\",\"category\":\"\",\"order\":1,\"evaluationCriteria\":\"2-3문장\",\"questionCategory\":\"RESUME|CS\",\"modelAnswer\":\"\",\"referenceType\":\"MODEL_ANSWER|GUIDE\"}]}
---

두 그룹의 User Prompt는 동일하게:

---
직무: 백엔드
레벨: 주니어
면접 유형: Java/Spring, CS 기초
질문 수: 6개
CS 세부 주제: 운영체제, 네트워크
세션 ID: test-session-001
이전 면접과 중복되지 않는 새로운 관점의 질문을 생성해주세요.
---

이렇게 작성해줘."
```

### 3.3 언어 분석용 고정 답변 데이터 작성

Claude Code에게 세 번째로 시킬 작업이다.

```
프롬프트:
"fixtures/sample_answers.json을 만들어줘.
언어 분석 A/B 테스트에 사용할 고정 답변 데이터야.
동일한 답변을 양쪽 프롬프트에 넣어서 피드백 품질을 비교할 거야.

다음 3개 질문-답변 쌍을 만들어줘:

1. 좋은 답변 (기술 용어 정확, 구조적)
   질문: 'JPA의 N+1 문제가 무엇이고 어떻게 해결할 수 있나요?'
   답변: fetch join, EntityGraph, BatchSize를 정확히 언급하는 구조적 답변

2. 보통 답변 (대략 맞지만 부정확한 용어 사용)
   질문: 'Spring의 @Transactional이 동작하는 원리를 설명해주세요'
   답변: 프록시 기반이라는 건 알지만 'AOP'를 '인터셉터'로 잘못 표현하고,
   전파 속성을 정확히 모르는 답변

3. 부족한 답변 (필러워드 많고 핵심 빗나감)
   질문: 'TCP 3-way handshake를 설명하고, 이것이 백엔드 서버 성능에 미치는 영향을 말해주세요'
   답변: '음... 그러니까... TCP는 뭐 연결을 하는 거고...' 식으로 
   필러워드가 많고 핵심을 빗나가는 답변

각 답변은 실제 STT 전사처럼 구어체로 작성해줘.
JSON 형식:
[
  {
    'id': 'good_answer',
    'question': '...',
    'transcript': '...',
    'expected_score_range': [80, 100],
    'expected_keywords': ['fetch join', 'EntityGraph', ...]
  },
  ...
]"
```

### 3.4 언어 분석 프롬프트 작성

```
프롬프트:
"prompts/ 디렉토리에 언어 분석 A/B 프롬프트도 작성해줘.

## 그룹 A (기존) — verbal_system.txt

---
당신은 면접 언어 분석 전문가입니다.
면접자의 답변 텍스트를 분석하여 언어적 커뮤니케이션을 평가합니다.

평가 기준:
1. 답변 논리성 (verbal_score: 0-100)
2. 필러워드 개수 (filler_word_count)
3. 핵심 키워드 활용
4. 말투/어조 적절성

반드시 아래 JSON 형식으로만 응답하세요:
{
  \"verbal_score\": 0,
  \"filler_word_count\": 0,
  \"tone_label\": \"\",
  \"tone_comment\": \"\",
  \"comment\": \"\"
}
---

## 그룹 B (레이어링) — verbal_system.txt

---
백엔드(Java/Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다.

## 전문 분야
Java/Spring 백엔드 답변 분석 기준:
- JVM, Spring, JPA 기술 용어의 정확한 사용
- 성능 수치(TPS, 응답시간, GC pause time) 구체적 언급
- '원인→해결→결과' 구조 설명 능력

키워드 사전:
JVM, GC, 힙 메모리, 메타스페이스, 스레드 풀, HikariCP, 커넥션 풀,
트랜잭션 격리 수준, @Transactional, 전파 속성, 롤백,
영속성 컨텍스트, 1차 캐시, 지연 로딩, 즉시 로딩, N+1, fetch join, EntityGraph,
Spring IoC, 빈 스코프, AOP, 프록시, CGLIB,
@Version, 낙관적 락, 비관적 락, 데드락

## 평가
1. verbal_score(0-100): 90+=핵심정확+기술깊이, 70+=대체로양호, 50+=핵심빗나감, 30+=이해부족/오류, 0+=무관
2. filler_word_count: '음','어','그','아','뭐','이제','약간','좀','그러니까' 등장 횟수
3. keyword_usage: 핵심 키워드 추출 + 정확성 평가. 오용 시 구체 지적.
4. tone_label: PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE

JSON만 응답:
{\"verbal_score\":0,\"filler_word_count\":0,\"keyword_usage\":{\"used_keywords\":[],\"accuracy\":\"\",\"keyword_comment\":\"\"},\"tone_label\":\"\",\"tone_comment\":\"\",\"comment\":\"\"}
---

양쪽 모두 User Prompt는 동일하게 fixtures/sample_answers.json의 데이터를 사용."
```

---

## 4. 테스트 스크립트 작성

### 4.1 질문 생성 A/B 테스트

Claude Code에게 시킬 작업이다.

```
프롬프트:
"scripts/run_question_gen_test.py를 작성해줘.

요구사항:
1. Anthropic API (claude-sonnet-4-20250514)를 사용
2. prompts/group_a/와 prompts/group_b/의 System+User 프롬프트를 각각 로드
3. 동일한 조건으로 각 그룹당 5회씩 호출 (temperature=0.9)
4. 각 호출의 응답 JSON을 파싱하여 results/question_gen_results.json에 저장
5. 저장 형식:

{
  'test_id': 'question_gen_001',
  'timestamp': '...',
  'conditions': {
    'position': 'BACKEND',
    'tech_stack': 'JAVA_SPRING',
    'level': 'JUNIOR',
    'types': ['JAVA_SPRING', 'CS_FUNDAMENTAL']
  },
  'results': {
    'group_a': [
      {
        'run': 1,
        'input_tokens': 1234,
        'output_tokens': 5678,
        'questions': [...파싱된 질문 목록...]
      },
      ...5회
    ],
    'group_b': [
      ...5회
    ]
  }
}

6. API 키는 환경변수 ANTHROPIC_API_KEY에서 읽기
7. 각 호출 사이에 1초 sleep
8. 에러 발생 시 해당 run을 skip하고 계속 진행"
```

### 4.2 언어 분석 A/B 테스트

```
프롬프트:
"scripts/run_verbal_test.py를 작성해줘.

요구사항:
1. Anthropic API (claude-sonnet-4-20250514)를 사용
   (실제 프로젝트에서는 GPT-4o지만, 테스트 편의상 Claude로 통일)
2. fixtures/sample_answers.json에서 3개 답변을 로드
3. 각 답변에 대해 group_a, group_b 프롬프트로 각각 3회씩 호출 (temperature=0.3)
4. 응답 JSON을 파싱하여 results/verbal_results.json에 저장
5. 저장 형식:

{
  'test_id': 'verbal_001',
  'timestamp': '...',
  'results': {
    'good_answer': {
      'group_a': [
        {'run': 1, 'verbal_score': 85, 'filler_word_count': 2, ...},
        ...3회
      ],
      'group_b': [
        {'run': 1, 'verbal_score': 90, 'filler_word_count': 2, 'keyword_usage': {...}, ...},
        ...3회
      ]
    },
    'medium_answer': {...},
    'poor_answer': {...}
  }
}"
```

---

## 5. 평가 스크립트 작성

### 5.1 자동 평가 지표

Claude Code에게 시킬 작업이다.

```
프롬프트:
"scripts/evaluate_results.py를 작성해줘.

results/question_gen_results.json과 results/verbal_results.json을 읽어서
다음 지표를 계산하고 results/evaluation_report.md를 생성해줘.

## 질문 생성 평가 지표

1. 직무 특화 키워드 비율 (Keyword Specificity Rate)
   - 각 질문의 content + modelAnswer에서 직무 특화 키워드가 몇 개 포함되었는지 계산
   - 직무 특화 키워드 목록 (BACKEND × JAVA_SPRING):
     JVM, GC, 힙, 메타스페이스, 스레드 풀, HikariCP, 커넥션 풀,
     @Transactional, 전파 속성, 영속성 컨텍스트, 1차 캐시,
     지연 로딩, 즉시 로딩, N+1, fetch join, EntityGraph,
     Spring IoC, AOP, 프록시, CGLIB, @Version, 낙관적 락, 비관적 락
   - 범용 키워드 목록 (어느 직무든 나올 수 있는 것):
     데이터베이스, 성능, 최적화, 설계, 아키텍처, 트래픽, 확장성
   - 계산: 직무특화키워드수 / (직무특화키워드수 + 범용키워드수)
   - 그룹 A vs B 평균 비교

2. 질문 깊이 점수 (Question Depth Score)
   - 각 질문의 content에서 다음 패턴을 카운트:
     * '왜' / '어떻게' / '비교' / '트레이드오프' / '차이' → 심화 질문 지표
     * '무엇' / '설명' / '정의' → 기본 질문 지표
   - 심화 비율 = 심화지표 / (심화지표 + 기본지표)

3. 실무 연결성 점수 (Practical Connection Score)
   - 질문에 구체적 기술명이 포함된 비율
   - 예: 'HikariCP', 'G1 GC', 'Resilience4j' 같은 구체적 도구/기술명

4. 토큰 사용량 비교
   - 그룹별 평균 input_tokens, output_tokens

## 언어 분석 평가 지표

5. 키워드 감지 능력 (Keyword Detection)
   - group_b에만 있는 keyword_usage 필드 분석
   - good_answer에서 expected_keywords를 잡아내는 비율
   - medium_answer에서 오용 키워드를 지적하는 비율

6. 점수 분별력 (Score Discrimination)
   - good/medium/poor 답변에 대한 verbal_score 차이
   - 이상적: good(80+) > medium(50-70) > poor(30 이하)
   - 그룹별로 이 분별이 얼마나 명확한지

7. 피드백 구체성 (Feedback Specificity)
   - comment 필드의 평균 글자 수
   - comment에 직무 특화 키워드가 포함된 비율

## 출력 형식

results/evaluation_report.md에 다음 구조로 작성:

# 프롬프트 A/B 테스트 결과 리포트

## 요약
| 지표 | 그룹 A (기존) | 그룹 B (레이어링) | 차이 |
|------|-------------|-----------------|------|
| ...  | ...         | ...             | ...  |

## 질문 생성 상세
### 직무 특화 키워드 비율
(그래프 대신 텍스트 표로)

### 질문 깊이 점수
...

### 실제 질문 비교 (대표 예시 3개)
그룹 A에서 나온 질문 vs 그룹 B에서 나온 질문 나란히 비교

## 언어 분석 상세
### 점수 분별력
...

### 키워드 감지 능력
...

## 결론
통계적으로 유의미한 차이가 있는 지표와 없는 지표를 구분하여 정리"
```

---

## 6. 실행 순서

Claude Code에게 순서대로 시킨다.

```
Step 1: "위 프로젝트 구조를 만들고 모든 프롬프트 파일을 작성해줘"
        → 결과 확인: prompts/ 디렉토리의 A/B 프롬프트, fixtures/ 데이터

Step 2: "run_question_gen_test.py를 실행해줘. API 키는 환경변수에 있어"
        → 결과 확인: results/question_gen_results.json

Step 3: "run_verbal_test.py를 실행해줘"
        → 결과 확인: results/verbal_results.json

Step 4: "evaluate_results.py를 실행해서 평가 리포트를 만들어줘"
        → 결과 확인: results/evaluation_report.md

Step 5: "평가 리포트를 보고 결과를 요약해줘.
        특히 그룹 B가 그룹 A보다 유의미하게 좋은 지표가 있는지,
        반대로 차이가 없거나 오히려 나빠진 지표가 있는지 분석해줘"
```

---

## 7. 결과 해석 가이드

### 7.1 레이어링이 유의미한 경우

다음 지표에서 그룹 B가 그룹 A보다 일관되게 높으면 레이어링의 효과가 입증된다.

| 지표 | 기대하는 차이 | 의미 |
|------|-------------|------|
| 직무 특화 키워드 비율 | B > A by 20%+ | 레이어링이 직무 전문성을 높인다 |
| 질문 깊이 점수 | B > A by 10%+ | 심화 질문이 더 많이 나온다 |
| 실무 연결성 점수 | B > A by 15%+ | 구체적 기술명이 더 많이 언급된다 |
| 점수 분별력 | B의 good-poor 차이 > A의 차이 | 피드백이 더 정확하게 분별한다 |
| 키워드 오용 감지 | B만 감지 가능 | A에는 키워드 사전이 없어서 구조적으로 불가 |

### 7.2 레이어링이 유의미하지 않은 경우

다음 상황이면 레이어링의 효과가 미미하거나 다른 접근이 필요하다.

| 상황 | 해석 | 대응 |
|------|------|------|
| 키워드 비율 차이 5% 미만 | 모델이 이미 직무를 잘 파악 | 페르소나보다 예시 질문 제공이 더 효과적일 수 있음 |
| B의 점수 분별력이 A와 동일 | 키워드 사전이 채점에 영향 미미 | 키워드 사전 대신 모범답변 비교 방식 검토 |
| B의 토큰이 A보다 현저히 적은데 품질 동일 | 토큰 절약 효과만 유의미 | 레이어링보다 조건부 블록 최적화에 집중 |

### 7.3 주의사항

- 5회 반복은 통계적 유의성을 보장하기엔 적다. 경향만 확인하는 것이고, 확신을 얻으려면 30회+ 반복이 필요하다.
- temperature=0.9 (질문 생성)에서는 동일 프롬프트라도 결과 편차가 크다. 분산도 함께 확인해야 한다.
- Claude로 GPT-4o 프롬프트를 테스트하는 것이므로, 실제 배포 시에는 GPT-4o로 재검증이 필요하다.

---

## 8. 추가 테스트 (선택)

기본 테스트가 끝난 후 확장할 수 있는 테스트들이다.

### 8.1 다른 직무 조합 테스트

```
프롬프트:
"같은 테스트를 FRONTEND × REACT_TS × MID 조건으로도 실행해줘.
그룹 B의 프롬프트는 프론트엔드 레이어링 프롬프트로 교체해야 해."
```

### 8.2 Overlay 유무 비교

```
프롬프트:
"그룹 C를 추가해줘. 그룹 C는 Base 프로필만 사용하고 Overlay 없이 테스트.
B(Base+Overlay) vs C(Base만)로 Overlay의 추가 효과를 측정."
```

### 8.3 페르소나 깊이 비교

```
프롬프트:
"후속 질문 프롬프트에서 FULL vs MEDIUM vs MINIMAL 페르소나를 비교해줘.
동일한 질문+답변에 대해 3가지 깊이로 각각 후속 질문을 생성하고,
품질 차이가 있는지 확인."
```
