# 면접 준비 페이지 리디자인

- **Status**: Completed
- **Date**: 2026-03-10

## Why

Setup 위저드에서 선택한 정보(직무, CS 상세 토픽 등)가 준비 페이지에 제대로 표시되지 않고, 질문 목록이 불필요하게 노출되며, 디바이스 테스트가 접힌 채 부차적으로 취급되고 있었다. 준비 페이지의 본질은 **마이크/스피커/카메라가 잘 동작하는지 확인**하는 것이므로, 이를 메인 콘텐츠로 재구성했다.

## 변경 사항

### 1. 레이아웃 변경 — 디바이스 테스트가 메인
- 질문 목록 섹션 제거
- "질문 다시 생성하기" 버튼 제거
- 디바이스 테스트 접기/펼치기 토글 제거 (항상 표시)

### 2. 인트로 텍스트 + 태그 한 줄 정리
- 태그를 한 줄로 정렬 (overflow-x-auto 가로 스크롤)
- BE에서 `csSubTopics` 필드를 InterviewResponse에 추가하여 FE에서 태그로 표시

### 3. 디바이스 테스트 UX 개선
- 카메라/마이크/스피커 3개 수동 테스트 카드
- 각 항목: idle(회색) → testing(노란) → passed(초록 체크) / denied(빨간 X) 4단계
- 마이크: 음성 레벨 감지 시 자동 passed
- 스피커: 440Hz 테스트 사운드 재생 → "소리가 들려요" 확인 클릭
- 3개 모두 passed여야 "면접 시작" 버튼 활성화

## 수정 파일

### Backend
| 파일 | 변경 |
|------|------|
| `Interview.java` | `csSubTopics` 컬럼 추가, Builder 파라미터, `getCsSubTopicList()` |
| `InterviewResponse.java` | `csSubTopics` 필드 + `from()` 매핑 |
| `InterviewService.java` | `createInterview`에서 `csSubTopics` 전달 |

### Frontend
| 파일 | 변경 |
|------|------|
| `types/device.ts` | `DeviceTestStatus`, `DeviceTestState` 타입 재정의 |
| `types/interview.ts` | `InterviewSession`에 `csSubTopics?` 추가 |
| `hooks/use-device-test.ts` | 자동→수동 트리거, 스피커 테스트 추가, `allPassed` |
| `components/interview/device-test-section.tsx` | 3개 테스트 카드 UI |
| `pages/interview-ready-page.tsx` | 레이아웃 재구성 |

## 검증
- `npx tsc --noEmit` 통과
- `./gradlew compileJava` BUILD SUCCESSFUL
