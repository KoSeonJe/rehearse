# Plan 11: Nonverbal Rubric (D11~D14, 결정론적 매핑)

> 상태: Draft
> 작성일: 2026-04-20 (TODO 09 반영 추가, 2026-04-21 plan-11a 선행 분리)
> 주차: W7 후반 (plan-11a 머지 후 착수, plan-08과 병렬, plan-09 선행)
> 원본: `docs/todo/2026-04-20/09-nonverbal-rubric.md`
> 선행 `[blocking]`: **plan-11a** (Gemini 프롬프트 필드 확장 — `speed_variance`/`gaze_on_camera_ratio`/`posture_unstable_count` 3개 신설)
> 선행: plan-08(`_dimensions.yaml` 확장), plan-00c(turn 상태에 이전 턴 시그널 캐시)
> 후행: plan-09 Synthesizer가 본 결과를 Delivery 섹션에서 소비

## Why

기술 루브릭(D1~D10)은 LLM으로 텍스트 답변을 채점하지만, 비언어(필러 워드 수/톤/시선/자세)는 **이미 Lambda Verbal+Vision analyzer가 숫자 지표를 생산**한다. 현재는 "filler_word_count: 12"가 많은지 적은지 기준이 없어 피드백이 "음... 어... 가 많으셨어요" 수준에 머무름.

비언어는 카테고리와 독립적이고 수치 기반 → **단일 공통 루브릭 + 결정론적 매퍼(LLM 호출 0)** 로 D11~D14 4차원 채점. 기술 루브릭과 다른 데이터 소스·처리 방식이라 plan-08과 섞지 않고 형제 플랜으로 분리.

### 현 프로젝트 적합성 (반영)
- Lambda 기존 `verbal_analyzer.py` 출력: `filler_word_count`, `tone_label ∈ {PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE}` — D11/D12 원천 **존재**
- `speed_variance`, `gaze_on_camera_ratio`, `posture_unstable_count` 3개 수치 필드는 **VERIFICATION_REPORT.md §D3 에서 부재 확인 → plan-11a 에서 선행 확장 완료 후 본 plan 착수**
- CLAUDE.md: Lambda 분석 프롬프트 개편은 `prompt-improvement-2026-04` Lane 3-5 별도 트랙. **plan-11a 는 필드 확장만의 최소 범위**, 프롬프트 전면 개편은 Out of Scope.
- AI Provider Stack(memory): Lambda는 **Gemini 주력**. Whisper는 STT fallback. 매핑은 결정론적이라 provider 무관.

## 전제 (Phase 0 선행 필수)

- **`[blocking]` plan-11a** — `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` 3개 Lambda 출력 필드 확장 완료
- plan-00a `INVENTORY.md` — Lambda 코드 현황 확인 (완료, VERIFICATION_REPORT §D3 참조)
- plan-00c — `InterviewRuntimeState`에 `lastTurnNonverbalSignals` 필드 추가 (D14 Composure 이전 턴 비교용)
- plan-00e `FEEDBACK_DOMAIN.md` — partial-first Delivery 섹션을 구조화 JSON으로 상향(기존 문자열 comment 대체)
- plan-08 `_dimensions.yaml` — D11~D14 차원 추가. plan-08 범위를 "기술 루브릭(D1~D10)만"으로 명시 주석

## 생성/수정 파일

### 신규 (Lambda Python)
| 파일 | 작업 |
|---|---|
| `lambda/analysis/analyzers/nonverbal_rubric_mapper.py` | 신규. 결정론적 threshold 매퍼. verbal/vision 결과 + prev_turn_signals → D11~D14 점수 |
| `lambda/analysis/tests/test_nonverbal_rubric_mapper.py` | 신규. 경계값 + 결정론 + D14 교차 분석 테스트 |

### 신규 (Backend YAML 리소스)
| 파일 | 작업 |
|---|---|
| `backend/src/main/resources/rubric/nonverbal-rubric.yaml` | 신규. scope=global, D11~D14 base_weight + per_turn_rules + level_expectations |
| `backend/src/main/resources/rubric/nonverbal-context-weights.yaml` | 신규. question_category/track/mode/difficulty 별 multiplier |
| `backend/src/main/resources/rubric/nonverbal-improvement-actions.yaml` | 신규. D11~D14 × level_1→2/2→3 개선 액션 템플릿 (plan-09 Synthesizer 소비) |

### 수정 (기존 plan-08 산출물)
| 파일 | 작업 |
|---|---|
| `backend/src/main/resources/rubric/_dimensions.yaml` | **수정**. D11~D14 4개 차원 추가. 각 차원에 `category: nonverbal`, `data_source`, `measurement`, threshold-based scoring |
| `backend/src/main/resources/rubric/_mapping.yaml` | **수정**. 신규 rule: `always_apply: nonverbal-rubric` (기술 rubric과 **병렬** 적용, 대체 아님) + context_weights 참조 |

### 신규 (Backend Java)
| 파일 | 작업 |
|---|---|
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/NonverbalRubricScorer.java` | 신규 `@Service`. Lambda 이벤트 핸들러에서 호출. Python 매퍼 결과를 수신만 하는 얇은 어댑터 (단, context_weights 적용은 Java 측에서) |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/NonverbalTurnScore.java` | 신규 record. D11~D14 + raw_signals + appliedContextMultiplier |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/NonverbalContextWeightsLoader.java` | 신규. YAML 로드 + `resolve(questionCategory, track, mode, difficulty)` |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/entity/NonverbalScoreEntity.java` | 신규 JPA Entity (V28 매핑) |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/repository/NonverbalScoreRepository.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/dto/NonverbalScoreResponse.java` | 신규 DTO |

### 수정 (plan-00c 연계, 신규 Flyway)
| 파일 | 작업 |
|---|---|
| `backend/src/main/resources/db/migration/V28__create_nonverbal_score.sql` | 신규. `rubric_score`와 분리된 테이블(결정론 매퍼라 별도 aggregate) |
| `backend/src/main/resources/db/migration/rollback/V28__rollback.sql` | 신규 |

### 수정 (Lambda handler 출력 — 프롬프트 필드는 plan-11a 에서 선행 확장)

> **중요**: `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` 3개 필드의 **Gemini 프롬프트 스키마 확장은 plan-11a 에서 완료**. 본 plan 은 확장된 필드를 소비하는 매퍼와 handler 페이로드 연결만 담당.

| 파일 | 작업 |
|---|---|
| `lambda/analysis/handler.py` | **수정**. `nonverbal_rubric_mapper.score_turn(verbal, vision, prev, meta)` 호출 후 결과를 기존 이벤트 페이로드에 `nonverbal_score` 필드로 첨부. plan-11a 가 제공하는 3개 수치 필드 소비 |

### 수정 (plan-09 연계)
| 파일 | 작업 |
|---|---|
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackService.java` (plan-09 산출) | 수정. Delivery 섹션에 D11~D14 점수 + 관찰 + 개선 액션 주입 (nonverbal-improvement-actions.yaml 참조) |
| `backend/src/main/resources/prompts/template/session-feedback-synthesizer.txt` (plan-09) | 수정. "비언어 및 전달" 섹션을 차원별 구조로 강제 |

## 상세

### D11~D14 차원 요약 (정의는 `_dimensions.yaml` 본체)

| ID | 차원 | 데이터 소스 | 점수 규칙(요약) |
|---|---|---|---|
| D11 | Fluency | verbal.filler_word_count | ≥6→1, 3~5→2, ≤2→3 |
| D12 | Confidence Tone | verbal.tone_label + speed_variance | HESITANT 또는 var≥0.3→1, CONFIDENT/PROFESSIONAL 및 var<0.15→3, 그 외→2 |
| D13 | Eye Contact & Posture | vision.gaze_on_camera_ratio + posture_unstable_count | gaze<0.3 또는 posture>5→1, gaze>0.7 및 posture≤2→3, 그 외→2 |
| D14 | Composure | D11/D12/D13 × prev_turn 비교 (difficulty≥medium) | drops≥2→1, drops=1→2, 0→3 |

### Deterministic Mapper (LLM 0)
- Python `NonverbalRubricMapper.score_turn(verbal, vision, prev, meta)` — plan-08 Scorer와 **완전 분리**
- 같은 입력 → 같은 점수 (100% 재현성). Judge 불필요.

### Context Weights 적용 (Java)
- Python 매퍼는 raw 점수만 산출. Java `NonverbalRubricScorer`가 `NonverbalContextWeightsLoader.resolve(...)`로 multiplier 적용
- 분리 근거: Lambda는 단일 턴만 알고 context(session track, mode, difficulty)는 backend가 정답
- 규칙 예시:
  - BEHAVIORAL/COMMUNICATION → ×1.2 / CS_FUNDAMENTAL → ×0.8 / RESUME+INTERROGATION → ×1.3 / RESUME+PLAYGROUND → ×0.9 + D14 비활성

### `_mapping.yaml` 병렬 적용
```yaml
# 기술 rubric 매핑(plan-08) + 비언어 rubric 병렬 적용
always_apply:
  - rubric_id: nonverbal-v1
    with_context_weights: nonverbal-cw-v1
rules: # (기존 plan-08)
```
→ 모든 세션 턴은 기술 rubric 1개 + nonverbal rubric 1개 2쌍 점수 산출

### V28 스키마 (rubric_score와 분리 이유: 결정론, no LLM, 다른 수명주기)
```sql
CREATE TABLE nonverbal_score (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  turn_id BIGINT NOT NULL,
  d11_fluency TINYINT,
  d12_tone TINYINT,
  d13_posture TINYINT,
  d14_composure TINYINT,
  raw_signals JSON NOT NULL,
  context_multiplier DECIMAL(3,2),
  created_at DATETIME NOT NULL,
  INDEX idx_interview_turn (interview_id, turn_id),
  CONSTRAINT fk_nv_score_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
```

### Feature Flag
```yaml
rehearse:
  features:
    nonverbal-rubric:
      enabled: false                 # default off, dogfooding 후 on
      context-weights-enabled: true
      composure-tracking: true
      fallback-to-raw-analysis: true # 매퍼/필드 부재 시 기존 TimestampFeedback 문자열로 복귀
```

### plan-09 Synthesizer 연계
- 신규 Synthesizer 입력: `NONVERBAL_SCORES_BY_TURN` + `NONVERBAL_AGGREGATE` (세션 평균 + D14 악화 턴 리스트)
- Delivery 섹션 템플릿은 본 plan의 `nonverbal-improvement-actions.yaml`의 `level_1_to_2` / `level_2_to_3` 액션을 우선순위(최저 점수 차원)로 제시

## 담당 에이전트

- Implement(Lambda): `backend` + `ai-media-specialist` — Python mapper + verbal/vision 프롬프트 필드 최소 확장
- Implement(Backend): `backend` + `database-architect` — YAML 로더/Scorer/Entity + V28
- Implement(Synthesizer 연계): plan-09 담당과 공동 — Delivery 섹션 스키마 확장
- Review: `architect-reviewer` — 기술/비언어 rubric 경계(섞지 말 것), `SessionFeedback` 스키마 확장 호환성
- Review: `code-reviewer` — 결정론 매퍼 경계값, context_weights 누수 방지, DTO 분리

## 검증

1. **결정론 테스트**: 동일 입력 100회 → 점수 100회 동일 (`pytest` 단위)
2. **경계값 테스트**: filler 2/3/5/6 → D11 3/2/2/1 (전수)
3. **D14 교차 분석**: prev D11=3 D12=3 → curr D11=1 D12=2(drops=2+1) → D14=1
4. **Context weights**: BEHAVIORAL ×1.2, CS ×0.8, RESUME/INTERROGATION ×1.3, RESUME/PLAYGROUND D14 disabled 4종 단위 테스트
5. **수동 라벨 대비 정확도 ≥90%** (20개 세션 수동 채점 vs 매퍼)
6. **D14 변화 감지 정확도 ≥80%** (꼬리질문 턴 기준 수동 라벨)
7. **Lambda 필드 확장 비회귀**: 기존 `TimestampFeedback` 생성 경로 회귀 0
8. **V28 영속화 + rollback** 로컬 동작
9. **Feature flag off**: 기존 Verbal/Vision 원본 문자열 경로로 완전 복귀
10. `progress.md` 11 → Completed

## Out of Scope

- Gemini 프롬프트 전면 개편(Whisper 포함) — `prompt-improvement-2026-04` Lane 3-5
- 세션 전체(session_wide_degradation) 패턴 분석의 고급 통계 — MVP 이후
- 사용자 설문 기반 유용성 측정 — 별도 피드백 트랙
- **Lambda `verbal` / `technical` 블록 제거** — **plan-13** (본 plan은 비언어 D11~D14 채점만. Lambda content 정리는 별건)

## 연계 plan (2026-04-22 추가)

- **plan-13 Lambda Content Removal**: Lambda가 content 블록을 제거하고 delivery 전용으로 축소되면, 본 plan의 D11~D14 비언어 채점은 그대로 유지되며 Lambda delivery 출력(`vocal` + `vision` + `attitude`)만 소비. 본 plan과 plan-13은 **독립**이며 동시 진행 가능.
- **plan-08 기술 루브릭 (D1~D10)**: Content 평가 유일 소스. 본 plan의 D11~D14와 **섞지 말 것** (데이터 소스도 다름: LLM vs 결정론 매퍼).
