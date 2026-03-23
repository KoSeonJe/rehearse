# Plan 01: Gemini SDK Lambda Layer 빌드 및 환경변수 설정

> 상태: Draft
> 작성일: 2026-03-23

## Why

Gemini 네이티브 오디오 분석을 위해 `google-generativeai` Python 패키지가 Lambda 런타임에 필요하다. 현재 Lambda Layer(`rehearse-analysis-deps:v2`)에는 openai, boto3, httpx만 포함되어 있다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| Lambda Layer | `google-generativeai>=0.8.0` 포함 Layer 빌드 (Python 3.12 cp312) |
| Lambda 환경변수 | `GEMINI_API_KEY`, `GEMINI_MODEL=gemini-2.5-flash` 추가 |
| `lambda/analysis/config.py` | GEMINI_API_KEY, GEMINI_MODEL 환경변수 읽기 추가 |

## 상세

### Layer 빌드

기존 `rehearse-analysis-deps:v2` Layer에 `google-generativeai`를 추가하거나, 별도 Layer로 분리.

```bash
pip install google-generativeai -t python/ --platform manylinux2014_x86_64 --only-binary=:all: --python-version 3.12
zip -r gemini-layer.zip python/
aws lambda publish-layer-version --layer-name rehearse-gemini-deps --zip-file fileb://gemini-layer.zip --compatible-runtimes python3.12
```

### config.py 변경

```python
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
```

## 담당 에이전트

- Implement: `devops-engineer` — Layer 빌드, 환경변수 설정
- Review: `code-reviewer` — config.py 변경 검증

## 검증

- Lambda에 Layer 연결 후 `import google.generativeai` 성공 확인 (smoke test)
- 환경변수 `GEMINI_API_KEY` 설정 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
