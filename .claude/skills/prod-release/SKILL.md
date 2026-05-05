---
name: prod-release
description: develop → main 프로덕션 릴리즈 스킬. 버저닝(SemVer) 규칙 적용, 릴리즈 PR 템플릿 생성, 어노테이티드 태그 발행, GitHub Release 노트 생성, 배포 후 헬스체크. "/prod-release", "프로덕션 배포", "릴리즈", "release", "main 머지"에서 트리거.
---

# Production Release 스킬

`develop → main` 머지 시 안전하게 프로덕션 배포를 수행한다.
main에 push되면 `.github/workflows/deploy-prod.yml`이 자동 실행되므로, 이 스킬은 **머지 이전**의 버저닝·릴리즈 PR·태그·릴리즈 노트를 책임진다.

## 핵심 원칙

1. **main은 항상 배포 가능한 상태** — PR 없이 직접 push 금지
2. **태그는 기록이자 롤백 포인트** — 모든 릴리즈는 annotated tag + GitHub Release로 남긴다
3. **머지 전 검증, 머지 후 검증 분리** — CI 통과 ≠ 프로덕션 동작 (헬스체크로 확정)
4. **릴리즈 단위는 사용자 관점 변경** — 커밋 1개도 릴리즈 가능, 하지만 의미 있는 묶음 권장

---

## 버저닝 규칙 (이 프로젝트)

**SemVer 2.0 + 0.x 베타 페이즈 규약.** 제품이 베타인 동안은 `0.x.y`로 관리하고, 정식 공개 시점에 `v1.0.0`을 발행한다.

### 현재 상태 (2026-04-15 기준, v0.1.0 발행 후)

- 최신 태그: **v0.1.0** (첫 프로덕션 릴리즈, 2026-04-15)
- 기본 브랜치: `develop` (feature PR 기본 타겟). `main`은 프로덕션 배포 트리거 브랜치로만 사용.
  - paths-filter가 default branch에 의존하지 않도록 `deploy-prod.yml`에 `base: github.event.before || HEAD~1` 명시됨
- 앱 버전 파일: `frontend 0.1.0`, `backend 0.1.0`
- 제품 단계: **베타** (프론트 BETA 배지 표시 중)
- 이후 릴리즈부터는 정규 `develop → main` PR 플로우 적용

### 0.x 페이즈 증가 규칙

> **핵심: 0.x에서는 breaking change에 major를 올리지 않는다.** SemVer 사양상 0.x는 "public API 불안정" 상태로 정의되어 있어, 호환성 깨짐도 minor로 처리한다.

| 변경 유형 | 예시 | 버전 증가 |
|-----------|------|-----------|
| **Minor** (새 기능 / breaking change / 주요 변경) | 신규 페이지·API, API 계약 변경, DB 스키마 변경 | `0.1.0 → 0.2.0` |
| **Patch** (버그 수정, 문구·프롬프트 튜닝, 내부 리팩토링) | UI 버그, 프롬프트 미세 조정 | `0.1.0 → 0.1.1` |
| **Major (0 → 1)** — 정식 GA 선언 시 딱 한 번 | BETA 배지 제거, API 안정성 약속 | `0.x.y → 1.0.0` |

### 베타/프리릴리즈 필요 시 (선택)

내부 테스트 후 배포하고 싶을 때:
- 내부 테스트: `v0.2.0-beta.1`, `v0.2.0-beta.2`
- RC: `v0.2.0-rc.1`
- 정식: `v0.2.0`

**기본 정책**: suffix 없이 `vMAJOR.MINOR.PATCH`만 사용. 중대 변경을 사전 테스트할 때만 `-beta.N`/`-rc.N` 사용.

### v1.0.0 (GA) 발행 기준

아래 조건이 모두 충족되는 시점에 `v1.0.0`을 발행하고 BETA 배지를 제거한다:
- 핵심 사용자 플로우(면접 생성 → 녹화 → 피드백)가 프로덕션에서 안정적으로 동작
- 외부 의존 API 계약이 당분간 깨지지 않을 것으로 확신
- 롤백/모니터링 체계 검증 완료

GA 전환은 단순한 버전 점프가 아니라 **"우리 이제 API 안정성 약속함"** 이라는 외부 선언이다. 이후부터 breaking change는 major 증가가 강제된다.

---

## 트리거

- `/prod-release [버전]` — 예: `/prod-release 0.2.0`
- "프로덕션 배포", "main 머지", "릴리즈 해줘", "release"

버전 미지정 시 Step 2에서 변경 사항 분석 후 제안.

---

## 실행 절차

### Step 1: 사전 상태 점검

```bash
# 현재 브랜치 확인 (develop이어야 함)
git rev-parse --abbrev-ref HEAD

# develop 최신화
git fetch origin
git checkout develop
git pull origin main --ff-only 2>/dev/null || true   # main의 핫픽스 동기화
git pull origin develop --ff-only

# main과 develop 차이 확인
git log --oneline origin/main..origin/develop
```

**중단 조건:**
- develop이 아니면 → "develop 브랜치에서만 실행 가능합니다" 후 중단
- `main..develop` 커밋이 0개면 → "릴리즈할 변경이 없습니다" 후 중단
- 미커밋 변경사항 있으면 → 사용자에게 커밋/스태시 요청

#### GitHub Actions 시크릿 검증 (필수)

`deploy-prod.yml`이 **실제 prod 리소스**를 가리키는지 확인. 시크릿이 dev로 설정되어 있으면 prod 배포가 dev를 덮어쓰는 치명적 사고 발생 (2026-04-15 v0.1.1 ↔ v0.1.2 사이에 실제 발생한 사고).

```bash
# 마지막 업데이트 시점이 prod 인프라 구축일보다 이전이면 의심
gh secret list | grep -E "EC2_HOST|S3_BUCKET|CLOUDFRONT|EC2_SSH"
```

**교차 검증 방법**: 최신 deploy-prod 워크플로우 로그에서 실제 접속된 호스트/버킷을 추적
```bash
# 최근 성공 run의 deploy 로그에서 s3 sync 타겟 확인
gh run view <RUN_ID> --log | grep -E "s3 sync|EC2_HOST"
```

**점검 체크리스트** (`.claude.local.md`의 prod 리소스와 비교):
- [ ] `EC2_HOST` == prod EIP (예: `43.201.187.118`)
- [ ] `EC2_SSH_KEY` == prod key 내용 (`rehearse-prod-key.pem`)
- [ ] `S3_BUCKET_NAME` == prod 프론트 버킷 (`rehearse-frontend-prod`)
- [ ] `CLOUDFRONT_DISTRIBUTION_ID` == prod CF ID (예: `E2UWW3KP4S5VOV`)

**수정 명령:**
```bash
gh secret set EC2_HOST --body "<PROD_EIP>"
gh secret set S3_BUCKET_NAME --body "<PROD_FRONTEND_BUCKET>"
gh secret set CLOUDFRONT_DISTRIBUTION_ID --body "<PROD_CF_ID>"
gh secret set EC2_SSH_KEY < ~/.ssh/<PROD_KEY>.pem
```

> **핵심 원칙**: prod 인프라가 새로 구축되거나 재구성된 직후에는 반드시 이 단계를 수행. 시크릿은 `gh secret list`에서 **last update 시점**만 보이고 값은 보이지 않으므로, 타임스탬프 기반 추론 + 로그 교차 검증이 유일한 경로.

### Step 2: 버전 결정

최신 태그 조회:
```bash
LATEST_TAG=$(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || echo "v0.0.0")
```

`main..develop` 커밋 메시지를 분석해 증가 타입 제안 (0.x 페이즈 기준):

| 패턴 | 제안 (0.x) |
|------|-----------|
| `feat:` 포함 / `BREAKING CHANGE` / `!:` | **Minor** |
| `fix:`, `refactor:`, `chore:`, `docs:`만 | **Patch** |
| 사용자가 "GA" 명시 | **1.0.0** (별도 확인) |

> 0.x에서는 breaking change도 minor로 처리한다. Major 증가는 "정식 GA" 선언 시 0 → 1 한 번뿐.

제안 예시:
```
최신 태그: (없음 → 첫 릴리즈, 0.1.0 부터 시작)
혹은
최신 태그: v0.1.0
변경 요약: feat 3건, fix 5건, refactor 2건
→ 제안: v0.2.0 (Minor)

이 버전으로 진행할까요? (Y/n, 다른 버전 입력 가능)
```

사용자 확정 후 `NEW_VERSION` 변수에 저장 (예: `NEW_VERSION=v0.2.0`).

**첫 릴리즈 처리:** `git describe` 가 실패하면 `LATEST_TAG=없음`으로 간주하고 `v0.1.0` 을 기본 제안으로 내민다.

### Step 3: 앱 버전 파일 동기화 (선택·권장)

**frontend `package.json`:**
```bash
cd frontend
npm version --no-git-tag-version ${NEW_VERSION#v}
cd ..
```

**backend `build.gradle.kts`** (현재 `0.1.0`):
```bash
# version = "0.1.0" → version = "0.2.0"
sed -i.bak "s/^version = \".*\"/version = \"${NEW_VERSION#v}\"/" backend/build.gradle.kts && rm backend/build.gradle.kts.bak
```

변경 확정:
```bash
git add frontend/package.json backend/build.gradle.kts
git commit -m "chore: release ${NEW_VERSION}

버전 파일 동기화

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
git push origin develop
```

### Step 4: 릴리즈 PR 생성 (develop → main)

**브랜치 전략**: 릴리즈 전용 브랜치 **불필요**. develop에서 main으로 직접 PR한다 (릴리즈 브랜치는 병렬 개발이 많을 때 필요한데, 이 프로젝트는 develop이 곧 스테이징).

**PR 제목:**
```
Release ${NEW_VERSION}
```

**PR 본문** — `.github/pull_request_template.md`를 따르되, 릴리즈용으로 변형한 아래 템플릿 사용:

```markdown
## 릴리즈 개요

- **버전**: ${NEW_VERSION}
- **이전 버전**: ${LATEST_TAG}
- **배포 대상**: prod (https://rehearse.co.kr, https://api.rehearse.co.kr)

## 변경 내용

### ✨ 기능 추가 (feat)
<!-- main..develop 커밋 중 feat: -->
-

### 🐛 버그 수정 (fix)
<!-- main..develop 커밋 중 fix: -->
-

### 🔧 리팩토링/기타 (refactor/chore/docs)
-

## 영향 범위 & 리스크

- **DB 마이그레이션**: 있음/없음 (있으면 스크립트/롤백 방법)
- **환경 변수 변경**: 있음/없음
- **API 호환성**: breaking / compatible
- **데이터 영향**: 있음/없음

## 배포 체크리스트

- [ ] CI 통과 (Backend CI, Frontend CI)
- [ ] develop에서 수동 smoke 테스트 (dev 환경)
- [ ] 환경 변수/시크릿 prod에 반영됨
- [ ] DB 마이그레이션 prod 실행 계획 확인 (해당 시)
- [ ] 머지 후 prod 헬스체크 확인
- [ ] GitHub Release 노트 발행

## 배포 후 검증

- [ ] https://api.rehearse.co.kr/actuator/health → 200
- [ ] https://rehearse.co.kr 랜딩 페이지 로드
- [ ] 주요 플로우 1개 수동 검증 (면접 생성 → 피드백)

## 롤백 방법

1. 직전 태그로 revert:
   \`\`\`bash
   git revert -m 1 <merge-commit-sha>
   git push origin main
   \`\`\`
2. 또는 이전 ECR 이미지로 수동 교체 (EC2에서 `docker compose`):
   - ECR 태그: `prod-<이전_sha>`

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

**PR 생성 명령:**
```bash
gh pr create \
  --base main \
  --head develop \
  --title "Release ${NEW_VERSION}" \
  --body "$(cat /tmp/release-pr-body.md)"
```

**Draft로 먼저 생성**하지 말 것 — 바로 리뷰 요청 상태로 생성. 단, "여러 PR이 연속 대기 중"이면 Draft로 생성 후 사용자가 올리도록 한다.

### Step 5: 머지 대기 (수동 단계)

자동 머지 **금지**. 사용자가 리뷰 후 직접 **"Create a merge commit"** 으로 머지한다.
- Squash 금지: develop의 개별 커밋 히스토리를 main에도 보존해야 태그가 의미 있는 지점을 가리킴
- Rebase 금지: merge commit이 있어야 "릴리즈 지점"이 명확함

**사용자에게 안내:**
```
✅ 릴리즈 PR 생성 완료: <PR_URL>

리뷰 후 "Create a merge commit"으로 머지해주세요.
머지되면 이어서 `/prod-release continue ${NEW_VERSION}`를 실행하면
태그 발행 + GitHub Release를 처리합니다.

(또는 머지가 끝난 상태에서 바로 이 스킬을 다시 실행하면 자동 감지하여 태그 단계로 진입합니다.)
```

### Step 6: 머지 확인 및 태그 발행

머지 커밋 SHA 확인:
```bash
git fetch origin main
git checkout main
git pull origin main --ff-only
MERGE_SHA=$(git rev-parse HEAD)
```

**Annotated tag** 발행 (lightweight 태그 절대 금지 — release 노트 본문 저장):
```bash
# 태그 메시지: PR 본문의 "변경 내용" 섹션을 재활용
git tag -a "${NEW_VERSION}" -m "Release ${NEW_VERSION}

$(gh pr view <PR번호> --json body -q .body | sed -n '/## 변경 내용/,/## 영향 범위/p' | sed '$d')
"

git push origin "${NEW_VERSION}"
```

### Step 7: GitHub Release 발행

**원칙**: 릴리즈 노트는 단순 커밋 목록이 아니라 **사용자 관점의 변경사항 요약**이어야 한다. 기능 영역별로 그룹화하여 읽는 사람이 "이 릴리즈에서 뭐가 바뀌었는지"를 한눈에 파악할 수 있게 작성한다.

#### 작성 전략

| 릴리즈 유형 | 범위 | 방식 |
|------------|------|------|
| **첫 릴리즈 (태그 없음)** | 저장소 전체 히스토리 | 영역별 테마 그룹(면접 플로우·AI 피드백·인프라 등)으로 요약. 커밋 나열 금지 |
| **Minor (feat 포함)** | `${LATEST_TAG}..main` | 사용자 영향 있는 기능을 `✨ 기능`, `🐛 버그`, `♻️ 품질` 섹션으로 분류 |
| **Patch (fix only)** | `${LATEST_TAG}..main` | 버그/개선 위주로 압축 요약. `--generate-notes`로 충분할 수 있음 |

#### 작성 명령

**Patch 또는 일반 Minor**: 자동 생성 + 필요시 보정
```bash
gh release create "${NEW_VERSION}" \
  --target main \
  --title "${NEW_VERSION}" \
  --generate-notes \
  --notes-start-tag "${LATEST_TAG}"
```

**첫 릴리즈 / 주요 Minor**: 수동 작성 권장
```bash
# 1) 변경 범위 파악
git log --oneline "${LATEST_TAG:-$(git rev-list --max-parents=0 HEAD)}"..main \
  | grep -iE "feat|fix:|refactor:" > /tmp/changes.txt

# 2) Claude가 /tmp/changes.txt를 읽고 영역별 요약 작성 → /tmp/release-notes.md

# 3) 릴리즈 생성 (또는 edit)
gh release create "${NEW_VERSION}" \
  --target main \
  --title "${NEW_VERSION}" \
  --notes-file /tmp/release-notes.md

# 이미 생성된 릴리즈면
gh release edit "${NEW_VERSION}" --notes-file /tmp/release-notes.md
```

#### 릴리즈 노트 템플릿 (Minor 이상)

```markdown
[1~2줄 하이라이트 — 이 릴리즈의 가장 큰 변화]

## ✨ 기능 추가
### {영역 A} (예: 면접 플로우)
- 항목 1 (#PR번호)
- 항목 2

### {영역 B} (예: AI 피드백)
- ...

## 🐛 버그 수정
- ...

## ♻️ 품질·리팩토링
- ...

## 🛠 인프라 & 운영
- ...

## 🔄 마이그레이션/롤백 노트 (해당 시)
- DB 스키마 변경, 환경 변수 추가 등
- 롤백 명령 또는 주의사항
```

**Pre-release 태그(`-beta`, `-rc`)인 경우:**
```bash
gh release create "${NEW_VERSION}" --prerelease ...
```

### Step 8: 배포 모니터링

main push → `deploy-prod.yml` 자동 실행. 이 스킬은 **워크플로우 완료까지 대기**한다:

```bash
# 실행 중인 워크플로우 조회
RUN_ID=$(gh run list --workflow=deploy-prod.yml --branch=main --limit=1 --json databaseId -q '.[0].databaseId')
gh run watch "$RUN_ID" --exit-status
```

성공하면 헬스체크:
```bash
curl -sf https://api.rehearse.co.kr/actuator/health | jq .
curl -sIf https://rehearse.co.kr | head -1
```

### Step 9: 성공 리포트

```
✅ 프로덕션 배포 완료
  버전:        ${LATEST_TAG} → ${NEW_VERSION}
  머지 SHA:    ${MERGE_SHA:0:7}
  GitHub Release: <release_url>

  검증 결과:
    - CI:                 ✓
    - deploy-prod 워크플로우: ✓
    - /actuator/health:   200
    - 프론트 랜딩:         200

  롤백 명령 (문제 발생 시):
    git revert -m 1 ${MERGE_SHA}
    git push origin main
    # 또는 이전 ECR 이미지 교체: prod-<이전 커밋 SHA>
```

### Step 10: 실패 시 리포트

배포 실패(워크플로우 fail 또는 헬스체크 실패) 시:

```
⚠️ 배포 실패 감지
  실패 단계: {deploy-prod / health-check}
  로그:      gh run view ${RUN_ID} --log-failed

  즉시 조치:
    1. 로그 확인 후 원인 파악
    2. 빠른 수정 가능 → fix 커밋 후 develop → main 핫픽스 PR
    3. 즉시 롤백 필요 → 아래 명령 실행
         git revert -m 1 ${MERGE_SHA}
         git push origin main
    4. 이전 이미지로 수동 복구:
         ssh ec2-user@<host>
         cd ~/rehearse/backend
         docker compose pull backend   # 이전 태그 지정 필요
```

---

## 첫 릴리즈 부트스트랩 플로우 (main 브랜치가 없을 때)

**트리거 조건**: `origin/main`이 존재하지 않음 + 태그 없음. (2026-04-15 v0.1.0이 이 플로우로 발행되었음. 저장소 재초기화나 신규 프로젝트에서만 재발생)

### 왜 일반 플로우를 쓸 수 없나
- `develop → main` PR은 main이 존재해야 가능
- paths-filter는 브랜치 최초 생성(`before=0000000`)일 때 변경 감지가 실패 → backend/frontend 빌드·배포 단계가 **전부 skip**
  - 과거에는 default branch 폴백으로도 같은 문제가 있었으나, 현재는 `deploy-prod.yml`에 `base: github.event.before || HEAD~1` 명시로 해결됨

### 부트스트랩 절차

1. **버전 파일 동기화** (Step 3과 동일)
   - `backend/build.gradle.kts`, `frontend/package.json` → 첫 버전(`0.1.0`)
   - develop에 commit & push

2. **main 브랜치 생성 (직접 push, 1회성)**
   ```bash
   git push origin develop:main
   ```
   > 이후 릴리즈부터는 PR 플로우를 따른다. 이 직접 push는 부트스트랩 1회 예외.

3. **GitHub 기본 브랜치 유지** (`develop`)
   - 이 프로젝트는 default branch = `develop`이 정상 상태 (feature PR의 기본 타겟)
   - 과거에는 paths-filter의 default branch 폴백 때문에 부트스트랩 시 `main`으로 전환했으나, 현재는 `deploy-prod.yml`에 `base: github.event.before || HEAD~1`이 명시되어 default branch에 의존하지 않음 → 전환 불필요

4. **Annotated tag 발행 + GitHub Release**
   ```bash
   git tag -a "v0.1.0" <main_sha> -m "Release v0.1.0 ..."
   git push origin v0.1.0
   gh release create v0.1.0 --target main --notes-file /tmp/release-notes.md
   ```

5. **첫 push 워크플로우는 skip됨 → `workflow_dispatch`로 재실행**
   ```bash
   gh workflow run deploy-prod.yml --ref main
   ```
   - 이 시점에 paths-filter는 `before`가 없어서 HEAD~1 fallback → 버전 bump 커밋의 변경이 정상 검출됨
   - 이후부터는 정상 트리거

6. **헬스체크** (Step 8~9)

### 부트스트랩의 한계

- 첫 릴리즈는 "이전 이미지" ECR 태그가 없어 **fix-forward만 가능**. revert-based rollback은 의미 없음
- PR 리뷰 게이트가 없음 → 릴리즈 체크리스트를 텍스트로 수동 확인
- 브랜치 보호룰이 있으면 직접 push가 막힘. 부트스트랩 시점에 보호룰 해제 or 생성 전에 수행

### 부트스트랩 후 검증

- `gh repo view --json defaultBranchRef` → `main` 확인
- paths-filter annotation에 `"changes will be detected from last commit"` 메시지 확인 (HEAD~1 fallback 정상 동작)
- 다음 릴리즈부터 Step 1~10 정규 플로우 적용

---

## 핫픽스(긴급) 플로우

프로덕션에 긴급 수정이 필요할 때:

1. `main`에서 `hotfix/{간단설명}` 브랜치 생성
2. 수정 + 커밋
3. `main`으로 PR, 버전은 **Patch** 증가 (`v0.2.0` → `v0.2.1`)
4. 머지 후 Step 6~9 수행
5. **반드시 `develop`에도 역동기화**: `main` → `develop` cherry-pick 또는 merge PR

---

## 안전 장치 요약

| 단계 | 장치 | 효과 |
|------|------|------|
| Step 1 | main-develop diff 확인 | 빈 릴리즈 방지 |
| Step 2 | 커밋 기반 버전 제안 | 일관된 SemVer |
| Step 3 | 앱 버전 파일 동기화 | 런타임에서도 버전 조회 가능 |
| Step 4 | 릴리즈 PR 템플릿 | 리뷰 누락 방지 |
| Step 5 | merge commit 유지 | 태그 기준점 명확 |
| Step 6 | Annotated tag | 릴리즈 메타데이터 보존 |
| Step 7 | GitHub Release | 대외 changelog 자동화 |
| Step 8 | 워크플로우 watch + 헬스체크 | 배포 실패 즉시 감지 |
| Step 10 | revert + ECR 이전 태그 | 2분 내 롤백 가능 |

---

## FAQ

**Q. Squash/Rebase 머지가 왜 금지?**
A. `v0.2.0` 태그가 가리키는 merge commit이 곧 "이 시점 이전의 모든 커밋 = 이 릴리즈의 범위"를 정의한다. Squash하면 개별 커밋이 사라져 `git log v0.1.0..v0.2.0`이 의미를 잃는다.

**Q. develop에 이미 다음 버전 작업이 섞여 있으면?**
A. 이 스킬은 "develop = 다음 릴리즈 후보"를 전제로 한다. 작업이 너무 많이 섞이면 릴리즈 브랜치(`release/v0.2.0`)로 분리하는 전략으로 확장할 수 있다. 지금은 **develop을 짧게 유지**하는 것이 원칙.

**Q. Lambda 배포는?**
A. 이 스킬과 별개. `lambda-deploy` 스킬을 사용한다. 백엔드 릴리즈와 Lambda 배포가 연동되어야 한다면 이 스킬 Step 8 이후에 수동으로 `/lambda-deploy`를 실행한다.

**Q. 0.x에서 breaking change 생겼는데 minor만 올려도 되나?**
A. 된다. SemVer 사양상 0.x는 "불안정 기간"으로 정의되어 있어 minor 증가만으로 호환성 깨짐을 허용한다. 단, **변경 내용에 명시**하고 프론트엔드/백엔드 모두 같은 릴리즈에서 맞춰 올린다.

**Q. 언제 `v1.0.0`으로 넘어가나?**
A. "API 안정성 약속 가능" + "BETA 배지 제거 시점" 두 조건이 충족될 때. 버저닝 섹션의 GA 발행 기준 참조. 그 전까지는 0.x만 계속 증가.
