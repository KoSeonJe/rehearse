# Rehearse

> AI 기반 개발자 모의면접 플랫폼 — 타임스탬프 동기화 영상 피드백

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=flat&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?style=flat&logo=typescript&logoColor=white)

---

## Why Rehearse?

기존 AI 면접 서비스는 면접이 끝나면 텍스트 점수표만 제공합니다.
"표정이 불안했습니다" 같은 피드백을 받아도, **어느 시점에서 그랬는지** 확인할 방법이 없습니다.

Rehearse는 녹화된 면접 영상 위에 피드백을 타임스탬프로 연결합니다.
피드백을 클릭하면 해당 장면으로 이동하여, 자신의 답변과 표정을 직접 확인하며 복기할 수 있습니다.

|  | 기존 서비스 | Rehearse |
|--|-----------|----------|
| 피드백 | 면접 종료 후 텍스트 점수표 | **타임스탬프 기반 영상+피드백 동기화** |
| 비언어 분석 | 없음 또는 추상적 점수 | **GPT-4o Vision 기반 시선·표정·자세 분석 (Lambda 후처리)** |
| 개발자 특화 | 범용 BQ 중심 | **이력서 PDF 기반 CS·시스템 설계·행동 면접** |
| 후속 질문 | 고정 질문 목록 | **답변 맥락 기반 AI 실시간 생성** |

---

## 사용자 플로우

### 1. 면접 설정

<!-- ![면접 설정](docs/images/setup.png) -->

4단계 위저드로 면접을 구성합니다. 직무·레벨·시간을 선택하고, 이력서 PDF를 업로드하면 AI가 맞춤 질문을 생성합니다.

- 5개 직무 (Backend, Frontend, DevOps, Data Engineer, Fullstack)
- 3개 레벨 (Junior, Mid, Senior)
- CS 기초 선택 시 세부 주제 토글 (자료구조, OS, 네트워크, DB)
- 이력서 기반 면접 선택 시 PDF 드래그앤드롭 업로드

### 2. AI 면접 진행

<!-- ![면접 진행](docs/images/conduct.png) -->

AI 면접관이 음성으로 질문하고, 사용자의 답변을 실시간 인식합니다. 답변 내용에 따라 심화·보충·반론 후속 질문을 즉석에서 생성합니다. 질문 세트별로 영상을 녹화하여 S3에 업로드합니다.

### 3. 타임스탬프 피드백 리뷰

<!-- ![피드백 리뷰](docs/images/review.png) -->

질문 세트 녹화가 완료되면 Lambda가 자동으로 분석을 시작합니다. OpenAI Whisper로 STT를 수행하고, GPT-4o Vision으로 시선·표정·자세를 분석하여 타임스탬프별 피드백을 생성합니다.

영상 플레이어와 피드백 패널이 동기화됩니다. 타임라인의 마커나 피드백 카드를 클릭하면 해당 장면으로 바로 이동합니다.

### 4. 종합 리포트

<!-- ![종합 리포트](docs/images/report.png) -->

100점 만점의 종합 점수와 함께 강점·보완점을 요약합니다. 리포트에서 바로 피드백 리뷰 페이지로 이동하여 영상을 보며 정밀 교정할 수 있습니다.
