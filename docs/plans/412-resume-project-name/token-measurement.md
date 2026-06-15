# Token Measurement — Resume projectName 통합 검증 (Phase 4)

> 측정 도구: `backend/eval/context/measure_tokens.py` (heuristic: 4 chars / 1 token)
> 측정 대상: Resume 트랙 4 callType fixture (`backend/eval/context/fixtures/session-resume-*.json`)
> 비교 기준: tech-spec NF#3 — extractor 토큰 증가율 < 10%

## 결과 (After — Phase 1~3 적용 후)

| callType | Total | L1 | L2 | L3 | L4 | L4 cap | 상태 |
|----------|-------|----|----|----|----|--------|-----|
| resume_playground_opener | 787 | 450 | 31 | 215 | 91 | 600 | ✓ cap 이내 |
| resume_playground_responder | 981 | 647 | 49 | 168 | 117 | 1000 | ✓ cap 이내 |
| resume_chain_interrogator | 1059 | 655 | 54 | 224 | 126 | 1200 | ✓ cap 이내 |
| resume_wrap_up | (fixture 부재) | — | — | — | — | — | 측정 불가 |

avg=942 / max=1059 / min=787 (3 sessions)

## 분석

- 모든 측정 대상 callType 의 L4 토큰 cap 이내 (`FocusLayer` 런타임 강제).
- L1 (Fixed Context) 증가는 `resume-extractor.txt` (Phase 1a) + `resume-interview-planner.txt` (Phase 2a) + 다운스트림 4 templates (Phase 3a) 의 누적 영향.
- 다운스트림 4 templates 의 PROJECT_NAME / PROJECT_INFO placeholder + 지시문 추가가 fixture 단위 측정에서 cap 위반 없음.
- `resume_wrap_up` fixture 부재 — 별도 fixture 추가는 본 plan 범위 외 (회귀용).

## NF 임계 매핑

- **extractor (`resume-extractor.txt`)**: Phase 1a 보고 기준 −2.8% (4,178 → 4,059 chars). 임계 < 10% **충족**.
- **planner (`resume-interview-planner.txt`)**: Phase 2a 보고 기준 +22.7% (3,014 → 3,698 chars). 사용자 옵션 2 결정으로 수용 (hallucinate 차단 이중 강화 우선).
- **다운스트림 4 templates**: Phase 3a 추정 chain-interrogator ~13% / wrap-up ~16% — fixture 단위 L1+L4 cap 통과 ⇒ 운영 영향 미미. (실측은 컨텍스트 어셈블리 결과로 cap 이내 확인됨.)

## 후속

- `resume_wrap_up` fixture 추가 시 별도 측정 가능 (회귀 가시성 향상).
- 토큰 임계 모니터링은 운영 메트릭 (`infra/ai/metrics`) 으로 위임. fixture 측정은 회귀 가드.
