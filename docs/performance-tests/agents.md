# Performance Tests — 에이전트 가이드

> LLM 호출을 수반하는 plan 의 품질·지연·비용을 측정·기록하는 표준 절차.
> 자동 단위 테스트(`./gradlew test`) 로 커버되지 않는 **비결정적 LLM 품질** 을 별도 LIVE 테스트 + 수동 수집 + 고정 템플릿 문서로 추적한다.

---

## 디렉토리 규칙

```
docs/performance-tests/
├── agents.md                           # 이 파일 — 가이드·템플릿
├── {plan-id}/                          # plan 별 폴더. 예: plan-01-intent-classifier
│   ├── {YYYY-MM-DD}-{scope}.md         # 실행 결과 리포트. 실행마다 파일 추가
│   └── {YYYY-MM-DD}-misclassified.json # (선택) 상세 오답 샘플 등 부록 데이터
```

- 폴더명은 `plan-{번호}-{토픽}` kebab-case
- 파일명에 날짜 prefix 필수 (시간순 추적)
- 동일 plan 의 재측정은 **기존 파일 수정 금지, 새 날짜 파일 추가**

---

## 언제 새 문서를 추가하는가

1. plan 구현 Phase A 게이트 검증 시 (최초 측정)
2. 프롬프트 / 모델 / 온도 / max_tokens 등 파라미터 변경 후
3. 롤백 직전 회귀 확인 (before/after 2회)
4. 스테이징 → 프로덕션 승격 직전
5. Alert 이 발화해 p95 / 정확도 회귀가 의심될 때

---

## 측정 대상 지표 (필수 포함)

| 지표 | 정의 | 수집 방법 |
|------|------|---------|
| 정확도 (classification) | (맞춘 개수) / (전체 골든셋) | LIVE 테스트 실행 로그 집계 |
| 분기별 정확도 | intent/category 별 sub-accuracy | 오답 로그 분류 |
| False Positive | 정상 intent 를 다른 intent 로 오판 | 오답 매트릭스 |
| p50 / p95 latency (end-to-end) | 클라 → 응답 받기까지 | Grafana `rehearse.ai.call.duration_seconds` |
| p50 / p95 latency (LLM 호출만) | chat() 시작 → 반환 | 동일 메트릭, `call.type` 태그 filter |
| 토큰 소비 (input / output / cached) | 전체 평균 + per 호출 | `rehearse.ai.call.tokens.*` 카운터 |
| 비용 (USD) | 토큰 × 모델 단가 | 토큰 × application.yml 모델 단가 매핑 |
| 실패율 | fallback 발동 % | `IntentResult.fallback=true` 비율 or 로그 집계 |

**선택 지표**: 제공자 간 일치율 (OpenAI vs Claude), 연속 fallback 스파이크, cache hit 률.

---

## 합격 기준 템플릿

plan 마다 Goal 에 합의된 수치를 **측정 전에** 확정해 리포트에 기재. 예:

- Intent Classifier: 전체 정확도 ≥ 90%, OFF_TOPIC 자체 ≥ 80%, CLARIFY FP ≤ 3%, 제공자 차이 ≤ 5%p
- Answer Analyzer: claim 추출 F1 ≥ 0.85, 분기 신호 오판 ≤ 5%
- Follow-up Generator v3: 수동 품질 판정 3/5 건 이상 후계 우세

합격 기준은 plan 스펙 `검증` 섹션에서 인용. 기준을 바꾸려면 plan 문서 PR 부터.

---

## 리포트 문서 템플릿

새 측정 결과는 아래 구조를 그대로 복제해 작성한다. 선택 섹션은 `(N/A)` 로 유지.

```markdown
# {plan-id} 성능 테스트 결과 — {YYYY-MM-DD}

> 실행자: {이름 / 에이전트}
> 환경: local / staging / prod
> 커밋: {sha}
> 브랜치: {branch}
> 소요 시간: {초}

## 목적

이 측정이 확인하는 것 한 문장. (Phase A 게이트 / 회귀 검증 / 모델 교체 검토 등)

## 대상 모델·파라미터

| 항목 | 값 |
|------|------|
| Provider | OpenAI / Claude |
| Model | gpt-4o-mini |
| Temperature | 0.1 |
| Max tokens | 200 |
| Response format | JSON_OBJECT |
| Prompt caching | auto |

## 골든셋 / 시나리오

| 분류 | 건수 | 출처 |
|------|------|------|
| ANSWER | 10 | `IntentClassifierGoldenSetLiveTest.goldenSet()` |
| ... | ... | ... |
| 합계 | 25 | — |

## 결과 — 정확도

| 분류 | 맞춤 | 전체 | 정확도 | 합격 기준 | 판정 |
|------|------|------|--------|--------|------|
| 전체 | X | 25 | X% | ≥ 90% | PASS / FAIL |
| ANSWER | | | | | |
| CLARIFY_REQUEST | | | | | |
| GIVE_UP | | | | | |
| OFF_TOPIC | | | | ≥ 80% | |

## 결과 — 지연·비용

| 지표 | 값 | 비고 |
|------|------|------|
| p50 latency | | |
| p95 latency | | |
| 평균 input 토큰 | | |
| 평균 output 토큰 | | |
| cached read (시스템 프롬프트) | | |
| 1회 호출당 예상 비용 | | |
| Fallback 발동 | X/25 | — |

## 오답 상세 (있으면)

| # | mainQuestion | answerText (요약) | expected | actual | 가설 |
|---|--------------|------------------|----------|--------|------|
|   |              |                  |          |        |      |

## 제공자 비교 (선택)

OpenAI vs Claude 두 번 실행한 경우만 작성.

| 분류 | OpenAI | Claude | 차이 | 판정 |
|------|--------|--------|------|------|
| 전체 | | | | ≤ 5%p |

## 결론

- 합격 여부:
- 이월 이슈:
- 후속 조치:
- 다음 측정 트리거:
```

---

## 실행 절차 (LIVE 테스트)

1. `.env` 에 `OPENAI_API_KEY`, `CLAUDE_API_KEY` 세팅 확인
2. 실행:
   ```bash
   cd backend
   OPENAI_API_KEY=$(grep '^OPENAI_API_KEY=' .env | cut -d= -f2-) \
   CLAUDE_API_KEY=$(grep '^CLAUDE_API_KEY=' .env | cut -d= -f2-) \
   LIVE_TEST=true ./gradlew test --tests "{GoldenSetLiveTest}" --info
   ```
3. 실행 로그에서 `[MISS]`, `골든셋 정확도: X/Y = Z%`, `BUILD SUCCESSFUL` 확인
4. 본 문서 템플릿 복제 → 수치 기입 → `{YYYY-MM-DD}-{scope}.md` 로 저장
5. Micrometer 지표는 스테이징 배포 후 Grafana 캡처 첨부

## 자동화 후보 (Phase 외)

- 주간 cron 으로 골든셋 회귀 — 정확도 5%p 이상 하락 시 Slack alert
- 새 프롬프트 PR 머지 직전 pre-merge job 에서 자동 실행 (비용 < 1c / 회)
- LIVE_TEST 전체 suite 를 별도 GitHub Action `performance.yml` 로 schedule 화

## 주의

- LLM 출력은 **비결정적**이다. 1회 측정이 아닌 **3회 평균** 을 권장 (최소 2회)
- 개별 케이스 hard-assert 금지. 집계 지표만 PASS/FAIL
- 사용자 입력(골든셋 발화문) 에 PII 포함 금지 — 합성 데이터만 사용
- API 키 / 로그 / 오답 샘플에 토큰·JWT 섞여 저장되지 않도록 확인
- 비용 관리: 대형 골든셋(100건 이상) 은 명시 승인 필요
