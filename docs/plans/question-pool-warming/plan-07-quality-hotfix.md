# Phase 0: P0 Hotfix — 오류/오타/잘린 답변 수정

## Why

시드 데이터 전수 검증 중 기술적 오류 2건, 오타 1건, 잘린(truncated) 답변 ~18건 발견.
이들은 사용자에게 직접적으로 잘못된 정보를 제공하므로 즉시 수정 필요.

## 수정 대상

### 확인된 오류 (2건)
1. `cs-fundamental-junior.sql` line 9 — "LinkedList는 삽입/삭제가 O(1)" → 노드 참조 조건 누락
2. `system-design-junior.sql` line 26 — "HashMa" → "HashMap" 오타

### Truncated 답변 (~18건, 전수 스캔 필요)
model_answer가 80자 미만이거나 문장이 중간에 끊긴 항목. 대상 파일:
- `cs-fundamental-mid.sql`, `cs-fundamental-senior.sql`
- `backend-python-django.sql`, `backend-node-nestjs.sql`
- `frontend-react-ts.sql`, `frontend-vue-ts.sql`
- `fullstack-react-spring.sql`, `system-design-senior.sql`

## 작업 절차

1. 전체 seed SQL 파일에서 truncated 답변 스캔 (80자 미만 model_answer 검출)
2. 확인된 오류 2건 수정
3. truncated 답변 복원 (질문 맥락에 맞는 완전한 답변으로)
4. SQL 문법 유효성 확인
5. INSERT 수 변동 없음 확인

## Agent 할당

- **Implement**: `executor` — 오류 수정 + truncated 답변 복원
- **Review**: `code-reviewer` — 수정 정확성 확인

## 검증

- [ ] truncated 답변 0건
- [ ] 기술적 오류 0건
- [ ] 각 파일 INSERT 수 원본과 동일
- [ ] SQL 문법 유효
