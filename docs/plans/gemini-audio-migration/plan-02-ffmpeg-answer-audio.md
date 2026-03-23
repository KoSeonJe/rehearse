# Plan 02: FFmpeg 답변 구간별 오디오 추출

> 상태: Draft
> 작성일: 2026-03-23

## Why

현재 `extract_audio()`는 전체 영상에서 WAV 1개를 추출한다. Gemini에 답변별 오디오를 전송하려면 각 답변의 `startMs~endMs` 구간만 잘라서 mp3로 추출해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/extractors/ffmpeg_extractor.py` | `extract_answer_audios()` 함수 추가 |

## 상세

### 함수 스펙

```python
def extract_answer_audios(
    video_path: str,
    answers: list[dict],  # [{"startMs": 0, "endMs": 15101}, ...]
    output_dir: str,
) -> list[str]:
    """답변 구간별 오디오를 mp3로 추출.

    Returns:
        list of mp3 file paths (same order as answers)
    """
```

### FFmpeg 명령

```bash
ffmpeg -i video.webm -ss {startMs/1000} -t {(endMs-startMs)/1000} \
  -vn -acodec libmp3lame -ar 16000 -ac 1 -q:a 5 \
  -y answer_0.mp3
```

- mp3 사용 이유: WAV 대비 ~1/10 크기 → Gemini File API 업로드 속도 개선, /tmp 절약
- `-q:a 5`: 중간 품질 (음성 분석에 충분)
- 16kHz mono: 음성 인식에 최적

### /tmp 용량 계산

- Lambda 1회 = 최대 3답변
- 답변당 1분 mp3 ~500KB
- 3개 합계 ~1.5MB → 512MB 제한 대비 무시 가능

## 담당 에이전트

- Implement: `executor` — FFmpeg 명령 구현
- Review: `code-reviewer` — 에러 처리, 타임아웃

## 검증

- 테스트 영상에서 startMs/endMs 구간 정확히 추출되는지 확인
- 추출된 mp3 재생 시 해당 구간 답변만 포함되는지
- `progress.md` 상태 업데이트 (Task 2 → Completed)
