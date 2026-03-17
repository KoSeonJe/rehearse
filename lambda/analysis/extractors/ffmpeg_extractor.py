import os
import subprocess
from pathlib import Path

from config import Config


def extract_audio(video_path: str, output_dir: str) -> str:
    audio_path = os.path.join(output_dir, "audio.wav")
    cmd = [
        "ffmpeg", "-i", video_path,
        "-vn", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
        "-y", audio_path,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if result.returncode != 0:
        raise RuntimeError(f"FFmpeg 오디오 추출 실패: {result.stderr[:500]}")
    print(f"[FFmpeg] 오디오 추출 완료: {audio_path}")
    return audio_path


def extract_frames(video_path: str, output_dir: str) -> list[str]:
    frames_dir = os.path.join(output_dir, "frames")
    os.makedirs(frames_dir, exist_ok=True)

    interval = Config.FRAME_INTERVAL_SEC
    cmd = [
        "ffmpeg", "-i", video_path,
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
        "ffprobe", "-v", "quiet",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        video_path,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        raise RuntimeError(f"FFprobe 실패: {result.stderr[:500]}")
    return int(float(result.stdout.strip()) * 1000)
