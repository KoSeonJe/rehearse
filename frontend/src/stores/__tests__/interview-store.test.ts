import { describe, it, expect, beforeEach } from 'vitest'
import { useInterviewStore } from '@/stores/interview-store'

describe('interview-store — 시간 만료 신호 (isTimeOverdue)', () => {
  beforeEach(() => {
    useInterviewStore.getState().reset()
  })

  it('초기 상태에서 isTimeOverdue 는 false 이다', () => {
    expect(useInterviewStore.getState().isTimeOverdue).toBe(false)
  })

  it('setTimeOverdue(true) 호출 시 플래그가 true 로 set 된다', () => {
    useInterviewStore.getState().setTimeOverdue(true)
    expect(useInterviewStore.getState().isTimeOverdue).toBe(true)
  })

  it('reset() 호출 시 isTimeOverdue 가 false 로 복귀한다', () => {
    useInterviewStore.getState().setTimeOverdue(true)
    expect(useInterviewStore.getState().isTimeOverdue).toBe(true)

    useInterviewStore.getState().reset()
    expect(useInterviewStore.getState().isTimeOverdue).toBe(false)
  })
})
