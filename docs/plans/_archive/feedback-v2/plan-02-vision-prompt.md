# Plan 02: Vision 프롬프트에 노트북 시선 보정 추가

> 상태: Draft
> 작성일: 2026-04-01

## Why

현재 Vision 프롬프트는 "시선이 카메라를 향하지 않으면 부정 평가"하는데, 노트북/웹캠 면접에서는 화면을 보느라 시선이 카메라 아래를 향하는 것이 정상이다. 이로 인해 불필요한 부정 피드백이 생긴다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/vision_analyzer.py` | `_SYSTEM_PROMPT`의 평가 기준 + 주의사항 수정 |

## 상세

### 1. eyeContactLevel 평가 기준 변경

기존:
```
1. eyeContactLevel: GOOD(안정적 응시) | AVERAGE(간헐적 흐트러짐) | NEEDS_IMPROVEMENT(자주 딴 곳 응시/불안정)
```

변경:
```
1. eyeContactLevel: GOOD(화면/카메라 방향 응시) | AVERAGE(간헐적으로 다른 곳 응시) | NEEDS_IMPROVEMENT(자주 고개를 돌리거나 전혀 다른 방향 응시)
   ※ 노트북 화면을 보느라 시선이 약간 아래를 향하는 것은 GOOD으로 평가
```

### 2. 주의 섹션에 추가

기존 주의사항 뒤에 추가:
```
- 중요: 이것은 노트북/웹캠으로 진행하는 온라인 면접입니다. 면접자가 화면을 보느라 시선이 카메라 아래쪽을 향하는 것은 정상입니다. 시선이 아래로 향하는 것만으로는 부정적 평가를 하지 마세요. 고개를 완전히 돌리거나 완전히 다른 곳을 응시하는 경우에만 시선 문제로 판단하세요.
```

## 담당 에이전트

- Implement: `backend` — Lambda 프롬프트 수정
- Review: `code-reviewer` — 프롬프트 명확성

## 검증

- 노트북 면접 프레임(시선이 약간 아래)으로 Vision 분석 → eyeContactLevel이 GOOD 또는 AVERAGE인지 확인
- 고개를 완전히 돌린 프레임 → NEEDS_IMPROVEMENT가 나오는지 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
