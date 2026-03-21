# Plan 05: 노션 + 로컬 문서 보완

> 상태: Draft
> 작성일: 2026-03-20
> 우선순위: P2
> 태그: [parallel] (Phase A)

## Why

노션 "전체 서비스 로직" 문서와 로컬 `docs/` 문서에 삭제된 기능(VAD), 변경된 아키텍처(STT 전환, 순차 분석), 설명 부족(temperature, 폴링, 스케줄러) 등의 불일치가 남아있다. 문서와 코드 간 괴리는 신규 기여자 혼란과 잘못된 의사결정을 유발한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/architecture/system-flow.md` | 아래 6개 항목 반영 |
| 노션 "전체 서비스 로직" 페이지 | 동일 항목 반영 (Notion MCP) |

## 상세

### 수정 항목 (6개)

1. **VAD 표기 제거** (댓글 2)
   - 코드에서 이미 삭제된 VAD(Voice Activity Detection) 관련 설명 4곳 제거
   - 대상: system-flow.md + 노션

2. **Web Speech API STT 행 삭제** (댓글 3)
   - Whisper 전환 완료로 Web Speech API STT 설명 삭제
   - TTS 관련 내용은 유지
   - 대상: system-flow.md + 노션

3. **"병렬 분석" → "순차 분석"** (댓글 4)
   - Lambda에서 질문세트별 순차 분석으로 변경됨을 반영
   - 대상: system-flow.md + 노션

4. **temperature=0.9 설명 추가** (댓글 5)
   - Claude API 호출 시 temperature=0.9 사용 이유 설명 추가
   - "다양하고 자연스러운 후속 질문 생성을 위해 높은 temperature 사용"
   - 대상: system-flow.md + 노션

5. **폴링 전략별 이유 추가** (댓글 6)
   - 3곳의 폴링 패턴에 각각 왜 해당 전략을 사용하는지 설명 추가
   - 분석 상태 폴링, 리포트 상태 폴링, 파일 업로드 상태 폴링
   - 대상: system-flow.md + 노션

6. **스케줄러별 필요성 설명 추가** (댓글 11, 12)
   - 3개 스케줄러(좀비 정리, 타임아웃, 만료)의 존재 이유와 동작 조건 설명
   - 대상: system-flow.md + 노션

### 동기화 전략

- 로컬 `docs/architecture/system-flow.md` 먼저 수정
- 노션 페이지는 Notion MCP를 통해 동일 내용 반영
- 두 문서의 내용이 일치하도록 검증

## 담당 에이전트

- Implement: `oh-my-claudecode:writer` — 문서 작성 (로컬 + 노션)
- Review: `architect-reviewer` — 문서 내용이 현재 코드와 일치하는지 검증

## 검증

- system-flow.md에서 VAD 관련 표현이 없는지 Grep 확인
- Web Speech API STT 관련 행이 삭제되었는지 확인
- "병렬 분석" 표현이 "순차 분석"으로 변경되었는지 확인
- temperature, 폴링, 스케줄러 설명이 추가되었는지 확인
- 노션 페이지에도 동일 내용이 반영되었는지 확인
- `progress.md` 상태 업데이트 (Task 5 → Completed)
