# Gemini 네이티브 오디오 분석 전환 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-23

## Why

현재 면접 분석 파이프라인은 Whisper STT(음성→텍스트) + GPT-4o LLM(텍스트→언어 평가)의 2단계 구조다. 이 구조에는 근본적 한계가 있다:

1. **음성 특성 소실**: Whisper가 텍스트로 변환하는 순간 톤, 감정, 말 빠르기 변화, 전략적 멈춤 vs 당황 멈춤 등의 정보가 사라진다. GPT-4o는 텍스트만 보고 평가하므로 "어떻게 말했는지"를 판단할 수 없다.
2. **2단계 비용**: Whisper($0.006/분) + GPT-4o 언어 평가(~$0.002/건)를 각각 호출해야 한다.
3. **파이프라인 복잡도**: STT → LLM 2단계 순차 호출, 25MB 청크 분할 로직, 타임스탬프 오프셋 보정 등의 복잡도가 있다.

Gemini 2.5 Flash는 오디오를 텍스트로 변환하지 않고 네이티브로 처리하므로, 1회 호출로 전사(transcript) + 언어 평가 + 음성 특성 분석을 동시에 수행할 수 있다.

## Goal

- Whisper STT + GPT-4o 언어 평가를 Gemini 2.5 Flash 네이티브 오디오 분석으로 교체
- 기존에 없던 **음성 특성 피드백** 추가 (톤 자신감, 감정, 말 빠르기, 필러워드 목록, 멈춤 패턴)
- GPT-4o Vision 비언어 분석(시선/표정/자세)은 그대로 유지
- API 비용 ~43% 절감
- 파이프라인 단순화 (STT+LLM 2단계 → Gemini 1단계)

### 성공 기준

- Gemini transcript 품질이 Whisper 대비 동등 이상 (한국어 전사 정확도)
- transcript가 요약/생략 없이 전체 전사 (할루시네이션 없음)
- 음성 특성 피드백(toneConfidence, emotionLabel, speechPace) 일관성 확보
- 총 분석 시간이 기존 대비 동등 이하

## Evidence

- Gemini 2.5 Flash는 오디오를 네이티브 멀티모달로 처리 (텍스트 변환 없이 직접 분석)
- Google AI Studio에서 오디오 파일 업로드 → 분석 가능 확인
- Gemini File API로 오디오 업로드 후 프롬프트와 함께 전송하는 패턴 지원

## Trade-offs

| 포기하는 것 | 얻는 것 |
|------------|---------|
| Whisper의 검증된 STT 정확도 | Gemini 네이티브 오디오 분석 (음성 특성 포함) |
| 기존 안정적 파이프라인 | 비용 절감 + 단순화 |
| OpenAI 단일 벤더 | OpenAI(Vision) + Google(Audio) 멀티 벤더 |

**고려한 대안:**
- OpenAI GPT-4o Audio: 네이티브 오디오 지원하지만 비용이 Gemini 대비 높음
- Claude Audio: 2026년 3월 기준 오디오 네이티브 미지원
- 기존 유지 + 음성 분석 별도 추가: 복잡도 증가, 비용 절감 없음

## 아키텍처 / 설계

### 현재 파이프라인 (As-Is)

```
S3 영상 다운로드
    ↓
FFmpeg 추출
├── 전체 오디오 WAV 1개
└── 답변 구간별 프레임 JPEG (3초 간격)
    ↓
순차 실행 (동기식):
  1. Whisper STT (전체 WAV → 세그먼트 타임스탬프)
  2. 답변별 루프:
     ├── 타임스탬프 기반 transcript 텍스트 분리
     ├── Vision API (답변 구간 프레임 → 비언어 분석)
     └── GPT-4o LLM (transcript → 언어 평가)
  3. 종합 점수 계산 (가중 평균)
    ↓
API 서버에 피드백 저장
```

### 변경 후 파이프라인 (To-Be)

```
S3 영상 다운로드
    ↓
FFmpeg 추출
├── 답변 구간별 오디오 MP3 (startMs~endMs 기반, 최대 3개)
└── 답변 구간별 프레임 JPEG (3초 간격, 기존 유지)
    ↓
병렬 실행 (asyncio 또는 ThreadPoolExecutor):
  ├── Gemini 음성 분석 × N (오디오 → transcript + 언어 평가 + 음성 특성)
  └── Vision API × N (프레임 → 비언어 분석, 기존 유지)
    ↓
종합 평가 (Gemini 1회 호출)
    ↓
API 서버에 피드백 저장
```

### Lambda 트리거 단위 (중요)

- Lambda는 **질문세트(QuestionSet) 단위**로 트리거됨
- S3 key: `videos/{interviewId}/qs_{questionSetId}.webm`
- **1회 Lambda 실행 = 1개 질문세트 = 최대 3답변 (MAIN 1 + FOLLOWUP 최대 2)**
- 30분 면접 전체가 아니라, 질문세트 1개의 영상만 처리

### AI 호출 변경 요약

| 단계 | As-Is | To-Be |
|------|-------|-------|
| 전사(STT) | Whisper API × 1 (전체 WAV) | **Gemini × N (답변별, 전사 통합)** |
| 언어 평가 | GPT-4o LLM × N (답변별) | **Gemini에 통합 (1회 호출에 전사+평가)** |
| 비언어 분석 | GPT-4o Vision × N (답변별) | **GPT-4o Vision × N (유지)** |
| 종합 평가 | 가중 평균 계산 (코드) | **Gemini LLM 1회 호출** |

## Scope

### In

- Lambda: Whisper + GPT-4o 언어 평가 → Gemini 교체
- Lambda: FFmpeg 답변 구간별 mp3 추출 기능 추가
- Lambda: 동기식 → 병렬 실행 (Gemini 음성 + Vision 비언어)
- Lambda: 종합 평가를 Gemini LLM 호출로 교체
- Backend: 피드백 엔티티/DTO에 음성 특성 필드 추가
- Backend: DB 마이그레이션
- Backend: AnalysisProgress 4단계로 변경
- Frontend: 피드백 뷰어에 음성 특성 영역 추가
- Frontend: 프로그레스 UI 4단계 반영

### Out

- GPT-4o Vision 비언어 분석 변경 (유지)
- Convert Lambda (MediaConvert) 변경
- 면접 생성/질문 생성 로직 변경
- 프론트엔드 녹화/업로드 로직 변경

## 제약조건 / 환경

### Lambda 환경
- Python 3.12, /tmp 512MB, 타임아웃 15분 (900초)
- 현재 Layer: ffmpeg-static:1, rehearse-analysis-deps:v2
- **추가 필요**: `google-generativeai` 패키지 Layer

### 환경변수
- 유지: `OPENAI_API_KEY` (Vision용), `FFMPEG_PATH`, `FFPROBE_PATH`, `S3_BUCKET`, `API_SERVER_URL`, `INTERNAL_API_KEY`
- 추가: `GEMINI_API_KEY`, `GEMINI_MODEL` (gemini-2.5-flash)

### 기존 코드 보존
- Whisper (`stt_analyzer.py`) + GPT-4o 언어 평가 (`verbal_analyzer.py`) 코드는 **삭제하지 않고 폴백용으로 유지**
- Gemini 전체 장애 시 기존 파이프라인으로 폴백 가능해야 함

### 피드백 스키마 하위 호환
- 기존 필드 (`verbalScore`, `verbalComment`, `fillerWordCount`, 비언어 4개)는 유지
- 신규 필드는 nullable로 추가 (기존 데이터 영향 없음)

## 미결정 사항

다음 항목은 구현 전 결정 필요:

1. **fillerWordCount(int) + fillerWords(string[])**: 둘 다 유지? fillerWords만으로 대체?
2. **PARTIAL_COMPLETED 상태**: 음성 성공 + 비언어 실패 시 별도 상태? 기존 COMPLETED에 포함?
3. **Gemini 폴백 트리거**: config flag (`USE_GEMINI=true/false`)? try-except 자동 폴백?
4. **asyncio vs ThreadPoolExecutor**: Lambda에서 asyncio 안정성 검증 필요
5. **Gemini File API rate limit**: Tier 1 기준 동시 업로드 가능 수 확인 필요
