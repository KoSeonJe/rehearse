# Lambda — Rehearse Analysis & Convert

> 이 파일은 `lambda/` 하위 파일 작업 시 자동 로드된다. 루트 `CLAUDE.md`와 함께 적용.

## 구조

```
lambda/
├── analysis/   # 영상/음성 분석 — Gemini 주력 + OpenAI Vision(프레임) + Whisper(STT fallback)
└── convert/    # MediaConvert 트리거
```

- Runtime: Python 3.12
- 실행 환경: AWS Lambda (Docker 이미지 기반, ECR 저장)
- 트리거: S3 이벤트 → EventBridge → Lambda (Backend는 Lambda를 직접 invoke하지 않음)

## AI 스택 (Backend와 다름!)

| 용도 | 모델 | 비고 |
|------|------|------|
| Audio 통합 분석 (주력) | **Google Gemini** | 음성+텍스트 통합 |
| 프레임 분석 (비언어) | OpenAI **GPT-4o Vision** | 얼굴/자세 분석 |
| STT (fallback 경로) | OpenAI **Whisper** | Gemini 경로 실패 시 |

> Backend는 GPT-4o-mini + Claude fallback. Lambda와 **프로바이더 스택이 다름**을 의식할 것. 공용 프롬프트 빌더 아님.

## 배포

자체 deploy 스크립트 사용 (GitHub Actions workflow 없음):

```bash
cd lambda
./deploy.sh                   # 단순 배포
./lambda-safe-deploy.sh       # 버전 발행 → smoke test → alias 전환 (권장)
```

- Safe deploy 스크립트: `update-function-code` → `publish-version` → smoke test → `update-alias` (실패 시 자동 롤백)
- `/lambda-deploy` 스킬 트리거 가능 — 람다 배포 요청 시 반드시 safe deploy 경로 사용

## 참조 문서

- `docs/architecture/lambda-deployment.md` — 배포 상세, IAM, 환경변수
- `docs/architecture/system-flow.md` — S3 → EventBridge → Lambda → S3 결과 쓰기 플로우
- `docs/plans/` 내 prompt-redesign, prompt-improvement-2026-04 — 분석 프롬프트 개편 이력

## 작업 규칙

- **경로 변경 금지** — `lambda/` 위치 이동은 9개 이상 docs/architecture 문서 + deploy 스크립트 + plan 경로 갱신 필요. 별도 플랜으로만.
- **Backend Java 코드와 import 공유 없음** — 완전 독립. EventBridge로만 소통.
- **로컬 테스트**: Docker 이미지 빌드 후 AWS SAM 또는 컨테이너 직접 실행. 프로덕션 배포 전 smoke test 필수.
- **Spec 없는 수정 금지** — 프롬프트/로직 변경은 `.omc/plans/` 또는 `docs/plans/`에 spec 선행.

## 에이전트 호출 시

Lambda는 전담 커스텀 에이전트가 없으므로 일반 `executor` (sonnet) 또는 `python` 지식이 필요한 범용 에이전트 사용. `deployment-engineer` / `devops-engineer`는 배포 파이프라인 작업에 활용.
