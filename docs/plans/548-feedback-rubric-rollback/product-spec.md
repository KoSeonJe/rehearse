# Product Spec — 피드백 시스템 롤백 (루브릭/차원 채점 제거, main 단순 피드백 복원)

> **작성자**: 사용자 (PM 초안: create-product-spec)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성
> **관련 Issue**: #548 (Epic)

---

## 문제 상황 (Problem)

- **현재 상태 (develop)**: 인터뷰 종료 후 피드백 화면이 루브릭 기반으로 동작한다.
  - 답변별 피드백 = 차원(dimension) 점수 카드(1~5점) + 관찰 텍스트 + 근거 인용 + `NOT_EVALUABLE` 상태 노출.
  - 비언어 피드백 = D11~D14 차원 점수 카드 형식 (lambda 분석이 차원 점수로 산출).
  - 세션 단위 종합 피드백 모달(강점/약점/주간계획) + 코치노트 FAB 추가됨.
  - 데이터: `question_score`, `question_score_dimension`, `session_feedback`, (구) `rubric_score` 테이블.
- **직전 상태 (main, 복원 목표)**: 피드백 화면이 2탭 코멘트 기반이다.
  - Content 탭 = 구조화 코멘트(긍정/부정/제안) + 정확도 이슈 + 코칭.
  - Delivery 탭 = 시선/자세/표정 레벨 + 발화속도/톤/감정/필러어 배지.
  - 데이터: 단일 `timestamp_feedback` 테이블, 세션 종합 피드백 없음.
- **인지 채널**: 사용자(제품 오너)가 develop 의 루브릭 차원 점수 방식을 직접 사용해보고 "이전 방식이 더 낫다" 고 판단.

## 왜 해야 하는가 (Why)

- **사용자 임팩트**: 피드백은 본 서비스의 핵심 가치. 루브릭 차원 점수 방식이 사용자가 보기에 직관성·유용성이 떨어진다고 판단됨 → 핵심 제품 경험 저하.
- **운영 / 시스템 임팩트**: 루브릭 시스템은 채점 파이프라인·이벤트 리스너·세션 synthesizer·다수 DB 테이블·프롬프트 템플릿으로 복잡도가 큼. 가치 대비 유지 비용 과다.
- **외부 압력**: 제품 오너의 명시적 롤백 요구. 루브릭 데이터는 dev 전용이라 폐기 비용 없음.

## 해결 방향 (Approach)

핵심 접근: **피드백 영역만 main 형식으로 논리적 롤백**. develop 의 무관 개선(질문풀 어드민, 이력서 트랙 재설계, 버그픽스 등)은 유지한 채 피드백 관련 코드 경로만 main 으로 되돌린다.

- 답변별 피드백을 루브릭 차원 점수 → main 의 코멘트 기반 Content/Delivery 2탭으로 복원.
- 비언어 피드백을 D11~D14 차원 점수 → main 의 배지/레벨 형식으로 복원 (lambda 비언어 출력 형식 되돌림 또는 변환).
- 세션 종합 피드백은 **유지하되 루브릭 의존/용어를 제거**한다. 종합 피드백 생성 프롬프트가 차원 점수가 아닌 코멘트 기반 데이터로 서술형 결과를 만들도록 재구성한다. (품질 고도화는 비스코프 — 아래 참조.)
- dev 환경 루브릭/차원/점수 테이블은 forward 마이그레이션으로 정리(데이터 dev 전용 → 백업 불필요).

대안 비교:
- 전체 `git reset main`: 단순하나 무관 개선 160커밋 전부 손실 → 기각.
- 피드백 영역 논리적 롤백 (채택): 무관 작업 보존 + 운영 정합성 유지.

단계 분리: 단일 phase. 롤백은 부분 적용 시 화면/응답 불일치로 깨진 상태가 되므로 원자적으로 처리.

## Evidence

- 코드 추적:
  - `frontend/src/components/feedback/feedback-panel.tsx` — main=2탭(Content/Delivery), develop=단일 섹션 + 세션 모달.
  - `frontend/src/components/feedback/content-tab.tsx` — develop 에서 89→183줄, 루브릭 차원 + 비언어 섹션 렌더.
  - `backend/.../domain/feedback/rubric/**`, `feedback/score/**`, `feedback/session/**` — develop 전용 추가분.
  - `backend/.../domain/feedback/dto/TimestampFeedbackResponse.java` — main=content+delivery, develop=technicalFeedback+nonverbalFeedback 차원 구조.
- DB: develop V24~V52 적용 (main ≈ V23). 루브릭/점수 테이블 = `rubric_score`(V26), `question_score`(V36), `question_score_dimension`, `nonverbal_score`(V33), `session_feedback`(V27).
- lambda: 비언어 신호 → D11~D14 차원 점수 매핑 도입 (#374), google.genai SDK 교체 (#516).
- 루브릭 도입 PR: #369(턴 10차원 채점), #489(비언어 차원 점수), #543(세션 피드백).
- 사용자 결정: 논리적 롤백 + dev 전용 루브릭 데이터 drop + delivery 전체 롤백 + 세션 피드백 유지·프롬프트 개선.

## Goal

- [ ] 인터뷰 종료 후 피드백 화면이 main 의 Content / Delivery 2탭 구조로 노출된다.
- [ ] BE 피드백 응답에 dimension / rubric / score 관련 필드가 부재하고, main 형식(content / delivery) 필드만 존재한다.
- [ ] 세션 종합 피드백이 루브릭 차원·점수 용어 없이 강점/약점/계획을 코멘트 기반 서술형으로 노출한다.
- [ ] dev 환경에서 인터뷰를 완주해도 루브릭/차원/점수 테이블에 신규 데이터가 생기지 않는다.
- [ ] 질문풀 어드민·이력서 트랙 등 무관 기능이 회귀 없이 동작한다 (회귀 테스트 통과, 빌드/ArchUnit 통과).

## Non-Goals

- 새로운 피드백 방식 설계 — 사유: 이번 작업은 main 복원(롤백)이지 신규 UX 설계가 아님.
- 질문풀 어드민·이력서 트랙·lambda SDK 등 루브릭 무관 develop 개선 변경 — 사유: 보존 대상.
- prod DB 데이터 마이그레이션 — 사유: 루브릭 데이터는 dev 환경에만 존재.
- 세션 종합 피드백 시각 레이아웃 개편 — 사유: 콘텐츠/프롬프트 정리만 목표, 디자인 신규 작업 아님.

## 수용 기준 (Acceptance Criteria)

- [ ] 인터뷰 종료 후 피드백 화면에 Content / Delivery 두 탭이 노출되고, 차원 점수 카드(1~5점)는 화면 어디에도 보이지 않는다.
- [ ] Content 탭에 구조화 코멘트(긍정/부정/제안) + 정확도 이슈 + 코칭이 main 과 동일한 구조로 노출된다.
- [ ] Delivery 탭에 시선/자세/표정 레벨 + 발화속도/톤/감정/필러어가 배지·레벨 형식으로 노출된다.
- [ ] 세션 종합 피드백을 열면 강점/약점/계획이 루브릭 차원명·점수 표기 없이 서술형 텍스트로 노출된다 (각 항목 비어있지 않게 생성됨 — 품질/깊이 평가는 비스코프).
- [ ] dev 환경에서 인터뷰 1회 완주 후, 루브릭/차원/점수 테이블에 신규 row 가 적재되지 않는다.
- [ ] 질문풀 어드민 페이지와 이력서 트랙 면접 흐름이 회귀 없이 동작한다 (해당 회귀 테스트 통과).
- [ ] BE/FE/lambda 빌드, 전체 테스트, ArchUnit 가드가 모두 통과한다.

## 비스코프 (Don't)

- 새 피드백 UX/디자인 개편 — 사유: 이번은 main 형식 복원만. 신규 디자인은 별도 plan.
- prod DB 루브릭 데이터 처리 — 사유: 데이터 dev 전용, 처리할 운영 데이터 없음.
- 세션 종합 피드백 모달 레이아웃·시각 디자인 신규 작업 — 사유: 루브릭 의존/용어 제거까지만.
- 세션 종합 피드백 품질 고도화 (답변 인용 정확도 / 서술 깊이 / 톤 튜닝) — 사유: 이번은 루브릭 의존 제거 + 서술형 동작 복원까지. 품질 개선은 별도 plan.
- 루브릭 채점 품질 개선 / 재도입 — 사유: 롤백이 목적, 반대 방향 작업.

## 참고

- 관련 Issue: #548 (Epic)
- 관련 PR (롤백 대상): #369, #374, #384, #385, #489, #490, #543
- 사용자 결정 기록:
  - 롤백 범위 = 논리적 롤백 (무관 작업 유지)
  - DB = dev 전용 루브릭 데이터 그냥 drop
  - Delivery = 전체 롤백 (verbal+delivery, lambda 비언어 출력 main 형식 복원 포함)
  - 세션 피드백 = 유지 + 루브릭 의존/용어 제거 + synthesizer 프롬프트 코멘트 기반 재구성 (품질 고도화는 별도 plan)
