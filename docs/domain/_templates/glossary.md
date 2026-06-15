# {도메인명} 용어집

> 한글 ↔ 영문 / 코드 식별자 / DB 컬럼 매핑. 모호한 용어만 등록.

| 용어 (한) | 영문 / 코드 식별자 | 정의 | 참고 |
|---------|----------------|------|------|
| 세션 | `Session` / `interview_session` | 인터뷰 1회 단위 | `schema.md#interview_session` |
| 턴 | `Turn` / `interview_turn` | 질문 1개 + 답변 1개 단위 | `schema.md#interview_turn` |
| 꼬리질문 | `FollowUp` / intent=`FOLLOW_UP` | 직전 답변 모호도 ≥ 임계 시 발동 | `api/submit-answer.md` |
| ... | ... | ... | ... |

## 약어

| 약어 | 풀이 | 비고 |
|------|------|------|
| STT | Speech-to-Text | Whisper / Web Speech API |
| ... | ... | ... |
