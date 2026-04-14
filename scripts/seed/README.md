# seed TTS 주입 Runbook

17 개 seed SQL (`backend/src/main/resources/db/seed/*.sql`) 의 `question_pool` INSERT 에
`tts_content` 컬럼을 채워 넣는 자동화 스크립트.

## 전제

- Python 3.9+
- `pip install anthropic`
- 유효한 `ANTHROPIC_API_KEY` 환경변수 (Claude Haiku 호출용, 예상 비용 ~$0.3~0.5)

## 1회 전체 실행

```bash
export ANTHROPIC_API_KEY=sk-ant-...

# (1) LLM 으로 tts 매핑 생성 (약 1,348건, 동시 8 요청)
python3 scripts/seed/generate_tts_content.py \
  --seed-dir backend/src/main/resources/db/seed \
  --out scripts/seed/out/tts_mapping.json

# (2) seed 파일에 주입 (dry-run 먼저)
python3 scripts/seed/inject_tts_into_seed.py \
  --seed-dir backend/src/main/resources/db/seed \
  --mapping scripts/seed/out/tts_mapping.json \
  --dry-run

# 문제 없으면 실제 적용
python3 scripts/seed/inject_tts_into_seed.py \
  --seed-dir backend/src/main/resources/db/seed \
  --mapping scripts/seed/out/tts_mapping.json
```

## 20건 샘플 실행 (권장, 전체 실행 전에)

```bash
python3 scripts/seed/generate_tts_content.py \
  --seed-dir backend/src/main/resources/db/seed \
  --out scripts/seed/out/tts_mapping.json \
  --limit 20

# 샘플 결과 확인
jq 'to_entries | map({k: .key, q: .value.content, tts: .value.tts_content}) | .[0:5]' \
  scripts/seed/out/tts_mapping.json

# OK 이면 --resume 로 나머지 이어서
python3 scripts/seed/generate_tts_content.py \
  --seed-dir backend/src/main/resources/db/seed \
  --out scripts/seed/out/tts_mapping.json \
  --resume
```

## 검증

```bash
# 1. 모든 seed 에 tts_content 존재
grep -L 'tts_content' backend/src/main/resources/db/seed/*.sql
# README.md 제외 비어야 함

# 2. 마크다운 누출 검사 (Python *args/**kwargs 는 예외)
grep -nE '\*\*[^k]' backend/src/main/resources/db/seed/*.sql
grep -nE '\*\*$' backend/src/main/resources/db/seed/*.sql

# 3. 파서 스모크 테스트 재실행 (주입 후 카운트 갱신)
python3 scripts/seed/_smoke_test.py
# 기대: 주입된 파일들이 has_tts 가 total 과 같아진다
```

## 롤백

모든 작업은 seed 파일에 대한 텍스트 편집이므로 `git restore backend/src/main/resources/db/seed/` 로 즉시 원복.

## 파일 구성

| 파일 | 역할 |
|---|---|
| `generate_tts_content.py` | LLM 호출, tts_mapping.json 생성 |
| `inject_tts_into_seed.py` | 매핑을 seed SQL 의 INSERT 문에 주입 |
| `_smoke_test.py` | 파서·주입 로직 검증 (LLM 미호출) |
| `out/tts_mapping.json` | 생성된 TTS 매핑 (gitignore 권장 아님 — 재현성을 위해 커밋 고려) |

## 알려진 제약

- INSERT 문이 `INSERT IGNORE INTO question_pool ( ... ) VALUES ( 'X', 'Y', ...` 형태일 때만 매칭됨. 멀티-VALUES 튜플이나 다른 문법은 지원 안 함.
- `content` 컬럼 내 작은따옴표(`'`)는 `''` 이스케이프로 저장돼야 파싱됨 (MySQL 표준).
- 생성된 `tts_content` 에 `'` 가 섞이면 자동으로 `''` 이스케이프 처리 (inject 단계).
