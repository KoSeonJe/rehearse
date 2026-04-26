# Plan 05: Resume Extractor (Phase 1) `[parallel:06]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W3
> 원본: `docs/todo/2026-04-20/06-resume-track.md` (Phase 0 + Phase 1)

## Why

현재 이력서 PDF 텍스트를 그대로 LLM에 공급하는 구조는 (a) 2단 레이아웃/헤더/아이콘 노이즈, (b) 한글 토큰 깨짐("띵" "동"), (c) 매 턴 동일 이력서를 다르게 해석하는 일관성 문제를 유발. 이력서를 **세션 시작 시 1회** JSON Skeleton(claims / implicit CS topics / interrogation chains / priority map)으로 구조화하면 downstream(plan-06/07) 전부가 동일한 불변 IR을 공유 → 질문 생성 품질이 근본부터 올라감.

**저장 정책은 세션 스코프 캐시로 단순화**(GDPR 상세는 Out of Scope). 2시간 TTL in-memory/Redis.

**Dynamic Pacing 원칙 (2026-04-22, plan-06 연동)**: Extractor 는 이력서의 **모든 chain 을 최대로 추출한다. duration 무관**. 15분 세션이든 60분 세션이든 동일한 Skeleton 을 생성 (이력서당 1회 추출, 비용 고정). 어디까지 소화할지는 plan-07 Orchestrator 가 `ClockWatcher` 로 런타임 판단.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/resume/resume-extractor.txt` | 신규. Phase 1 추출 프롬프트 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeExtractorPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/PdfTextExtractor.java` | **수정 (기존 클래스 — 신규 아님)**. 정규화 파이프라인(RemoveControlChars/CollapseWhitespace/FixKoreanTokenBreaks/RemoveHeaderFooter/ExtractByColumn) 추가 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ResumeIngestionService.java` | 신규. `PdfTextExtractor` 호출 + 언어 감지 + 섹션 분리 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ResumeExtractionService.java` | 신규. 텍스트 → Skeleton |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeSkeleton.java` | 신규 record — `implements CachedResumeSkeleton` (plan-00c 인터페이스, fileHash() 구현) |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/Project.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeClaim.java` | 신규 (plan-02의 `Claim`과 분리) |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/InterrogationChain.java` | 신규 (L1 WHAT → L2 HOW → L3 WHY_MECH → L4 TRADEOFF) |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java` | **수정**. `SKELETON_BY_CALL_TYPE` Map 에 `"resume_extractor"` 엔트리 추가 |

## 상세

### Phase 0 (Ingestion) 정규화 파이프라인

PDF 라이브러리: 현재 `org.apache.pdfbox:pdfbox:3.0.4` 사용 중 (build.gradle.kts L73). 추가 의존성 불필요. 2단 컬럼 추출은 `PDFTextStripper.setSortByPosition(true)` API 로 지원 가능 — 별도 라이브러리 추가 없이 ExtractByColumn 파이프라인 구현 가능.

- `RemoveControlChars` → `CollapseWhitespace` → `FixKoreanTokenBreaks` → `RemoveHeaderFooter` → `ExtractByColumn` (2단, `setSortByPosition(true)` 활용)
- 현재 `PdfTextExtractor.extract()` 는 단순 `PDFTextStripper.getText()` + 5000자 트림만 수행. 정규화 파이프라인 추가 시 `MAX_TEXT_LENGTH` 트림 위치도 파이프라인 말단으로 이동 필요.

### Phase 1 (Extraction) 출력 스키마
```json
{
  "resume_id": "r_<생성>",
  "candidate_level": "junior|mid|senior",
  "target_domain": "backend|frontend|...",
  "projects": [{
    "project_id": "p1",
    "claims": [{"claim_id": "p1_c1", "text": "...", "claim_type": "PROBLEM_SOLVING|ARCHITECTURE_CHOICE|IMPLEMENTATION|IMPACT_METRIC", "priority": "high|medium|low", "depth_hooks": [...]}],
    "implicit_cs_topics": [{"topic": "rate-limiting", "confidence": 0.8, "interrogation_chain": [{"level": 1-4, "type": "WHAT|HOW|WHY_MECH|TRADEOFF", "question": "..."}]}]
  }],
  "interrogation_priority_map": {"high": [...], "medium": [...], "low": [...]}
}
```

### 추출 원칙 (프롬프트에 명시)
1. **명시적 claim만 추출**: 창작/추측/보강 금지
2. **Implicit CS topic**은 confidence ≥ 0.3만 포함
3. **4단 심문 체인** 반드시 생성: WHAT → HOW → WHY_MECH → TRADEOFF
4. priority: high(문제 해결 스토리) / medium(아키텍처 선택) / low(흔한 구현)

### 모델 선택 (critic C3 해결 — plan-00b 전제)
- **기본값은 `application-prod.yml` 의 `gpt-4o-mini` + Claude Sonnet fallback 을 유지** (현 스택 드리프트 방지)
- `ChatRequest.modelOverride` 는 기본 `null` → application.yml 값 사용. 추출 품질 부족 확인 시 `application-prod.yml` 에서 직접 `gpt-4o` 로 변경 후 배포.
- Feature Flag runtime toggle은 사용하지 않는다. 모델 변경은 설정 파일 수정 + ECR 재배포로 처리.
- temperature: 0.2, max_tokens: 4096, callType: `"resume_extractor"`
- 비용 시나리오: gpt-4o-mini 기본 ≈ $0.005/이력서. 품질 이슈로 gpt-4o 전환 시 ≈ $0.05/이력서 → 월 1만 세션 기준 $50 vs $500 비교 후 의사결정

### 영속화 (critic C2 해결 — plan-00c 전제)
- Skeleton 은 plan-00c 의 V24 `resume_skeleton` 테이블에 저장(L2) + `InterviewRuntimeStateStore` 에 2h 캐시(L4)
- 조회 우선순위: 캐시(L4) → DB(L2) → 추출(신규)

### 저장 정책

**결정 (2026-04-27, B3): `ResumeSkeletonCache` 별도 신설 안 함 — `InterviewRuntimeStateStore` 재사용.**

`InterviewRuntimeState` 에는 이미 `CachedResumeSkeleton resumeSkeletonCache` 필드가 존재 (plan-00c 구현, `entity/CachedResumeSkeleton` 인터페이스). `ResumeSkeleton` record 가 이 인터페이스를 구현(`implements CachedResumeSkeleton`)하면 추가 캐시 추상화 없이 기존 Caffeine 2h TTL 캐시를 그대로 사용할 수 있다.

이유:
- TTL/Eviction/Metrics 정책 일원화 — Caffeine 캐시 설정 1곳에서만 관리
- `InterviewLockService` 동시성 락 동일 경로 재사용
- 세션 종료 시 `InterviewRuntimeStateStore.evict(interviewId)` 하나로 일괄 evict
- 캐시 추상화 중복 회피 (`ResumeSkeletonCache` 신설 시 Caffeine 캐시 2개 병존)

구현 시 예상 흐름:
```java
// ResumeExtractionService (또는 상위 진입점 서비스) 내부
ResumeSkeleton sk = Optional.ofNullable(
        (ResumeSkeleton) runtimeStateStore.get(interviewId).getResumeSkeleton())
    .or(() -> resumeSkeletonRepository.findByInterviewId(interviewId)
            .map(entity -> ResumeSkeleton.fromEntity(entity)))
    .orElseGet(() -> {
        ResumeSkeleton extracted = extractionService.extract(ingestedText);
        runtimeStateStore.update(interviewId,
            state -> state.setResumeSkeleton(extracted));  // setter 또는 withResumeSkeleton
        resumeSkeletonRepository.save(ResumeSkeleton.toEntity(extracted, interviewId));
        return extracted;
    });
```

주의: `InterviewRuntimeState.resumeSkeletonCache` 필드가 현재 생성자에서 외부 주입(`CachedResumeSkeleton resumeSkeletonCache` 파라미터)으로만 세팅됨. 본 구현 PR 에서 `setResumeSkeleton(ResumeSkeleton)` mutator 를 `InterviewRuntimeState` 에 추가하거나, `runtimeStateStore.update()` 의 `Consumer<InterviewRuntimeState>` 람다로 내부 필드를 직접 변경하는 방식으로 해결.

## 담당 에이전트

- Implement: `backend` — Ingestion + Extraction 서비스 + 캐시
- Implement: `prompt-engineer` — 추출 프롬프트 작성, few-shot 예시
- Review: `code-reviewer` — PDF 파싱 방어 코드, JSON 스키마 검증, 세션 캐시 동시성

## 검증

1. 본인 이력서 포함 10개 PDF에서 추출 성공률 100%
2. Expected claim 커버리지 ≥ 90% (수동 라벨 대비)
3. Implicit CS topic precision ≥ 80% (confidence ≥ 0.6 항목 기준)
4. 각 `InterrogationChain`이 반드시 4단계 보유
5. **추출 결과가 plan-04 `FixedContextLayer`에 주입 가능 (2026-04-27 갱신 — plan-04 머지 ee67201 반영)**
   - 주입 경로: `ContextBuildRequest(callType="resume_extractor", runtimeState, exchanges, focusHints, providerHint)` → `InterviewContextBuilder.build(req)` → `FixedContextLayer.build(req)` 에서 `SKELETON_BY_CALL_TYPE.getOrDefault("resume_extractor", DEFAULT_SKELETON)` 로 L1 시스템 블록 렌더링.
   - 통합 테스트 시나리오: `InterviewContextBuilder` 빈 직접 호출, `callType="resume_extractor"` 로 `ContextBuildRequest` 생성, `BuiltContext.messages()` 의 첫 번째 `ChatMessage` (SYSTEM role, `cacheControl=true`) 내용에 resume_extractor 역할 skeleton 포함 여부 assert.
   - `FixedContextLayer.SKELETON_BY_CALL_TYPE` 에 `"resume_extractor"` 엔트리 추가(본 plan PR 범위) 후 `DEFAULT_SKELETON` fallback 으로 떨어지지 않는지 검증.
   - `BuiltContext.perLayerTokens().get("L1")` > 0 이고 total ≤ 8000 (plan-04 token budget) assert.
6. 세션 종료 후 `InterviewRuntimeStateStore.evict(interviewId)` 호출 시 캐시 evict 확인 (`InterviewRuntimeState.resumeSkeletonCache` null 또는 store.get() `IllegalStateException`)
7. `progress.md` 05 → Completed
