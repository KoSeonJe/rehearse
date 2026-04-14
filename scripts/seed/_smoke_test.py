#!/usr/bin/env python3
"""파서/주입 로직 스모크 테스트 (LLM 미호출)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from generate_tts_content import iter_inserts, row_key  # noqa: E402
from inject_tts_into_seed import inject_into_text  # noqa: E402


SEED_DIR = Path(__file__).resolve().parents[2] / "backend/src/main/resources/db/seed"


def test_parse_counts():
    """각 seed 파일의 INSERT 수가 기대치와 일치하는지 검증."""
    expected = {
        "backend-java-spring.sql": (90, 90),  # (total, has_tts)
        "backend-kotlin-spring.sql": (90, 0),
        "backend-node-nestjs.sql": (90, 0),
        "backend-python-django.sql": (90, 0),
        "batch.sql": (13, 13),
        "behavioral-junior.sql": (30, 0),
        "behavioral-mid.sql": (30, 0),
        "behavioral-senior.sql": (30, 0),
        "cs-fundamental-junior.sql": (118, 0),
        "cs-fundamental-mid.sql": (120, 0),
        "cs-fundamental-senior.sql": (120, 0),
        "devops-aws-k8s.sql": (180, 0),
        "frontend-react-ts.sql": (180, 0),
        "frontend-vue-ts.sql": (90, 0),
        "fullstack-react-spring.sql": (90, 0),
        "system-design-junior.sql": (30, 0),
        "system-design-mid.sql": (30, 0),
        "system-design-senior.sql": (30, 0),
    }

    failed = []
    for name, (exp_total, exp_tts) in expected.items():
        path = SEED_DIR / name
        if not path.exists():
            failed.append(f"{name}: FILE MISSING")
            continue
        rows = list(iter_inserts(path.read_text("utf-8")))
        total = len(rows)
        has_tts = sum(1 for _, _, h in rows if h)
        if total != exp_total or has_tts != exp_tts:
            failed.append(
                f"{name}: got total={total} has_tts={has_tts}, "
                f"expected total={exp_total} has_tts={exp_tts}"
            )
        else:
            print(f"  OK  {name}: total={total} has_tts={has_tts}")
    if failed:
        print("\n[FAIL]")
        for f in failed:
            print(f"  {f}")
        sys.exit(1)
    print("\n[PASS] parse counts")


def test_inject_roundtrip():
    """kotlin-spring 파일의 첫 INSERT 를 가짜 매핑으로 주입해본다."""
    path = SEED_DIR / "backend-kotlin-spring.sql"
    text = path.read_text("utf-8")
    rows = list(iter_inserts(text))
    assert rows, "no rows parsed"
    cache_key, content, has_tts = rows[0]
    assert not has_tts
    fake_mapping = {
        row_key(cache_key, content): {
            "content": content,
            "tts_content": "TEST_TTS_VALUE",
        }
    }
    log: list[str] = []
    new_text, inj, skipped = inject_into_text(text, fake_mapping, log)
    assert inj == 1, f"expected 1 injection, got {inj}"
    # 신포맷 INSERT 가 포함됐는지 확인
    assert "tts_content" in new_text
    assert "'TEST_TTS_VALUE'" in new_text
    # 컬럼 선언에도 tts_content 있어야 함
    assert "(cache_key, content, tts_content," in new_text.replace("\n", " ")
    print(f"\n[PASS] inject roundtrip: injected={inj} skipped={skipped}")


def test_inject_skips_existing():
    """backend-java-spring.sql 은 이미 tts_content 있으므로 주입 0건이어야."""
    path = SEED_DIR / "backend-java-spring.sql"
    text = path.read_text("utf-8")
    log: list[str] = []
    _, inj, skipped = inject_into_text(text, {}, log)
    assert inj == 0, f"expected 0 injection on already-tts file, got {inj}"
    assert skipped == 90, f"expected 90 skipped, got {skipped}"
    print(f"[PASS] skip-existing: injected={inj} skipped={skipped}")


if __name__ == "__main__":
    test_parse_counts()
    test_inject_roundtrip()
    test_inject_skips_existing()
    print("\nALL TESTS PASSED")
