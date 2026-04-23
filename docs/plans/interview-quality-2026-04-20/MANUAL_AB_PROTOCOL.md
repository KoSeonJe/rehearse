# Manual A/B Comparison Protocol

> 작성일: 2026-04-23
> 목적: 각 plan PR 머지 전 품질 회귀를 수동으로 확인하는 표준 절차
> 배경: A/B 비교 인프라(plan-10 Eval Harness / Feature Flag runtime toggle)를 공격적으로 축소하고, ECR 이미지 2개를 개발서버 2개 포트에 병렬 기동한 뒤 동일 입력으로 수동 비교하는 방식으로 대체함.

---

## 왜 이 방식인가

자동화된 Judge LLM 기반 A/B 비교(plan-10 골든셋 + J1/J2/J3)는 구축 비용 대비 실제 사용 빈도가 낮고, 측정 인프라 자체가 본체(LLM 파이프라인 개선)보다 복잡해지는 문제가 있었다. "신버전이 구버전보다 진짜 나아졌는가"를 판단하는 목적에 한해, ECR 이미지 2개 + 수동 diff 3~5건이 충분하다는 판단에 따라 이 프로토콜로 대체한다.

---

## 사전 준비

- ECR 태그 2개 준비:
  - `before-{plan-id}`: 현재 develop 브랜치 기준 이미지
  - `after-{plan-id}`: PR 브랜치 기준 이미지
- 스테이징 EC2 또는 로컬 docker-compose에서 2개 컨테이너를 각각 8081/8082 포트로 기동
- 비교용 녹화 세션 3~5건 준비 (카테고리 커버):
  - CS_FUNDAMENTAL × 1
  - LANGUAGE_FRAMEWORK × 1
  - RESUME_BASED × 1~2
  - BEHAVIORAL × 1

---

## 실행 절차

1. 동일 녹화본을 두 컨테이너에 순차 투입 (실제 API 호출로, 모킹 금지)
2. 각 컨테이너의 세션 종료 피드백 JSON + 중간 꼬리질문 로그 저장
3. `diff` 또는 `jd` (JSON diff) 도구로 시각적 비교
4. 결과 요약 마크다운을 `eval/manual-ab/{YYYY-MM-DD}-{plan-id}.md` 에 기록

---

## 판정 기준 (정성)

- 신버전 꼬리질문이 구버전 대비 "사용자 답변 claim 에 더 정확히 꽂힌다" 가 3~5건 중 과반 이상
- 신버전 피드백이 구버전 대비 "관찰 인용 포함 + 다음 액션 구체" 가 과반 이상
- 지연/비용은 Grafana `rehearse_ai_call_duration_seconds` p95 로 별도 확인
- 회귀 의심 시 PR 반려 → 원인 분석 → 재실행

---

## 자동 검증 (유지)

Judge LLM 없이 정규식/assert 기반으로 다음 3개 게이트는 자동 검증으로 유지한다. plan-08 RubricScorer 단위/통합 테스트가 커버한다.

- G1 차원 누락률 0%: `DIMENSIONS_TO_SCORE` 지정 차원 중 score=null 비율 (의도적 null 제외)
- G2 evidenceQuote 포함률 ≥ 95%: score != null 차원의 인용 포함 여부
- G3 매핑 정확도 100%: `_mapping.yaml` 규칙대로 기대 rubric_id 귀결

---

## Cut-over (plan-13 포함)

Lambda Content Removal처럼 비가역 cut-over는 본 프로토콜 3~5건을 통과한 뒤 **ECR 태그 배포 + Lambda 함수 버전 업데이트를 동시에 진행**한다. 롤백은 이전 ECR 태그 재배포 + Lambda 이전 버전 alias 복구.

- 롤백 수단: ECR 이미지 태그 재배포 + 세션 스토어 캐시 퍼지
- 개별 feature flag runtime toggle은 사용하지 않음 (ECR 배포 단위로 전환)

---

## 결과 기록 형식

`eval/manual-ab/{YYYY-MM-DD}-{plan-id}.md` 에 아래 형식으로 저장:

```markdown
# Manual A/B 비교 결과 — {plan-id}

날짜: YYYY-MM-DD
ECR before: {tag}
ECR after: {tag}

## 세션별 비교

| 세션 | 카테고리 | 꼬리질문 판정 | 피드백 판정 | 특이사항 |
|------|---------|------------|-----------|---------|
| s01  | CS      | after ↑   | 동등       | —       |
| ...  | ...     | ...        | ...       | ...     |

## 종합 판정

- 꼬리질문: X/5건 after 우세
- 피드백: X/5건 after 우세
- 레이턴시: before p95 Xs → after p95 Xs
- 결론: 승인 / 재검토
```
