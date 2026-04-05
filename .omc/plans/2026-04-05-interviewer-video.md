# Interviewer Video 전환 기능

**Status**: In Progress

## Why

기존 `InterviewerAvatar`는 SVG 애니메이션으로 구현되어 있어 AI 면접관으로서의 현실감이 부족함.
사전 제작된 영상을 면접 상태(질문/경청/생각)에 따라 전환해 몰입감 있는 면접 경험을 제공한다.

## Goal

- AI가 질문하는 순간 → 질문 영상 송출
- 사용자가 답변하는 순간 → 경청 영상 송출
- 꼬리질문 생성 중 → 생각 영상 송출
- 아키텍처 변경 최소화 (컴포넌트 교체 수준)

## 구현 범위

- `interviewer-video.tsx` 신규 생성 (기존 `interviewer-avatar.tsx` 대체)
- `interview-page.tsx` 에서 `InterviewerAvatar` → `InterviewerVideo` 교체 (1줄)
- 영상 URL: `https://dev.rehearse.co.kr/assets/interviewer/{mood}.mp4`

## mood → 영상 매핑

| mood | 상태 | 영상 |
|---|---|---|
| `speaking` | AI 질문 중 (TTS) | `questioning.mp4` |
| `listening` | 사용자 답변 중 | `listening.mp4` |
| `thinking` | 꼬리질문 생성 중 | `thinking.mp4` |
| `neutral` | 대기 | `listening.mp4` (재사용) |

## 기술 결정

- HTML5 `<video loop autoPlay muted playsInline>`
- mood 변경 시 `useEffect` → `video.load()` + `video.play()`
- `useRef`로 video 엘리먼트 직접 제어
- 기존 ring/glow 테두리 애니메이션 유지