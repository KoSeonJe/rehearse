# Task 15 — AiCallMetrics 갱신 + dashboard 메모

> **위치**: `tasks/p2-be-15-ai-metrics.md`
> **답하는 질문**: 신규 callType 메트릭 + 폐기 callType panel 어떻게 정리?

---

## 목적

`AiCallMetrics` 에 신규 callType `resume_question_generator` 카운터 추가. 폐기 callType `compaction_summarizer` 카운터 제거 (Task 10 동행). 운영 대시보드 panel 제거 메모 = `docs/observability/dashboards.md` (파일 부재 시 신규 생성).

## 에이전트

- **구현**: `backend` — AiCallMetrics 카운터 + dashboard 운영자 노트 문서화
- **리뷰**: `code-reviewer-backend` — 메트릭 명명 / 라벨 / 대시보드 정합

## 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java` — `resume_question_generator` 카운터 추가 + `compaction_summarizer` 카운터 제거
- `backend/src/main/resources/application*.yml` — 신규 callType 모델 매핑 (Task 05 정합)
- `docs/observability/dashboards.md` — 신규 또는 갱신 (P2 머지 시 운영자 노트)

## 핵심 로직

```java
// AiCallMetrics (After)
@Component
public class AiCallMetrics {
    private final MeterRegistry registry;

    public void recordCall(String callType, Duration latency, boolean success) {
        registry.counter("ai.call.count", "type", callType, "status", success ? "ok" : "fail").increment();
        registry.timer("ai.call.latency", "type", callType).record(latency);
    }
}

// 등록되는 callType (After)
// - resume_question_generator  ← NEW
// - follow_up_writer
// - answer_analyzer
// - audio_turn_analyzer
// (제거: compaction_summarizer)
```

```markdown
# docs/observability/dashboards.md (운영자 노트 신규)

## 2026-05-NN — P2 머지 시점 변경

- 추가 panel: `ai.call.count{type="resume_question_generator"}` (이력서 면접 시작 시 1회 호출 / GPT-4o-mini primary + Claude fallback)
- 제거 panel: `ai.call.count{type="compaction_summarizer"}` (DialogueCompactor 폐기 동행)
```

## 의존
- 선행 Task: 05 (resume_question_generator callType 등장), 10 (compaction_summarizer 폐기)
- 외부: Micrometer + 운영 대시보드 (Grafana 가정)

## 테스트 케이스
- [ ] `AiCallMetricsTest` — `resume_question_generator` callType 카운터 증가 정상
- [ ] 폐기 callType `compaction_summarizer` 등록 부재 (registry grep)
- [ ] `application-prod.yml` 모델 매핑 = primary GPT-4o-mini + fallback Claude (Sonnet/Haiku) 확인

## 완료 기준
- [ ] 메트릭 카운터 추가 + 폐기
- [ ] dashboard 운영자 노트 추가
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
chore(BE): AiCallMetrics resume_question_generator 추가 + compaction_summarizer 제거
```

## 비고

- 대시보드 panel 자체 조작 = 운영자 수동 (코드 변경 비대상). 본 task = 코드 측 카운터 + 운영자 노트만
- Grafana 대시보드 URL = memory `reference_grafana` 참조 (부재 시 운영자 문의)
