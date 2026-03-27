# GPT-4o-mini-audio 후속질문 통합 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-27

## Why

### 1. Why? — 어떤 문제를 해결하는가?

후속질문 생성 API(`POST /api/v1/interviews/{id}/follow-up`)가 외부 API를 **2번 직렬 호출**하고 있다:

1. `WhisperService.transcribe()` — OpenAI Whisper STT (~2.4초)
2. `aiClient.generateFollowUpQuestion()` — GPT-4o-mini LLM (~2.5초)

총 **~4.9초**가 외부 API 대기에 소요되며, 이는 면접 진행 중 실시간 응답 UX에 직접적 영향을 미친다.

또한 Whisper STT의 정확도 문제가 확인됨: 기술 용어("O(n log n)")를 "5n log n"으로 잘못 인식하는 사례 발생.

### 2. Goal — 구체적인 결과물과 성공 기준

| 지표 | 현재 | 목표 |
|------|------|------|
| 외부 API 호출 수 | 2회 (Whisper + GPT) | 1회 (GPT-4o-mini-audio) |
| 응답시간 (외부 API 구간) | ~4.9초 | ~3.5초 (28% 단축) |
| STT 정확도 | "5n log n" (Whisper 오류) | "O(n log n)" (GPT-audio 정확) |
| Fallback | 없음 | GPT-audio 실패 → Whisper STT + Claude 텍스트 생성 |
| Lambda 변경 | - | 없음 (Gemini 유지) |

### 3. Evidence — 근거 데이터

동일 오디오(한국어 면접 답변, 1.2MB WAV)로 실측:

**응답시간:**

| 모델 | 1회 | 2회 | 3회 | 평균 |
|------|-----|-----|-----|------|
| Whisper STT | 2.96s | 1.62s | 2.65s | **2.41초** |
| GPT-4o-mini (후속질문) | 2.66s | 2.33s | 2.14s | **2.46초** |
| **현재 합계 (직렬)** | | | | **~4.9초** |
| GPT-4o-mini-audio (통합) | 5.42s | 4.89s | 4.46s | **4.9초** (긴 오디오) |
| GPT-4o-mini-audio (짧은 오디오) | 3.86s | 3.04s | 3.05s | **3.5초** |

**STT 정확도:**

| 모델 | 입력 | 출력 | 정확도 |
|------|------|------|--------|
| Whisper | "O(n log n)" 발화 | "5n log n" | 오류 |
| GPT-4o-mini-audio | 동일 | "O(n log n)" | 정확 |
| Gemini 2.5 Flash | 동일 | "O(N log N)" | 정확 |

**Gemini vs GPT 비교 (Lambda 용도):**

| 기준 | GPT-4o-mini-audio | Gemini 2.5 Flash |
|------|-------------------|------------------|
| 속도 | 4.9초 | 9.6초 (2배 느림) |
| 피드백 품질 | 일반적 | 더 구체적 |
| JSON 안정성 | markdown fence 포함 | 순수 JSON |

→ Backend(실시간)에는 GPT, Lambda(비동기)에는 Gemini가 각각 최적.

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 선택 | 대안 | 대안 제외 이유 |
|------|------|----------------|
| GPT-4o-mini-audio (Backend) | Gemini 2.5 Flash | 2배 느림 (9.6초), 실시간 API에 부적합 |
| Gemini 유지 (Lambda) | GPT 통일 | Lambda는 비동기, Gemini 피드백 품질이 더 높음 |
| Whisper fallback 유지 | 완전 삭제 | GPT-audio 장애 시 서비스 안정성 확보 |
| Backend만 전환 | Backend + Lambda 동시 | Lambda는 이미 잘 동작 중, 변경 리스크 불필요 |

## 아키텍처

### 변경 전

```
[오디오 파일] → WhisperService.transcribe() → answerText (~2.4초)
                                                    ↓
[answerText] → OpenAiClient.generateFollowUpQuestion() → 후속질문 (~2.5초)
                                                    ↓
              FollowUpTransactionHandler.saveFollowUpResult()
// 총 외부 API 대기: ~4.9초, 호출 2회
```

### 변경 후

```
[오디오 파일] → OpenAiClient.generateFollowUpWithAudio() → answerText + 후속질문 (~3.5초)
                                                    ↓
              FollowUpTransactionHandler.saveFollowUpResult()
// 총 외부 API 대기: ~3.5초, 호출 1회

[Fallback] GPT-audio 실패 시:
  → WhisperService.transcribe() → answerText
  → ClaudeApiClient.generateFollowUpQuestion() → 후속질문
```

## Scope

### In
- `AiClient` 인터페이스에 audio 메서드 추가
- `OpenAiClient`에 GPT-4o-mini-audio-preview 호출 구현
- `ResilientAiClient`에 audio fallback 경로 추가
- `InterviewService.generateFollowUp()` 수정 (resolveAnswerText 제거)
- `GeneratedFollowUp` DTO에 answerText 필드 추가
- `MockAiClient` mock 구현
- 테스트 + 검증

### Out
- Lambda 분석 파이프라인 변경 (Gemini 유지)
- WhisperService 삭제 (fallback용 유지)
- 질문 생성 API 변경 (텍스트 기반 유지)
- Frontend 변경

## 설계 결정

### answerText는 API 응답에만 포함하며 DB에 저장하지 않는다

- GPT-audio가 추출한 transcript(`answerText`)는 `FollowUpResponse`에 포함되어 FE로 전달
- FE가 이를 면접 진행 중 표시용으로 사용
- DB 영속화가 불필요한 이유:
  1. Lambda 분석 파이프라인이 별도로 Gemini STT를 수행하여 transcript를 생성·저장함
  2. 후속질문의 핵심 산출물은 `Question` 엔티티(질문 텍스트, 모범답안)이며, 답변 원문은 영상에 보존됨
  3. 중복 저장을 피하고 단일 진실 원천(Lambda 분석 결과)을 유지

### ClaudeApiClient는 AiClient를 구현하지 않는 구조

- `ResilientAiClient`만 `AiClient` 인터페이스를 구현하고, `OpenAiClient`/`ClaudeApiClient`는 직접 참조
- 따라서 `ClaudeApiClient`에 `generateFollowUpWithAudio()` 추가 불필요
- fallback 경로는 `ResilientAiClient`가 Whisper + Claude를 직접 조합

## 제약조건

- Java 21, Spring Boot 3.4.x
- GPT-4o-mini-audio-preview 모델 사용 (OpenAI REST API)
- 오디오 입력: WebM (MediaRecorder) → base64 인코딩
- `@Transactional(propagation = NOT_SUPPORTED)` TX 분리 패턴 유지
- `ResilientAiClient` primary/fallback 패턴 유지
