"""S3 키 파싱·유도 유틸. docs/architecture/s3-key-schema.md v1.0 규격 구현."""
import re
from dataclasses import dataclass
from typing import Optional

RAW_KEY_PATTERN = re.compile(
    r"^interviews/raw/"
    r"(?P<year>\d{4})/(?P<month>\d{2})/(?P<day>\d{2})/"
    r"(?P<interview_id>\d+)/(?P<qs_id>\d+)/"
    r"(?P<uuid>[a-f0-9]{12})\.webm$"
)


@dataclass(frozen=True)
class ParsedKey:
    interview_id: int
    question_set_id: int
    uuid: str
    raw_key: str


def parse_raw_key(key: str) -> Optional[ParsedKey]:
    m = RAW_KEY_PATTERN.match(key)
    if not m:
        return None
    return ParsedKey(
        interview_id=int(m.group("interview_id")),
        question_set_id=int(m.group("qs_id")),
        uuid=m.group("uuid"),
        raw_key=key,
    )


def derive_mp4_key(parsed: ParsedKey) -> str:
    return (
        parsed.raw_key.replace("interviews/raw/", "interviews/mp4/", 1)
        .rsplit(".", 1)[0]
        + ".mp4"
    )


def derive_feedback_key(parsed: ParsedKey) -> str:
    return (
        parsed.raw_key.replace("interviews/raw/", "interviews/feedback/", 1)
        .rsplit(".", 1)[0]
        + ".json"
    )
