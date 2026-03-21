# Rehearse 프롬프트 재설계 v3 — 토큰 최적화 적용

## 목차
1. [토큰 최적화 분석 결과](#1-토큰-최적화-분석-결과)
2. [최적화 전략 7가지](#2-최적화-전략-7가지)
3. [최적화된 질문 생성 프롬프트](#3-최적화된-질문-생성-프롬프트)
4. [최적화된 후속 질문 프롬프트](#4-최적화된-후속-질문-프롬프트)
5. [최적화된 언어 분석 프롬프트](#5-최적화된-언어-분석-프롬프트)
6. [최적화된 비언어 분석 프롬프트](#6-최적화된-비언어-분석-프롬프트)
7. [구현: 조건부 블록 조립 로직](#7-구현-조건부-블록-조립-로직)
8. [전후 비교 요약](#8-전후-비교-요약)

---

## 1. 토큰 최적화 분석 결과

### 1.1 v2 현재 토큰 사용량 (BACKEND × JAVA_SPRING 기준)

| 프롬프트 | 문자 수 | 추정 토큰 |
|---------|---------|----------|
| 질문 생성 System | 2,629자 | ~1,161 |
| 후속 질문 System | 1,356자 | ~630 |
| 언어 분석 System | 1,739자 | ~720 |
| 비언어 분석 System | 1,082자 | ~468 |
| **합계** | **6,806자** | **~2,979** |

### 1.2 발견된 낭비 패턴

| # | 낭비 패턴 | 낭비량 | 원인 |
|---|----------|--------|------|
| 1 | 페르소나 중복 | ~400 tok | 질문생성/후속/언어분석 3곳에서 풀 페르소나 반복 |
| 2 | 레벨 가이드 과잉 | ~129 tok | JUNIOR 면접인데 MID/SENIOR 가이드도 포함 |
| 3 | CS 블록 불필요 포함 | ~111 tok | CS_FUNDAMENTAL 유형이 아닌데 CS 세부주제 블록 포함 |
| 4 | 유형 가이드 과잉 | ~89 tok | 5개 유형 전체 포함, 선택된 2개만 필요 |
| 5 | JSON 스키마 장황 | ~52 tok | 들여쓰기+설명, 모델은 키 이름만으로 이해 가능 |
| 6 | 루브릭 과잉 | ~66 tok | 5단계 상세 설명, 압축 가능 |
| **합계** | | **~847 tok** | **전체의 ~28%** |

---

## 2. 최적화 전략 7가지

### 전략 1: 프롬프트별 페르소나 축약 (절약: ~400 tok)

**문제:** 질문 생성, 후속 질문, 언어 분석 3곳에서 동일한 풀 페르소나(~200 tok)를 반복.

**해결:** 프롬프트 역할에 따라 페르소나 깊이를 차등 적용.

| 프롬프트 | 페르소나 수준 | 이유 |
|---------|-------------|------|
| 질문 생성 | FULL (~200 tok) | 질문의 전문성을 결정하므로 풀 컨텍스트 필요 |
| 후속 질문 | MEDIUM (~80 tok) | 원래 질문+답변이 이미 맥락 제공 |
| 언어 분석 | MINIMAL (~40 tok) | 키워드 사전이 핵심, 페르소나는 방향성만 |

```
# FULL (질문 생성)
당신은 한국 IT 기업에서 10년 이상 경력의 백엔드 시니어 개발자 면접관입니다.
서버 사이드 아키텍처 설계, 대규모 트래픽 처리, 데이터 정합성 보장에 대한 깊은 이해를 가지고 있습니다.
... (전체 기술 스택 상세)

# MEDIUM (후속 질문)
당신은 백엔드(Java/Spring) 시니어 면접관입니다. API 설계, 동시성, 장애 대응, JVM/JPA 심화를 중시합니다.

# MINIMAL (언어 분석)
백엔드(Java/Spring) 면접 답변을 분석합니다.
```

---

### 전략 2: 레벨 가이드 필터링 (절약: ~129 tok)

**문제:** 3개 레벨(JUNIOR/MID/SENIOR) 가이드를 항상 전부 포함.

**해결:** 해당 레벨의 가이드만 주입. 코드에서 level 기반 분기.

```java
// Before: 항상 3개 포함
String levelGuide = JUNIOR_GUIDE + MID_GUIDE + SENIOR_GUIDE;

// After: 해당 레벨만
String levelGuide = switch (level) {
    case JUNIOR -> JUNIOR_GUIDE;
    case MID -> MID_GUIDE;
    case SENIOR -> SENIOR_GUIDE;
};
```

---

### 전략 3: CS 블록 조건부 제거 (절약: ~111 tok)

**문제:** interviewTypes에 CS_FUNDAMENTAL이 없어도 CS 세부주제 블록이 항상 포함.

**해결:** CS_FUNDAMENTAL이 선택된 경우에만 블록 주입.

```java
String csBlock = interview.getInterviewTypes().contains(CS_FUNDAMENTAL)
    ? CS_SUBTOPIC_BLOCK
    : "";  // 완전 제거
```

---

### 전략 4: 면접 유형 가이드 필터링 (절약: ~89 tok)

**문제:** overlay에 정의된 5개 유형 가이드를 전부 포함. 면접에서 2개만 선택해도 5개 모두 들어감.

**해결:** 선택된 interviewTypes에 해당하는 가이드만 추출.

```java
// 유형별 가이드를 Map으로 관리
Map<InterviewType, String> typeGuides = overlay.getInterviewTypeGuideMap();

// 선택된 유형만 필터링
String filteredGuide = interview.getInterviewTypes().stream()
    .filter(typeGuides::containsKey)
    .map(type -> "- " + type.name() + ": " + typeGuides.get(type))
    .collect(Collectors.joining("\n"));
```

이를 위해 overlay YAML 구조도 변경:

```yaml
# Before: 하나의 문자열
interview_type_guide: |
  - CS_FUNDAMENTAL: ...
  - BEHAVIORAL: ...
  - JAVA_SPRING: ...

# After: 유형별 Map
interview_type_guide:
  CS_FUNDAMENTAL: "OS, 네트워크, 자료구조. Java 관점 실무 연결 질문 포함"
  BEHAVIORAL: "장애 대응, 코드 리뷰, 기술 부채 등 팀 맥락 경험 질문"
  RESUME_BASED: "이력서의 Spring 프로젝트, JPA, 성능 개선 수치 기반"
  JAVA_SPRING: "JVM 메모리, GC, Spring IoC/AOP, @Transactional, JPA N+1..."
  SYSTEM_DESIGN: "Java/Spring 에코시스템 기반 대규모 시스템 설계"
```

---

### 전략 5: JSON 스키마 압축 (절약: ~52 tok)

**문제:** 들여쓰기, 설명 텍스트, placeholder 값이 토큰을 소비.

**해결:** 한 줄 스키마 + 필드 설명을 스키마 밖에 한 번만 기술.

```
# Before (~92 tokens)
반드시 아래 JSON 형식으로만 응답하세요:
{
  "questions": [
    {
      "content": "질문 내용",
      "category": "세부 카테고리명",
      "order": 1,
      "evaluationCriteria": "이 질문에서 평가할 핵심 포인트 (2-3문장)",
      "questionCategory": "RESUME 또는 CS",
      "modelAnswer": "모범답변 또는 답변 가이드",
      "referenceType": "MODEL_ANSWER 또는 GUIDE"
    }
  ]
}

# After (~40 tokens)
JSON만 응답. 형식:
{"questions":[{"content":"","category":"","order":1,"evaluationCriteria":"2-3문장","questionCategory":"RESUME|CS","modelAnswer":"","referenceType":"MODEL_ANSWER|GUIDE"}]}
```

> 주의: Claude와 GPT-4o 모두 한 줄 JSON 스키마를 정확히 파싱할 수 있음. 들여쓰기는 사람을 위한 것이지 모델을 위한 것이 아님.

---

### 전략 6: 루브릭 압축 (절약: ~66 tok)

**문제:** 5단계 점수 루브릭이 각각 1-2문장씩 상세 설명.

**해결:** 한 줄 압축 루브릭. 모델은 점수 범위와 키워드만으로 충분히 분별 가능.

```
# Before (~86 tokens)
점수 기준:
- 90-100: 질문의 핵심을 정확히 파악, 기술적 깊이와 구조를 갖춘 모범적 답변
- 70-89: 핵심 포함, 일부 깊이 부족하거나 구조 느슨
- 50-69: 관련 내용 언급하나 핵심을 빗나가거나 기대 수준 미달
- 30-49: 질문 이해 부족 또는 기술적 오류 포함
- 0-29: 무관하거나 답변 불가

# After (~20 tokens)
채점: 90+=핵심정확+기술깊이, 70+=대체로양호, 50+=핵심빗나감, 30+=이해부족/오류, 0+=무관
```

---

### 전략 7: 이력서 블록 조건부 제거 (절약: ~40 tok)

**문제:** 이력서 미제공 시에도 "이력서가 제공되면~" 안내 문구 포함.

**해결:** 이력서가 있을 때만 블록 주입.

```java
String resumeBlock = hasResume
    ? "이력서 기반(RESUME_BASED) 질문은 이력서의 프로젝트, 기술, 성과를 구체적으로 언급하여 생성하세요."
    : "";
```

---

## 3. 최적화된 질문 생성 프롬프트

> **모델:** claude-sonnet-4-20250514 | **Temperature:** 0.9 | **Max Tokens:** 4096

### 3.1 System Prompt 템플릿

```
{FULL_PERSONA}

면접 질문을 생성합니다.

## 평가 관점
{BASE_EVALUATION_PERSPECTIVE}

## 출제 가이드
{FILTERED_INTERVIEW_TYPE_GUIDE}

{CONDITIONAL_CS_SUBTOPIC_BLOCK}

## 난이도
{SINGLE_LEVEL_GUIDE}

## 질문 수
(면접시간(분)÷3) 반올림, 최소2 최대24. 유형별 균등 배분.

{CONDITIONAL_RESUME_BLOCK}

## 모범답변
- CS 질문: referenceType="MODEL_ANSWER", 핵심개념+실무예시 포함
- RESUME 질문: referenceType="GUIDE", 답변방향+좋은답변 조건
- questionCategory: 이력서/경험→"RESUME", 기술/CS→"CS"

## 응답
JSON만 응답. 형식:
{"questions":[{"content":"","category":"","order":1,"evaluationCriteria":"2-3문장","questionCategory":"RESUME|CS","modelAnswer":"","referenceType":"MODEL_ANSWER|GUIDE"}]}
```

### 3.2 조건부 블록 정의

#### CONDITIONAL_CS_SUBTOPIC_BLOCK

CS_FUNDAMENTAL이 interviewTypes에 포함된 경우에만 주입:

```
## CS 세부 주제
{csSubTopics}에서만 출제.
```

> csSubTopics 예시: "DATA_STRUCTURE(자료구조/알고리즘), NETWORK(TCP/IP, HTTP, DNS, 보안)"
> 지정된 주제만 나열하여 추가 절약.

CS_FUNDAMENTAL이 없으면 이 블록 전체를 빈 문자열로 처리.

#### CONDITIONAL_RESUME_BLOCK

이력서가 제공된 경우에만 주입:

```
## 이력서 활용
RESUME_BASED 질문은 이력서의 프로젝트, 기술, 성과를 구체적으로 언급하여 생성.
```

이력서가 없으면 빈 문자열.

#### SINGLE_LEVEL_GUIDE

해당 레벨 1개만 주입:

```java
// JUNIOR인 경우:
"JUNIOR: 기본 개념의 정확한 이해. 원리 중심 질문, CS 기초와 논리적 사고력 평가. 실무 경험보다 학습 의지."

// MID인 경우:
"MID: 실무 적용 경험과 문제 해결. 기술 선택 이유, 트레이드오프, 장애 시 판단력 평가."

// SENIOR인 경우:
"SENIOR: 아키텍처 의사결정, 팀 리딩, 기술 방향성. 시스템 조망, 조직 기술 영향력 평가."
```

#### FILTERED_INTERVIEW_TYPE_GUIDE

선택된 interviewTypes만 필터링하여 주입:

```
// interviewTypes = [JAVA_SPRING, SYSTEM_DESIGN]인 경우:
- JAVA_SPRING: JVM 메모리, GC, Spring IoC/AOP, @Transactional 전파, JPA N+1 해결, 동시성 제어
- SYSTEM_DESIGN: Java/Spring 기반 대규모 시스템 설계. Spring Cloud Gateway, Resilience4j, Kafka 맥락 포함
```

### 3.3 User Prompt

```
직무: {positionKorean} ({techStackDisplayName})
레벨: {levelKorean}
유형: {selectedTypesKorean}
질문 수: {questionCount}개
{conditionalCsSubTopics}
{conditionalResumeText}
세션: {UUID}
중복 없는 새 관점의 질문을 생성하세요.
```

> User Prompt에서도 줄바꿈/레이블 최소화. "면접 유형:" → "유형:", "세션 ID:" → "세션:"

### 3.4 토큰 비교 (BACKEND × JAVA_SPRING, JUNIOR, [JAVA_SPRING, SYSTEM_DESIGN], 이력서 없음)

| 구간 | v2 | v3 | 절약 |
|------|-----|-----|------|
| 페르소나 | ~200 | ~200 | 0 (질문생성은 FULL 유지) |
| 평가 관점 | ~90 | ~90 | 0 |
| 유형 가이드 | ~136 | ~47 | ~89 |
| CS 블록 | ~111 | 0 | ~111 |
| 레벨 가이드 | ~152 | ~35 | ~117 |
| 이력서 블록 | ~40 | 0 | ~40 |
| 모범답변 규칙 | ~60 | ~40 | ~20 |
| JSON 스키마 | ~92 | ~40 | ~52 |
| **System 합계** | **~1,161** | **~732** | **~429 (37%)** |

---

## 4. 최적화된 후속 질문 프롬프트

> **모델:** claude-sonnet-4-20250514 | **Temperature:** 1.0 | **Max Tokens:** 1024

### 4.1 System Prompt 템플릿

```
{MEDIUM_PERSONA}

답변 기반으로 후속 질문을 1개 생성합니다.

## 후속 유형
DEEP_DIVE(기술심화) | CLARIFICATION(모호함구체화) | CHALLENGE(약점/대안탐색) | APPLICATION(다른상황적용)

## 심화 방향
{BASE_FOLLOWUP_DEPTH}
{STACK_OVERLAY_FOLLOWUP_DEPTH}

## 규칙
- 질문 1개만. 복합 질문 금지.
- 이전 후속 질문과 다른 유형 사용.
- 모범답변(modelAnswer) 2-4문장 포함.

## 응답
JSON만 응답. 형식:
{"question":"","reason":"","type":"DEEP_DIVE|CLARIFICATION|CHALLENGE|APPLICATION","modelAnswer":""}
```

### 4.2 MEDIUM_PERSONA 예시 (BACKEND × JAVA_SPRING)

```
백엔드(Java/Spring) 시니어 면접관. API 설계, 동시성, 장애 대응에 깊은 전문성. JVM 내부, Spring IoC/AOP/트랜잭션, JPA 심화를 중시합니다.
```

> FULL 대비 ~60% 축약. 원래 질문+답변이 User Prompt에 이미 포함되므로 페르소나는 방향성만 제시하면 충분.

### 4.3 User Prompt

```
직무: {positionKorean} ({techStackDisplayName}) | 레벨: {levelKorean}

질문: {questionContent}
답변: {answerText}
비언어: {nonVerbalSummary|"없음"}

{conditionalPreviousFollowUps}

새 후속 질문을 생성하세요.
```

> "원래 질문:" → "질문:", "면접자 답변:" → "답변:", "비언어적 관찰:" → "비언어:"

### 4.4 토큰 비교

| 구간 | v2 | v3 | 절약 |
|------|-----|-----|------|
| 페르소나 | ~200 | ~80 | ~120 |
| 유형 설명 | ~60 | ~30 | ~30 |
| 심화 방향 | ~130 | ~130 | 0 |
| 규칙 | ~50 | ~30 | ~20 |
| JSON 스키마 | ~50 | ~25 | ~25 |
| **System 합계** | **~630** | **~435** | **~195 (31%)** |

---

## 5. 최적화된 언어 분석 프롬프트

> **모델:** gpt-4o | **Temperature:** 0.3 | **Max Tokens:** 500

### 5.1 System Prompt 템플릿

```
{MINIMAL_PERSONA}

## 전문 분야
{MERGED_VERBAL_EXPERTISE}

## 평가
1. verbal_score(0-100): 채점: 90+=핵심정확+기술깊이, 70+=대체로양호, 50+=핵심빗나감, 30+=이해부족/오류, 0+=무관
   평가요소: 핵심답변포함, 구조화(STAR), 기술키워드정확성, 논리흐름, 구체적수치/사례
2. filler_word_count: "음","어","그","아","뭐","이제","약간","좀","그러니까" 등장 횟수
3. keyword_usage: 핵심 키워드 추출 + 정확성 평가. 오용 시 구체 지적.
4. tone_label: PROFESSIONAL(격식) | CASUAL(구어체) | HESITANT(불확신) | CONFIDENT(확신) | VERBOSE(장황)

## 응답
JSON만 응답. 형식:
{"verbal_score":0,"filler_word_count":0,"keyword_usage":{"used_keywords":[],"accuracy":"ACCURATE|PARTIALLY_ACCURATE|INACCURATE","keyword_comment":""},"tone_label":"","tone_comment":"1-2문장","comment":"3-4문장"}
```

### 5.2 MINIMAL_PERSONA 예시 (BACKEND × JAVA_SPRING)

```
백엔드(Java/Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다.
```

> 언어 분석에서 페르소나의 역할은 "어떤 관점으로 볼 것인가"의 방향 설정뿐. 실질적 평가 기준은 키워드 사전과 평가 루브릭이 담당하므로 1문장이면 충분.

### 5.3 MERGED_VERBAL_EXPERTISE (변경 없음)

키워드 사전은 축약하지 않음. 이 부분은 모델의 판단 정확도에 직접 영향을 주는 핵심 컨텍스트.

```
Java/Spring 백엔드 답변 분석 기준:
- JVM, Spring, JPA 기술 용어의 정확한 사용
- 성능 수치(TPS, 응답시간, GC pause time) 구체적 언급
- "원인→해결→결과" 구조 설명 능력

키워드 사전:
JVM, GC, 힙 메모리, 메타스페이스, 스레드 풀, HikariCP, 커넥션 풀,
트랜잭션 격리 수준, @Transactional, 전파 속성, 롤백,
영속성 컨텍스트, 1차 캐시, 지연 로딩, 즉시 로딩, N+1, fetch join, EntityGraph,
Spring IoC, 빈 스코프, AOP, 프록시, CGLIB,
@Version, 낙관적 락, 비관적 락, 데드락,
Spring Cloud, 서킷 브레이커, Resilience4j, Spring Kafka
```

### 5.4 User Prompt

```
직무: {positionKorean} ({techStackDisplayName}) | 레벨: {levelKorean}
질문: {question_text}
{conditionalModelAnswer}
답변(STT): {transcript}
```

> "## 직무 정보" 같은 마크다운 헤더 제거. "면접자 답변 (STT 전사)" → "답변(STT)".
> 모범답변이 없으면 해당 줄 자체를 제거.

### 5.5 토큰 비교

| 구간 | v2 | v3 | 절약 |
|------|-----|-----|------|
| 페르소나 | ~200 | ~40 | ~160 |
| 키워드 사전 | ~180 | ~180 | 0 (축약 금지) |
| 루브릭 | ~86 | ~20 | ~66 |
| 평가 요소 | ~80 | ~40 | ~40 |
| JSON 스키마 | ~70 | ~35 | ~35 |
| **System 합계** | **~720** | **~419** | **~301 (42%)** |

---

## 6. 최적화된 비언어 분석 프롬프트

> **모델:** gpt-4o (Vision) | **Temperature:** 0.3 | **Max Tokens:** 500

### 6.1 System Prompt 템플릿

```
면접 영상 프레임의 비언어적 커뮤니케이션만 평가합니다. 답변 내용은 평가하지 않습니다.

## 평가
1. eye_contact_score(0-100): 90+=안정응시, 70+=간헐흐트러짐, 50+=자주딴곳, 30+=불안정, 0+=미응시
2. posture_score(0-100): 90+=바른자세유지, 70+=간헐구부정, 50+=불안정/흔듦, 30+=지속구부정, 0+=부적절
3. expression_label: CONFIDENT(자신감) | ENGAGED(몰입) | NEUTRAL(무표정) | NERVOUS(긴장) | UNCERTAIN(혼란)

## 주의
- 여러 프레임의 평균 경향 평가. 이상치에 과도한 가중치 금지.
- 사람 미확인 시 점수 50, comment에 설명.
- 한국 면접 문화 고려(차분함 긍정적).

## 응답
JSON만 응답:
{"eye_contact_score":0,"posture_score":0,"expression_label":"","comment":"한국어 2-3문장"}
```

### 6.2 User Prompt (변경 없음)

```
면접 영상에서 3초 간격 추출한 프레임입니다. 비언어적 커뮤니케이션을 분석하세요.
```

### 6.3 토큰 비교

| 구간 | v2 | v3 | 절약 |
|------|-----|-----|------|
| 역할 설명 | ~60 | ~30 | ~30 |
| 루브릭 (3항목) | ~220 | ~100 | ~120 |
| 주의사항 | ~80 | ~50 | ~30 |
| JSON 스키마 | ~50 | ~25 | ~25 |
| **System 합계** | **~468** | **~263** | **~205 (44%)** |

---

## 7. 구현: 조건부 블록 조립 로직

### 7.1 Java — QuestionGenerationPromptBuilder

```java
@Component
@RequiredArgsConstructor
public class QuestionGenerationPromptBuilder {

    private final PersonaResolver personaResolver;

    public PromptPair build(Interview interview) {
        TechStack stack = interview.getEffectiveTechStack();
        ResolvedProfile profile = personaResolver.resolve(interview.getPosition(), stack);
        Set<InterviewType> types = interview.getInterviewTypes();

        // 1. 페르소나: FULL
        String persona = profile.getFullPersona();

        // 2. 유형 가이드: 선택된 것만 필터링
        String typeGuide = profile.getInterviewTypeGuideMap().entrySet().stream()
            .filter(e -> types.contains(e.getKey()))
            .map(e -> "- " + e.getKey().getDisplayName() + ": " + e.getValue())
            .collect(Collectors.joining("\n"));

        // 3. CS 블록: CS_FUNDAMENTAL 포함 시에만
        String csBlock = "";
        if (types.contains(InterviewType.CS_FUNDAMENTAL)
                && !interview.getCsSubTopics().isEmpty()) {
            csBlock = "## CS 세부 주제\n" +
                interview.getCsSubTopics().stream()
                    .map(t -> t.getDisplayName())
                    .collect(Collectors.joining(", ")) +
                "에서만 출제.";
        }

        // 4. 레벨 가이드: 해당 레벨만
        String levelGuide = LevelGuideProvider.get(interview.getLevel());

        // 5. 이력서 블록: 있을 때만
        String resumeBlock = interview.hasResume()
            ? "## 이력서 활용\nRESUME_BASED 질문은 이력서의 프로젝트, 기술, 성과를 구체적으로 언급하여 생성."
            : "";

        // 6. 조립
        String systemPrompt = QUESTION_GEN_TEMPLATE
            .replace("{FULL_PERSONA}", persona)
            .replace("{BASE_EVALUATION_PERSPECTIVE}", profile.getEvaluationPerspective())
            .replace("{FILTERED_INTERVIEW_TYPE_GUIDE}", typeGuide)
            .replace("{CONDITIONAL_CS_SUBTOPIC_BLOCK}", csBlock)
            .replace("{SINGLE_LEVEL_GUIDE}", levelGuide)
            .replace("{CONDITIONAL_RESUME_BLOCK}", resumeBlock);

        return new PromptPair(systemPrompt, buildUserPrompt(interview, stack));
    }
}
```

### 7.2 Java — LevelGuideProvider

```java
public class LevelGuideProvider {

    private static final Map<Level, String> GUIDES = Map.of(
        Level.JUNIOR,
            "JUNIOR: 기본 개념의 정확한 이해. 원리 중심 질문, CS 기초와 논리적 사고력 평가. 실무 경험보다 학습 의지.",
        Level.MID,
            "MID: 실무 적용 경험과 문제 해결. 기술 선택 이유, 트레이드오프, 장애 시 판단력 평가.",
        Level.SENIOR,
            "SENIOR: 아키텍처 의사결정, 팀 리딩, 기술 방향성. 시스템 조망, 조직 기술 영향력 평가."
    );

    public static String get(Level level) {
        return GUIDES.get(level);
    }
}
```

### 7.3 Java — PersonaResolver (페르소나 축약 추가)

```java
@Component
public class PersonaResolver {

    public enum PersonaDepth { FULL, MEDIUM, MINIMAL }

    public ResolvedProfile resolve(Position position, TechStack techStack) {
        // ... 기존 merge 로직
    }

    /**
     * 프롬프트 역할에 따라 페르소나 깊이를 축약
     */
    public String getPersona(Position position, TechStack techStack, PersonaDepth depth) {
        ResolvedProfile profile = resolve(position, techStack);
        return switch (depth) {
            case FULL -> profile.getFullPersona();       // 질문 생성용
            case MEDIUM -> profile.getMediumPersona();   // 후속 질문용
            case MINIMAL -> profile.getMinimalPersona(); // 언어 분석용
        };
    }
}
```

### 7.4 ResolvedProfile — 축약 메서드

```java
public record ResolvedProfile(
    String fullPersona,         // Base + Overlay 전체
    String evaluationPerspective,
    Map<InterviewType, String> interviewTypeGuideMap,
    String followUpDepth,
    String verbalExpertise
) {
    /**
     * MEDIUM: 1-2문장 축약 페르소나
     * fullPersona의 첫 문장 + 핵심 기술 키워드
     */
    public String getMediumPersona() {
        // 예: "백엔드(Java/Spring) 시니어 면접관. API 설계, 동시성, 장애 대응, JVM/JPA 심화를 중시합니다."
        return mediumPersona;
    }

    /**
     * MINIMAL: 1문장 역할 정의
     */
    public String getMinimalPersona() {
        // 예: "백엔드(Java/Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다."
        return minimalPersona;
    }
}
```

### 7.5 Python — VerbalAnalyzerPromptFactory (언어 분석)

```python
class VerbalAnalyzerPromptFactory:

    SYSTEM_TEMPLATE = """{minimal_persona}

## 전문 분야
{verbal_expertise}

## 평가
1. verbal_score(0-100): 90+=핵심정확+기술깊이, 70+=대체로양호, 50+=핵심빗나감, 30+=이해부족/오류, 0+=무관
   평가요소: 핵심답변포함, 구조화(STAR), 기술키워드정확성, 논리흐름, 구체적수치/사례
2. filler_word_count: "음","어","그","아","뭐","이제","약간","좀","그러니까" 등장 횟수
3. keyword_usage: 핵심 키워드 추출 + 정확성 평가. 오용 시 구체 지적.
4. tone_label: PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE

## 응답
JSON만 응답:
{{"verbal_score":0,"filler_word_count":0,"keyword_usage":{{"used_keywords":[],"accuracy":"","keyword_comment":""}},"tone_label":"","tone_comment":"","comment":""}}"""

    USER_TEMPLATE = """직무: {position} ({tech_stack}) | 레벨: {level}
질문: {question}
{model_answer_line}답변(STT): {transcript}"""

    def build_system_prompt(self, position: str, tech_stack: str) -> str:
        overlay = self._get_overlay(position, tech_stack)
        return self.SYSTEM_TEMPLATE.format(
            minimal_persona=overlay["minimal_persona"],
            verbal_expertise=overlay["verbal_expertise"],
        )

    def build_user_prompt(self, position: str, tech_stack: str, level: str,
                          question: str, transcript: str,
                          model_answer: str | None = None) -> str:
        model_answer_line = f"모범답변: {model_answer}\n" if model_answer else ""
        return self.USER_TEMPLATE.format(
            position=position, tech_stack=tech_stack, level=level,
            question=question, model_answer_line=model_answer_line,
            transcript=transcript,
        )
```

### 7.6 Overlay YAML 구조 변경 (유형별 Map)

```yaml
# overlay/backend/java-spring.yaml

persona:
  full: |
    특히 Java/Kotlin 언어와 Spring Boot 에코시스템에 깊은 전문성을 가지고 있습니다.
    JVM 내부 동작, Spring IoC/AOP/트랜잭션 관리, JPA/Hibernate ORM에 대한
    실무 경험이 풍부하며, Spring Security, Spring Cloud 기반 MSA 설계/운영 경험이 있습니다.
  medium: "백엔드(Java/Spring) 시니어 면접관. API 설계, 동시성, 장애 대응, JVM/JPA 심화를 중시합니다."
  minimal: "백엔드(Java/Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다."

interview_type_guide:
  CS_FUNDAMENTAL: "OS, 네트워크, 자료구조. Java 관점 실무 연결. (예: TCP handshake → HikariCP 커넥션 풀)"
  BEHAVIORAL: "장애 대응, 코드 리뷰, 기술 부채 등 Java/Spring 팀 맥락 경험 질문"
  RESUME_BASED: "이력서의 Spring 프로젝트, JPA, 성능 개선 수치 기반 질문"
  JAVA_SPRING: "JVM 메모리/GC, Spring IoC/AOP, @Transactional 전파, JPA N+1, 동시성 제어"
  SYSTEM_DESIGN: "Java/Spring 기반 대규모 설계. Spring Cloud Gateway, Resilience4j, Kafka 맥락"

followup_depth: |
  Java/Spring 심화:
  - JVM → GC 로그, 힙 덤프, 메모리 릭
  - 트랜잭션 → 전파 속성 실수, Saga 패턴
  - JPA → N+1 전략 비교, 벌크 연산, 2차 캐시
  - 동시성 → synchronized vs Lock, @Version

verbal_expertise: |
  Java/Spring 답변 분석 기준:
  - JVM, Spring, JPA 기술 용어의 정확한 사용
  - 성능 수치(TPS, 응답시간, GC pause time) 구체적 언급
  - "원인→해결→결과" 구조 설명 능력

  키워드 사전:
  JVM, GC, 힙 메모리, 메타스페이스, 스레드 풀, HikariCP, 커넥션 풀,
  트랜잭션 격리 수준, @Transactional, 전파 속성, 롤백,
  영속성 컨텍스트, 1차 캐시, 지연 로딩, 즉시 로딩, N+1, fetch join, EntityGraph,
  Spring IoC, 빈 스코프, AOP, 프록시, CGLIB,
  @Version, 낙관적 락, 비관적 락, 데드락,
  Spring Cloud, 서킷 브레이커, Resilience4j, Spring Kafka
```

---

## 8. 전후 비교 요약

### 8.1 System Prompt 토큰 비교 (BACKEND × JAVA_SPRING, JUNIOR, [JAVA_SPRING, SYSTEM_DESIGN], 이력서 없음)

| 프롬프트 | v2 | v3 | 절약 | 절약률 |
|---------|-----|-----|------|-------|
| 질문 생성 | ~1,161 | ~732 | ~429 | 37% |
| 후속 질문 | ~630 | ~435 | ~195 | 31% |
| 언어 분석 | ~720 | ~419 | ~301 | 42% |
| 비언어 분석 | ~468 | ~263 | ~205 | 44% |
| **합계** | **~2,979** | **~1,849** | **~1,130** | **38%** |

### 8.2 면접 1회(QuestionSet 5개) 기준 비용 영향

| 호출 | 횟수 | v2 input tok | v3 input tok |
|------|------|-------------|-------------|
| 질문 생성 | 1회 | ~1,161 | ~732 |
| 후속 질문 | 5~15회 | ~9,450 | ~6,525 |
| 언어 분석 | 5회 | ~3,600 | ~2,095 |
| 비언어 분석 | 5회 | ~2,340 | ~1,315 |
| **합계** | | **~16,551** | **~10,667** |
| **절약** | | | **~5,884 tok (36%)** |

> 참고: 후속 질문의 경우 User Prompt에 이전 대화가 누적되므로 실제 토큰은 더 많지만, System Prompt 절약분은 매 호출에 적용됨.

### 8.3 최적화 원칙 요약

| 원칙 | 설명 |
|------|------|
| **조건부 주입** | 해당 조건에서만 블록 포함 (CS, 이력서, 레벨) |
| **역할별 축약** | 프롬프트 역할에 따라 페르소나 깊이 차등 (FULL/MEDIUM/MINIMAL) |
| **선택적 필터링** | 면접 유형 가이드는 선택된 것만 추출 |
| **스키마 압축** | JSON 들여쓰기 제거, 한 줄 표현 |
| **루브릭 압축** | 5단계 상세 → 1줄 키워드 요약 |
| **핵심 보존** | 키워드 사전, 심화 방향은 축약 금지 (품질 직결) |

### 8.4 축약하면 안 되는 것

| 블록 | 이유 |
|------|------|
| 기술 키워드 사전 | 모델의 키워드 인식/평가 정확도에 직접 영향 |
| 심화 방향 (followUpDepth) | 후속 질문의 다양성과 깊이를 결정 |
| 면접 유형 가이드 (선택된 것) | 질문의 전문성을 결정하는 핵심 지시 |
| JSON 필드 이름 | 파싱 호환성을 위해 정확히 유지 필요 |
