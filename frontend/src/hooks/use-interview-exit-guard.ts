import { useCallback, useEffect, useState } from 'react'

/**
 * 면접 진행 중 뒤로가기/새로고침/탭 닫기 이탈 가드.
 *
 * 현재 라우터가 `BrowserRouter`(non-data router)라 v7 `useBlocker`를 쓸 수 없어
 * popstate 센티넬 + beforeunload로 동등 기능을 구현한다. 설계 배경은
 * plans/idempotent-popping-cascade.md 참고.
 *
 * - active=false면 모든 리스너/상태가 해제되고 blocked도 자동 리셋된다.
 * - suppress=true면 popstate가 발생해도 모달을 띄우지 않는다 (다른 모달이 이미 열린 경우).
 * - 프로그래매틱 navigate(..., { replace: true })는 replaceState를 호출해 popstate를
 *   발생시키지 않으므로 이 가드에 블록되지 않는다.
 */
const SENTINEL_STATE = { __interviewGuard: true }

const isSentinelState = (state: unknown): boolean =>
  typeof state === 'object' && state !== null && '__interviewGuard' in state

export const useInterviewExitGuard = ({
  active,
  suppress = false,
}: {
  active: boolean
  suppress?: boolean
}) => {
  const [blocked, setBlocked] = useState(false)
  // active가 꺼지면 blocked 잔상을 즉시 무시 (finishing → completed 자동 전이 시 모달 정리).
  // 별도 effect 없이 파생값으로 계산해 cascading render를 피한다.
  const effectiveBlocked = active && blocked

  // 브라우저 레벨 이탈 (탭 닫기 / 새로고침 / 주소 직접 입력)
  useEffect(() => {
    if (!active) return
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [active])

  // 브라우저 뒤로/앞으로가기, 모바일 스와이프 back
  useEffect(() => {
    if (!active) return

    // 이미 센티넬에 있는 경우(재-활성·StrictMode 이중 마운트) 중복 push 방지
    if (!isSentinelState(window.history.state)) {
      window.history.pushState(SENTINEL_STATE, '', window.location.href)
    }

    const handler = () => {
      // 센티넬을 다시 push하여 URL 유지 (브라우저는 이전 히스토리 엔트리로 이동한 상태)
      window.history.pushState(SENTINEL_STATE, '', window.location.href)
      if (suppress) return
      setBlocked(true)
    }

    window.addEventListener('popstate', handler)
    return () => window.removeEventListener('popstate', handler)
  }, [active, suppress])

  const dismiss = useCallback(() => setBlocked(false), [])

  return { blocked: effectiveBlocked, dismiss }
}
