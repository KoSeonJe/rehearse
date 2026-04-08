# Prompt Improvement 2026-04

## Why
Rehearse는 4개 LLM(Claude Sonnet/Haiku, GPT-4o, GPT-4o Vision, Gemini 2.0 Flash)을 혼합 사용하지만, 전체 프롬프트를 일관된 기준으로 종합 감사한 적이 없다. 최근 3~4개월간 후속질문 자연스러움·스키마 방어·prompt caching 등 점진적 개선이 있었으나 중복·토큰 낭비·구조 불일치가 누적됐다. 또한 비언어 분석에서 **시선(eye contact) 피드백이 프롬프트 지시와 무관하게 반복 출력되는 현상**이 확인되어 즉시 교정이 필요하다.

## Goal
- **품질**: 영역별 골든셋 10~15케이스 5점 척도 평균 ≥ 4.0 (baseline 비회귀)
- **토큰·비용**: System 프롬프트 입력 토큰 ≥20% 절감
- **구조**: 영역별 프롬프트 카탈로그 1개, 버전·변경 이력·anti-pattern 정리
- **즉시 목표 (Lane 5)**: 자세/표정 텍스트 피드백에서 시선 관련 어휘 0회

## Evidence
- `docs/plans/prompt-redesign/` 완료된 2차원 페르소나 시스템 (Base × Overlay) — 재사용
- 최근 커밋: `03f5c3a`, `76bf4d4`, `d794e13`, `ab68b2f` (후속질문·질문 품질·prompt caching 개선)
- 기존 가이드: `docs/plans/prompt-redesign/background/prompt-test-guide.md`
- 에이전트: `.claude/agents/prompt-engineer.md` (A/B, few-shot, CoT, token optimization 전문)

## Trade-offs
- **동작 변경 최소화 vs 구조 개선**: Lane 2(후속질문)는 최근 집중 개선된 영역이므로 동작 불변 원칙. Lane 1/3/4는 구조 리디자인 허용.
- **Gemini response_schema 전환**: 파서 깨질 위험 → 먼저 프롬프트 내 스키마 유지한 축소만 시도.
- **eyeContactLevel enum 제거(안 A) vs 프롬프트 가드(안 B)**: 파서/FE/DB 영향 크므로 기본은 (B)+(C) 가드 방식. (A)는 영향도 조사 후 별도 결정.

## Scope (5 Lanes)
| # | 영역 | 파일 | 모델 |
|---|------|------|------|
| 1 | BE 질문 생성 | `backend/.../prompt/QuestionGenerationPromptBuilder.java` + `resources/prompts/template/question-generation.txt` + base/overlay YAML 23개 | Claude Sonnet 4 |
| 2 | BE 후속질문 | `.../FollowUpPromptBuilder.java` + `template/follow-up.txt` | Claude Haiku 4.5 |
| 3 | Lambda 언어분석 | `lambda/analysis/analyzers/verbal_prompt_factory.py` | GPT-4o |
| 4 | Lambda Gemini 종합 | `lambda/analysis/analyzers/gemini_analyzer.py` | Gemini 2.0 Flash |
| 5 | Lambda 비언어 (시선 핫픽스) | `lambda/analysis/analyzers/vision_analyzer.py` | GPT-4o Vision |

## Execution Order
1. **Stage 1 — Lane 5 핫픽스** (선행, 독립): 시선 피드백 제거 → Lambda 배포
2. **Stage 2 — Phase 0 벤치마크**: 골든셋 수집, baseline 출력·토큰 기록
3. **Stage 3 — Lane 1~4 Audit + Redesign**: `prompt-engineer` 4개 병렬 dispatch
4. **Stage 4 — Lane별 리뷰**: `code-reviewer`, `architect-reviewer`, `qa`, `backend` 조합
5. **Stage 5 — 통합 검증 게이트**: `test-engineer` + `critic`. 품질 비회귀 AND 토큰 ≥20% 절감 만족 시만 적용
6. **Stage 6 — Lane별 독립 PR 생성**
7. **Stage 7 — `REPORT.md` 문서화**

## Status
- [x] Stage 0: Spec 디렉토리 생성
- [ ] Stage 1: Lane 5 핫픽스 (In Progress)
- [ ] Stage 2: 벤치마크 수집
- [ ] Stage 3: Lane 1~4 Audit
- [ ] Stage 4: 리뷰
- [ ] Stage 5: 검증 게이트
- [ ] Stage 6: PR
- [ ] Stage 7: 리포트
