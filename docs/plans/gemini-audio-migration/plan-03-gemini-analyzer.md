# Plan 03: Gemini 오디오 분석기 구현

> 상태: Draft
> 작성일: 2026-03-23

## Why

Whisper(STT) + GPT-4o(언어 평가) 2단계를 Gemini 2.5 Flash 1회 호출로 통합한다. 추가로 Whisper에서 불가능했던 음성 특성 분석(톤, 감정, 말빠르기, 멈춤 패턴)도 수행한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/gemini_analyzer.py` | **신규** — Gemini 오디오 분석 + 종합 평가 |
| `lambda/analysis/analyzers/verbal_prompt_factory.py` | Gemini용 맥락 생성 함수 추가 |

## 상세

### analyze_answer_audio()

답변 오디오 1개를 Gemini에 전송하여 transcript + 언어 평가 + 음성 특성을 1회 호출로 분석.

**입력**: audio_path (mp3), question_text, position_context (verbal_prompt_factory에서 생성)
**출력**:
```python
{
    "transcript": "전체 전사 텍스트",
    "verbal": {
        "score": 0-100,
        "comment": "언어 피드백 2-3문장"
    },
    "vocal": {
        "fillerWordCount": 3,
        "fillerWords": ["음", "어"],
        "speechPace": "적절",
        "toneConfidence": 75,
        "emotionLabel": "자신감",
        "comment": "음성 특성 피드백 2-3문장"
    },
    "overallComment": "답변 종합 피드백 3-4문장"
}
```

**Gemini File API 패턴**:
```python
audio_file = genai.upload_file(audio_path, mime_type="audio/mpeg")
try:
    response = model.generate_content([audio_file, prompt], ...)
finally:
    genai.delete_file(audio_file.name)  # 반드시 삭제
```

**프롬프트에 포함할 맥락** (verbal_prompt_factory 통합):
- position (BACKEND, FRONTEND 등)
- techStack (Java/Spring, React 등)
- level (JUNIOR, SENIOR 등)
- 해당 직무의 기술 키워드 사전

### generate_overall_report()

음성 분석 + 비언어 분석 결과를 종합하여 최종 리포트 생성. 기존 `_compute_overall()`의 가중 평균 계산을 Gemini LLM 호출로 대체.

**입력**: audio_results (list), nonverbal_results (list), questions (list)
**출력**:
```python
{
    "overallScore": 72,
    "overallComment": "종합 평가 5-6문장",
    "verbalSummary": "언어 역량 종합",
    "vocalSummary": "음성 전달력 종합",
    "nonverbalSummary": "비언어 역량 종합",
    "strengths": ["강점1", "강점2", "강점3"],
    "improvements": ["개선점1", "개선점2", "개선점3"],
    "topPriorityAdvice": "가장 중요한 개선 조언"
}
```

### 에러 처리

- 3회 재시도 (exponential backoff: 1초, 3초, 9초)
- JSON 파싱 실패 시 1회 재요청
- 전체 실패 시 폴백값 반환
- File API 업로드 파일은 finally에서 반드시 삭제

## 담당 에이전트

- Implement: `executor` — Gemini API 연동, 프롬프트 설계
- Review: `architect-reviewer` — API 패턴, 에러 처리 구조
- Review: `code-reviewer` — 보안 (API 키), 리소스 관리 (File API 삭제)

## 검증

- 1분 한국어 오디오 → transcript 품질 확인 (Whisper 대비)
- transcript가 요약/생략 없이 전체 전사되는지
- 필러워드 감지 정확도
- toneConfidence, emotionLabel 일관성
- File API 업로드 후 예외 발생 시에도 파일 삭제 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
