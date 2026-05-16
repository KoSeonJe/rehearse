# Task B3 — FE useS3Upload 시그니처 변경 + x-amz-meta-trace-id 헤더 송신

> **PR**: PR-B
> **영역**: FE
> **선행 Task**: B2 (Response.traceId 노출)

---

## 변경 파일

- `frontend/src/hooks/use-s3-upload.ts` — **변경**. `upload(blob, presignedUrl)` → `upload(blob, presignedUrl, traceId)`. `xhr.setRequestHeader('x-amz-meta-trace-id', traceId)` 추가.
- `frontend/src/hooks/__tests__/use-s3-upload.test.tsx` — **신규 또는 확장**. XHR mock 으로 헤더 송신 검증.
- 호출부 — `UploadUrlResponse.traceId` 를 받아 `upload(...)` 에 전달하도록 수정. **3 site 인벤토리** (전수):
  - `frontend/src/hooks/use-answer-flow.ts:166` — `await s3Upload.upload(blob, urlRes.data.uploadUrl)` → `..., urlRes.data.traceId)`
  - `frontend/src/hooks/use-interview-session.ts:293` — `await s3UploadForFinish.upload(blob, urlRes.data.uploadUrl)` → `..., urlRes.data.traceId)`
  - `frontend/src/hooks/use-interview-session.ts:366` — `await s3UploadForFinish.upload(recoveredBlob, urlRes.data.uploadUrl)` → `..., urlRes.data.traceId)`

---

## 핵심 로직

```typescript
// use-s3-upload.ts (변경부)
export function useS3Upload() {
  const upload = useCallback((blob: Blob, presignedUrl: string, traceId: string) => {
    return new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', presignedUrl);
      xhr.setRequestHeader('Content-Type', blob.type);
      xhr.setRequestHeader('x-amz-meta-trace-id', traceId);  // 신규
      xhr.onload = () => xhr.status < 300 ? resolve() : reject(...);
      xhr.onerror = () => reject(...);
      xhr.send(blob);
    });
  }, []);
  return { upload };
}
```

---

## 테스트

**카테고리**: FE 단위 (vitest + XHR mock)

```typescript
describe('useS3Upload', () => {
  it('PUT 요청에 x-amz-meta-trace-id 헤더를 포함시킨다', async () => {
    const xhrMock = mockXMLHttpRequest();
    const { result } = renderHook(() => useS3Upload());
    await result.current.upload(blob, 'https://...', 'abc12345');
    expect(xhrMock.setRequestHeader).toHaveBeenCalledWith('x-amz-meta-trace-id', 'abc12345');
  });
});
```

호출부 (예: 인터뷰 답변 영상 업로드 컴포넌트) 수정:
```typescript
const { uploadUrl, traceId } = await fetchUploadUrl(interviewId, questionSetId);
await upload(blob, uploadUrl, traceId);
```

---

## 의존

- 선행: B2 (Response.traceId 응답 필드)
- 외부: XMLHttpRequest

---

## Verification Hook

- 명령: `cd frontend && npm run test -- src/hooks/__tests__/use-s3-upload.test.tsx`
- 통과 기준: 헤더 송신 검증 케이스 green
- 관찰 가능 동작: dev 환경 1회 업로드 → 브라우저 devtools Network 탭 → S3 PUT 요청 헤더 `x-amz-meta-trace-id` 확인 + 응답 200

---

## 커밋 메시지 (예상)

```
feat(FE): S3 PUT 시 x-amz-meta-trace-id 헤더 송신
```
