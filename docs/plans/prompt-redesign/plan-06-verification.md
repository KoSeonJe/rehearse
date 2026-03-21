# Phase 6: 통합 검증 + A/B 테스트

> **상태**: TODO
> **브랜치**: `feat/prompt-redesign-tests`
> **PR**: PR-6 → develop
> **의존**: Phase 2 + Phase 4 완료 (PR-2, PR-4 머지 후)

---

## 개요

프롬프트 재설계의 전체 파이프라인을 검증하고, A/B 테스트로 품질 개선을 수치화한다.

---

## Task 6-1: BE 통합 테스트

- **Implement**: `test-engineer`
- **Review**: `qa`

### 테스트 파일

- 신규/수정: `backend/src/test/java/com/rehearse/api/infra/ai/persona/PersonaResolverTest.java`
- 신규/수정: `backend/src/test/java/com/rehearse/api/infra/ai/prompt/QuestionGenerationPromptBuilderTest.java`
- 신규/수정: `backend/src/test/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilderTest.java`
- 수정: `backend/src/test/java/com/rehearse/api/domain/interview/entity/InterviewTest.java`

### 검증 항목

#### PersonaResolver

| 테스트 케이스 | 기대 결과 |
|-------------|----------|
| BACKEND + JAVA_SPRING | fullPersona에 "백엔드" + "Java/Kotlin" 포함 |
| FRONTEND + REACT_TS | fullPersona에 "프론트엔드" + "React" 포함 |
| BACKEND + null overlay | base only 프로필, verbalExpertise는 null/빈값 |
| 5 Position × 기본 TechStack 전체 | 모두 정상 resolve |
| getPersona(FULL) | 전체 페르소나 반환 |
| getPersona(MEDIUM) | 1-2줄 축약 반환 |
| getPersona(MINIMAL) | 1문장 반환 |

#### QuestionGenerationPromptBuilder

| 테스트 케이스 | 기대 결과 |
|-------------|----------|
| interviewTypes에 CS_FUNDAMENTAL 포함 + csSubTopics=[OS,NETWORK] | "## CS 세부 주제" 블록 존재 |
| interviewTypes에 CS_FUNDAMENTAL 미포함 | CS 블록 없음 |
| resumeText 있음 | "## 이력서 활용" 블록 존재 |
| resumeText null | 이력서 블록 없음 |
| interviewTypes=[JAVA_SPRING, SYSTEM_DESIGN] | 해당 2개 가이드만 포함, 나머지 없음 |
| level=JUNIOR | "JUNIOR:" 가이드만 포함, MID/SENIOR 없음 |

#### FollowUpPromptBuilder

| 테스트 케이스 | 기대 결과 |
|-------------|----------|
| BACKEND + JAVA_SPRING | MEDIUM 페르소나에 "백엔드(Java/Spring)" 포함 |
| previousExchanges 있음 | "이전 후속:" 섹션 포함 |
| previousExchanges 빈 리스트 | "이전 후속:" 섹션 없음 |

#### Interview 엔티티

| 테스트 케이스 | 기대 결과 |
|-------------|----------|
| techStack=null, position=BACKEND | getEffectiveTechStack() → JAVA_SPRING |
| techStack=PYTHON_DJANGO | getEffectiveTechStack() → PYTHON_DJANGO |
| techStack=null, position=FRONTEND | getEffectiveTechStack() → REACT_TS |

---

## Task 6-2: 하위 호환성 E2E 검증

- **Implement**: `qa`
- **Review**: `verifier`

### 시나리오

#### 시나리오 1: 기존 데이터 호환 (techStack=null)

```
1. 기존 interview (techStack=null, position=BACKEND) 조회
2. → getEffectiveTechStack() = JAVA_SPRING 확인
3. → 후속 질문 생성 시 JAVA_SPRING 페르소나 적용 확인
```

#### 시나리오 2: 새 인터뷰 (techStack 지정)

```
1. POST /api/v1/interviews { position: BACKEND, techStack: PYTHON_DJANGO, ... }
2. → interview.techStack = PYTHON_DJANGO 저장 확인
3. → 질문 생성 시 Python/Django 페르소나 + 키워드 사전 적용 확인
```

#### 시나리오 3: Lambda 분석 파이프라인

```
1. 면접 녹화 + S3 업로드
2. Lambda 트리거
3. GET /api/internal/.../answers 응답에 position, techStack, level 포함 확인
4. verbal_analyzer 로그에서 해당 스택 키워드 사전 사용 확인
5. 피드백 저장 정상 확인
```

#### 시나리오 4: FE 위저드

```
1. Position 선택 → TechStack 스텝 표시 확인
2. TechStack 선택/미선택 → API 요청 확인
3. Position 변경 → TechStack 리셋 확인
```

---

## Task 6-3: A/B 테스트 (prompt-test-guide.md 기반)

- **Implement**: `scientist`
- **Review**: `code-reviewer`

### 테스트 절차

참조: `docs/plans/prompt-redesign/background/prompt-test-guide.md`

#### 사전 준비

```
prompt-ab-test/
├── prompts/
│   ├── group_a/          ← 기존 프롬프트 (단일 페르소나)
│   └── group_b/          ← 레이어링 프롬프트 (Position×TechStack)
├── fixtures/
│   └── sample_answers.json
├── results/
├── scripts/
│   ├── run_question_gen_test.py
│   ├── run_verbal_test.py
│   └── evaluate_results.py
└── README.md
```

#### 테스트 조건

```
position: BACKEND
techStack: JAVA_SPRING
level: JUNIOR
interviewTypes: [JAVA_SPRING, CS_FUNDAMENTAL]
csSubTopics: [OS, NETWORK]
```

#### 실행

| 단계 | 스크립트 | 반복 | temperature |
|------|---------|------|------------|
| 질문 생성 | run_question_gen_test.py | 5회/그룹 | 0.9 |
| 언어 분석 | run_verbal_test.py | 3회/답변/그룹 | 0.3 |
| 평가 | evaluate_results.py | 1회 | - |

#### 평가 지표

| 지표 | 기대하는 차이 | 의미 |
|------|-------------|------|
| 직무 특화 키워드 비율 | B > A by 20%+ | 레이어링이 직무 전문성을 높인다 |
| 질문 깊이 점수 | B > A by 10%+ | 심화 질문이 더 많다 |
| 실무 연결성 점수 | B > A by 15%+ | 구체적 기술명이 더 많다 |
| 점수 분별력 | B의 good-poor 차이 > A | 피드백이 더 정확하게 분별 |
| 키워드 오용 감지 | B만 감지 가능 | A에는 키워드 사전이 없음 |
| 토큰 사용량 | B < A by ~35% | 조건부 블록 + 압축 효과 |

#### 결과물

- `prompt-ab-test/results/question_gen_results.json`
- `prompt-ab-test/results/verbal_results.json`
- `prompt-ab-test/results/evaluation_report.md`

### 주의사항

- 5회 반복은 통계적 유의성 부족 → 경향 확인용
- 확신 필요 시 30회+ 추가 반복
- Claude로 GPT-4o 프롬프트 테스트 → 실 배포 시 GPT-4o로 재검증 필요

---

## 토큰 모니터링

### 배포 후 확인

ClaudeApiClient에 이미 토큰 사용량 로그가 있음 (L141):

```java
log.info("Claude API 호출 완료: input_tokens={}, output_tokens={}", inputTokens, outputTokens);
```

v2 → v3 전환 전후 비교:

| 프롬프트 | v2 예상 | v3 예상 | 목표 절약률 |
|---------|--------|--------|-----------|
| 질문 생성 System | ~1,161 tok | ~732 tok | 37% |
| 후속 질문 System | ~630 tok | ~435 tok | 31% |
| 언어 분석 System | ~720 tok | ~419 tok | 42% |
| 비언어 분석 System | ~468 tok | ~263 tok | 44% |
| **합계** | **~2,979** | **~1,849** | **38%** |

배포 1주일 후 실제 토큰 로그와 비교하여 차이 분석.
