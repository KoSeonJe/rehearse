# Commit Rule

## Format

Conventional Commits.

```
type(scope): subject
```

- **type**: `feat | fix | refactor | test | docs | chore | style | perf | build | ci`
- **scope**: 필수. `BE | FE | infra | eval | lambda | docs` 등 영역 명시.
- **subject**: 한국어, 명령형, 50자 이내.
- **body**: 작성 안 함 (필요 시에만 1-2줄).

## 커밋 단위

- 논리적 작업 단위 1개 = 커밋 1개.
- 여러 관심사 한 커밋에 섞지 말 것 (feat + refactor + test 동시 X).
- 리뷰 가능한 크기 유지.
- 빌드/테스트 통과 상태로 커밋.

## 금지

- `--no-verify` 훅 스킵.
- push 후 `--amend`.
- secrets/API 키 포함 커밋.

## 예시

```
feat(BE): Resume Project 도메인에 projectName 필드 추가
fix(FE): 인터뷰 타이머 race condition 해결
refactor(BE): runtimeState Map 키를 상수로 재사용
test(eval): Resume 트랙 fixture 3종 추가
```
