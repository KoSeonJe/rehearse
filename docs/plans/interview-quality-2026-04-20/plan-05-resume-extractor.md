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
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeSkeleton.java` | 신규 record |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/Project.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeClaim.java` | 신규 (plan-02의 `Claim`과 분리) |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/InterrogationChain.java` | 신규 (L1 WHAT → L2 HOW → L3 WHY_MECH → L4 TRADEOFF) |
| `backend/src/main/java/com/rehearse/api/domain/resume/ResumeSkeletonCache.java` | 신규. 세션 스코프 캐시 (2h TTL) |

## 상세

### Phase 0 (Ingestion) 정규화 파이프라인
- `RemoveControlChars` → `CollapseWhitespace` → `FixKoreanTokenBreaks` → `RemoveHeaderFooter` → `ExtractByColumn` (2단)
- PDF 라이브러리: PDFBox 또는 Tika (기존 의존성 재사용 확인)

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
- **기본값은 application.yml 의 `gpt-4o-mini` + Claude Sonnet fallback 을 유지** (현 스택 드리프트 방지)
- `ChatRequest.modelOverride` 는 기본 `null` → application.yml 값 사용. 추출 품질 부족 확인 시 `rehearse.features.resume-track.extraction-model` flag 경유로 `gpt-4o` 승격 (plan-00b @RefreshScope)
- temperature: 0.2, max_tokens: 4096, callType: `"resume_extractor"`
- 비용 시나리오: gpt-4o-mini 기본 ≈ $0.005/이력서. 품질 이슈로 gpt-4o 전환 시 ≈ $0.05/이력서 → 월 1만 세션 기준 $50 vs $500 비교 후 의사결정
- Exit Criteria (plan-12): flag OFF (gpt-4o-mini) 상태에서도 J1 Relevance ≥ 4.0 달성 시 gpt-4o 옵션 제거

### 영속화 (critic C2 해결 — plan-00c 전제)
- Skeleton 은 plan-00c 의 V24 `resume_skeleton` 테이블에 저장(L2) + `InterviewRuntimeStateStore` 에 2h 캐시(L4)
- 조회 우선순위: 캐시(L4) → DB(L2) → 추출(신규)

### 저장 정책
```java
ResumeSkeleton sk = resumeSkeletonCache
    .get(sessionId, fileHash)
    .orElseGet(() -> {
        ResumeSkeleton extracted = extractionService.extract(ingestedText);
        resumeSkeletonCache.putWithTTL(sessionId, fileHash, extracted, Duration.ofHours(2));
        return extracted;
    });
```

## 담당 에이전트

- Implement: `backend` — Ingestion + Extraction 서비스 + 캐시
- Implement: `prompt-engineer` — 추출 프롬프트 작성, few-shot 예시
- Review: `code-reviewer` — PDF 파싱 방어 코드, JSON 스키마 검증, 세션 캐시 동시성

## 검증

1. 본인 이력서 포함 10개 PDF에서 추출 성공률 100%
2. Expected claim 커버리지 ≥ 90% (수동 라벨 대비)
3. Implicit CS topic precision ≥ 80% (confidence ≥ 0.6 항목 기준)
4. 각 `InterrogationChain`이 반드시 4단계 보유
5. 추출 결과가 plan-04 `FixedContextLayer`에 주입 가능(L1 XML 렌더링 통합 테스트)
6. 세션 종료 후 2시간 경과 시 캐시 evict 확인
7. `progress.md` 05 → Completed
