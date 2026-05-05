# {도메인명} 스키마

> 대상 마이그레이션: `backend/src/main/resources/db/migration/V*__*.sql` 중 {도메인명} 관련

## 테이블 목록

| 테이블 | 성격 | 1:N 관계 |
|--------|------|---------|
| {table_a} | {1줄 성격} | {table_b} N |
| {table_b} | {1줄 성격} | — |

---

## {table_name}

### 성격
{이 테이블이 무엇을 표현하는지 1-2문장. row 1개 = 무엇 1개 단위?}

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `user_id` | BIGINT | FK → user.id, NOT NULL | 소유자 |
| `created_at` | DATETIME(6) | NOT NULL | 생성 시각 |
| `updated_at` | DATETIME(6) | NOT NULL | 마지막 갱신 |
| ... | ... | ... | ... |

### 인덱스
- `idx_{name}` (`col_a`, `col_b`) — {조회 패턴 / 사유}
- `uk_{name}` (`col_x`) — UNIQUE — {불변 제약 사유}

### 불변 / 정책
- {갱신 금지 컬럼 — 어떤 컬럼이 INSERT 후 변경 불가, 사유}
- {soft delete 여부 / TTL / 백필 정책}
- {복합 invariant — 예: status=COMPLETED 이면 ended_at NOT NULL}

### 마이그레이션 히스토리
- `V{NN}__{slug}.sql` — {1줄 요약}
