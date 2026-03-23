import math
import os
import subprocess
from pathlib import Path

from config import Config


def extract_audio(video_path: str, output_dir: str) -> str:
    audio_path = os.path.join(output_dir, "audio.wav")
    cmd = [
        Config.FFMPEG_PATH, "-i", video_path,
        "-vn", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
        "-y", audio_path,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if result.returncode != 0:
        raise RuntimeError(f"FFmpeg 오디오 추출 실패: {result.stderr[:500]}")
    print(f"[FFmpeg] 오디오 추출 완료: {audio_path}")
    return audio_path


def extract_answer_audios(
    video_path: str,
    answers: list[dict],  # [{"startMs": 0, "endMs": 15101}, ...]
    output_dir: str,
) -> list[str]:
    """답변 구간별 오디오를 mp3로 추출.

    Returns:
        list of mp3 file paths (same order as answers)
    """
    os.makedirs(output_dir, exist_ok=True)
    paths: list[str] = []
    for i, answer in enumerate(answers):
        start_sec = answer["startMs"] / 1000
        duration_sec = (answer["endMs"] - answer["startMs"]) / 1000
        output_path = os.path.join(output_dir, f"answer_{i}.mp3")
        cmd = [
            Config.FFMPEG_PATH, "-i", video_path,
            "-ss", str(start_sec),
            "-t", str(duration_sec),
            "-vn", "-acodec", "libmp3lame", "-ar", "16000", "-ac", "1", "-q:a", "5",
            "-y", output_path,
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if result.returncode != 0:
            raise RuntimeError(f"FFmpeg 답변 오디오 추출 실패 (answer {i}): {result.stderr[:500]}")
        print(f"[FFmpeg] 답변 오디오 추출 완료: {output_path}")
        paths.append(output_path)
    return paths


def extract_frames(video_path: str, output_dir: str) -> list[str]:
    frames_dir = os.path.join(output_dir, "frames")
    os.makedirs(frames_dir, exist_ok=True)

    interval = Config.FRAME_INTERVAL_SEC
    cmd = [
        Config.FFMPEG_PATH, "-i", video_path,
        "-vf", f"fps=1/{interval}",
        "-q:v", "2",
        "-y", os.path.join(frames_dir, "frame_%04d.jpg"),
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if result.returncode != 0:
        raise RuntimeError(f"FFmpeg 프레임 추출 실패: {result.stderr[:500]}")

    frame_files = sorted(Path(frames_dir).glob("frame_*.jpg"))
    print(f"[FFmpeg] 프레임 추출 완료: {len(frame_files)}장")
    return [str(f) for f in frame_files]


def get_video_duration_ms(video_path: str) -> int:
    cmd = [
        Config.FFPROBE_PATH, "-v", "quiet",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        video_path,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        raise RuntimeError(f"FFprobe 실패: {result.stderr[:500]}")

    raw = result.stdout.strip()
    if not raw or raw == "N/A":
        # WebM 컨테이너에 duration 메타데이터 없는 경우 — 스트림 레벨에서 재시도
        cmd_stream = [
            Config.FFPROBE_PATH, "-v", "quiet",
            "-show_entries", "stream=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            video_path,
        ]
        result2 = subprocess.run(cmd_stream, capture_output=True, text=True, timeout=30)
        raw = result2.stdout.strip().split("\n")[0]

    if not raw or raw == "N/A":
        # WebM (MediaRecorder)은 duration 메타데이터가 없는 경우가 많음
        # ffmpeg null decode 후 마지막 time= 값으로 duration 추정
        import re
        cmd_decode = [
            Config.FFMPEG_PATH, "-i", video_path,
            "-f", "null", "-",
        ]
        result3 = subprocess.run(cmd_decode, capture_output=True, text=True, timeout=300)
        # ffmpeg stderr 끝부분에 "time=00:01:23.45" 형식으로 최종 시간 출력
        time_matches = re.findall(r"time=(\d{2}):(\d{2}):(\d{2}\.\d+)", result3.stderr)
        if time_matches:
            last = time_matches[-1]
            hours, minutes, seconds = int(last[0]), int(last[1]), float(last[2])
            raw = str(hours * 3600 + minutes * 60 + seconds)

    if not raw or raw == "N/A":
        raise RuntimeError(f"FFprobe: 영상 duration을 확인할 수 없습니다 (output: {result.stdout.strip()!r})")

    return int(float(raw) * 1000)


def split_audio_chunks(audio_path: str, output_dir: str) -> list[tuple[str, int]]:
    """오디오 파일을 Whisper API 제한(25MB) 이하의 청크로 분할.

    파일이 25MB 이하면 분할 없이 원본 반환.
    각 청크는 10분(600초) + 2초 오버랩.

    Returns:
        list of (chunk_file_path, offset_ms) tuples
    """
    max_bytes = Config.WHISPER_MAX_FILE_SIZE_MB * 1024 * 1024
    if os.path.getsize(audio_path) <= max_bytes:
        return [(audio_path, 0)]

    chunk_duration = Config.WHISPER_CHUNK_DURATION_SEC
    overlap = Config.WHISPER_CHUNK_OVERLAP_SEC
    stride = chunk_duration - overlap

    # 16kHz mono 16-bit PCM: 32000 bytes/sec
    file_size = os.path.getsize(audio_path)
    total_duration_sec = file_size / 32000

    num_chunks = max(1, math.ceil((total_duration_sec - overlap) / stride))

    chunks: list[tuple[str, int]] = []
    for i in range(num_chunks):
        start_sec = i * stride
        chunk_path = os.path.join(output_dir, f"chunk_{i:04d}.wav")
        cmd = [
            Config.FFMPEG_PATH, "-i", audio_path,
            "-ss", str(start_sec),
            "-t", str(chunk_duration),
            "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
            "-y", chunk_path,
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if result.returncode != 0:
            raise RuntimeError(f"FFmpeg 청크 분할 실패 (chunk {i}): {result.stderr[:500]}")

        offset_ms = i * stride * 1000
        chunks.append((chunk_path, offset_ms))
        print(f"[FFmpeg] 청크 생성 완료: {chunk_path} (offset={offset_ms}ms)")

    return chunks
