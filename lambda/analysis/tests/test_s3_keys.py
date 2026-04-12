import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest
from s3_keys import parse_raw_key, derive_mp4_key, derive_feedback_key


class TestParseRawKey:
    def test_v1_format(self):
        key = "interviews/raw/2026/04/12/123/456/9c8b8a2d3f1e.webm"
        parsed = parse_raw_key(key)
        assert parsed.interview_id == 123
        assert parsed.question_set_id == 456
        assert parsed.uuid == "9c8b8a2d3f1e"
        assert parsed.raw_key == key

    def test_legacy_videos_prefix_returns_none(self):
        assert parse_raw_key("videos/123/qs_456.webm") is None

    def test_legacy_analysis_backup_returns_none(self):
        assert parse_raw_key("analysis-backup/123/qs_456.json") is None

    def test_mp4_key_rejected(self):
        assert parse_raw_key("interviews/mp4/2026/04/12/1/1/abc123def456.mp4") is None

    def test_feedback_key_rejected(self):
        assert parse_raw_key("interviews/feedback/2026/04/12/1/1/abc123def456.json") is None

    def test_garbage_key_rejected(self):
        assert parse_raw_key("random/garbage.txt") is None

    def test_invalid_date_rejected(self):
        assert parse_raw_key("interviews/raw/abcd/04/12/1/1/abc123def456.webm") is None

    def test_short_uuid_rejected(self):
        assert parse_raw_key("interviews/raw/2026/04/12/1/1/abc.webm") is None

    def test_uppercase_uuid_rejected(self):
        assert parse_raw_key("interviews/raw/2026/04/12/1/1/ABC123DEF456.webm") is None


class TestDeriveMp4Key:
    def test_v1_format(self):
        parsed = parse_raw_key("interviews/raw/2026/04/12/123/456/abc123def456.webm")
        assert derive_mp4_key(parsed) == "interviews/mp4/2026/04/12/123/456/abc123def456.mp4"


class TestDeriveFeedbackKey:
    def test_v1_format(self):
        parsed = parse_raw_key("interviews/raw/2026/04/12/123/456/abc123def456.webm")
        assert derive_feedback_key(parsed) == "interviews/feedback/2026/04/12/123/456/abc123def456.json"
