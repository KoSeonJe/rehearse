# Plan 02: questionpool → interview 병합 + tts → infra 이동

> 상태: Draft
> 작성일: 2026-04-13

## Why

**questionpool**: 10개 클래스가 독립 도메인이지만, 실제 소비자는 interview의 질문 생성 관련 클래스뿐이다 (import 5곳 모두 interview 관련). 독립 도메인이 아닌 interview.generation의 하위 모듈로 병합하여 응집도를 높인다.

**tts**: `TtsService`는 interface 1줄, `TtsController`는 `@ConditionalOnProperty`로 조건부 등록, 구현체 `GoogleCloudTtsService`는 이미 `infra/google/`에 위치. 비즈니스 도메인이 아닌 인프라 통합이므로 infra 레이어로 이동한다.

## 전제조건: 테스트 확인

모든 리팩토링 대상에 테스트가 존재한다:

| 대상 | 테스트 | 상태 |
|------|--------|------|
| QuestionPoolService | QuestionPoolServiceTest | ✅ |
| CacheableQuestionProvider | CacheableQuestionProviderTest | ✅ |
| FreshQuestionProvider | FreshQuestionProviderTest | ✅ |
| KeywordMatcher | KeywordMatcherTest | ✅ |
| QuestionGenerationLock | QuestionGenerationLockTest | ✅ |
| QuestionPool (entity) | QuestionPoolTest | ✅ |
| TtsController | TtsControllerTest | ✅ |

추가 테스트 작성 불필요. 즉시 리팩토링 가능.

## 생성/수정 파일

### questionpool → interview/generation/pool 이동

| 파일 | 작업 |
|------|------|
| `domain/interview/generation/pool/entity/QuestionPool.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/entity/CsSubTopic.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/repository/QuestionPoolRepository.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/service/QuestionPoolService.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/service/CacheableQuestionProvider.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/service/FreshQuestionProvider.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/service/KeywordMatcher.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/service/QuestionGenerationLock.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/converter/StringListJsonConverter.java` | questionpool에서 이동 |
| `domain/interview/generation/pool/util/QuestionCacheKeyGenerator.java` | questionpool에서 이동 |

### interview generation 서비스도 함께 이동 (응집도 향상)

| 파일 | 작업 |
|------|------|
| `domain/interview/generation/service/QuestionGenerationService.java` | interview/service에서 이동 |
| `domain/interview/generation/service/QuestionGenerationEventHandler.java` | interview/service에서 이동 |
| `domain/interview/generation/service/QuestionGenerationTransactionHandler.java` | interview/service에서 이동 |

### tts → infra/tts 이동

| 파일 | 작업 |
|------|------|
| `infra/tts/TtsService.java` | domain/tts에서 이동 |
| `infra/tts/TtsController.java` | domain/tts에서 이동 |
| `infra/tts/TtsRequest.java` | domain/tts에서 이동 |
| `infra/tts/TtsErrorCode.java` | domain/tts에서 이동 |

### 삭제

| 파일 | 작업 |
|------|------|
| `domain/questionpool/` | 빈 디렉토리 삭제 |
| `domain/tts/` | 빈 디렉토리 삭제 |

### import 업데이트 대상

| 파일 | 작업 |
|------|------|
| `domain/question/entity/Question.java` | QuestionPool import 변경 |
| `infra/google/GoogleCloudTtsService.java` | TtsService import 변경 |
| 테스트 파일 전체 | import 업데이트 |

## 상세

### 주의사항

- 패키지 이동만 수행, **로직 변경 없음**
- `@ComponentScan` 범위는 루트 패키지 기준이므로 영향 없음
- `QuestionPoolRepository`의 JPA 스캔도 루트 패키지 하위이므로 영향 없음
- `TtsController`의 `@ConditionalOnProperty` 설정은 그대로 유지

## 담당 에이전트

- Implement: `backend` — 패키지 이동, import 업데이트
- Review: `code-reviewer` — import 누락 검증
- Review: `architect-reviewer` — 도메인 경계, 의존성 방향

## 검증

- `./gradlew compileJava` — 컴파일 에러 없음
- `./gradlew test` — 전체 테스트 통과
- `domain/questionpool/` 디렉토리 삭제 확인
- `domain/tts/` 디렉토리 삭제 확인
- top-level 도메인 수 확인: 10개 → 11개 (question, analysis, feedback 추가, questionpool/tts 제거)
- `progress.md` 상태 업데이트 (Task 2 → Completed)
