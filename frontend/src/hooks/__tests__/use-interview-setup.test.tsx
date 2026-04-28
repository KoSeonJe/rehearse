import { describe, expect, it, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useInterviewSetup } from '@/hooks/use-interview-setup'

vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
}))

vi.mock('@/hooks/use-interviews', () => ({
  useCreateInterview: () => ({
    isPending: false,
    mutate: vi.fn(),
  }),
}))

describe('useInterviewSetup — RESUME_BASED 단독 선택 규칙', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('RESUME_BASED 선택 시 단독으로 활성화된다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handleTypeToggle('CS_FUNDAMENTAL')
      result.current.handleTypeToggle('BEHAVIORAL')
    })
    expect(result.current.interviewTypes).toEqual(['CS_FUNDAMENTAL', 'BEHAVIORAL'])

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
    })

    expect(result.current.interviewTypes).toEqual(['RESUME_BASED'])
  })

  it('RESUME_BASED 활성 상태에서 다른 타입 클릭 시 선택 변경되지 않는다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
    })
    expect(result.current.interviewTypes).toEqual(['RESUME_BASED'])

    act(() => {
      result.current.handleTypeToggle('CS_FUNDAMENTAL')
    })

    expect(result.current.interviewTypes).toEqual(['RESUME_BASED'])
  })

  it('RESUME_BASED 비활성화(토글 off) 시 선택 목록에서 제거된다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
    })
    expect(result.current.interviewTypes).toEqual(['RESUME_BASED'])

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
    })

    expect(result.current.interviewTypes).toEqual([])
  })

  it('RESUME_BASED 비활성화 후 다른 타입 정상 선택 가능하다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
      result.current.handleTypeToggle('RESUME_BASED')
    })
    expect(result.current.interviewTypes).toEqual([])

    act(() => {
      result.current.handleTypeToggle('BEHAVIORAL')
    })

    expect(result.current.interviewTypes).toEqual(['BEHAVIORAL'])
  })

  it('RESUME_BASED 활성 시 isOtherTypesDisabled가 true이다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
    })

    expect(result.current.isOtherTypesDisabled).toBe(true)
  })

  it('RESUME_BASED 비활성 시 isOtherTypesDisabled가 false이다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    expect(result.current.isOtherTypesDisabled).toBe(false)

    act(() => {
      result.current.handleTypeToggle('CS_FUNDAMENTAL')
    })

    expect(result.current.isOtherTypesDisabled).toBe(false)
  })

  it('Step 2 는 techStack 이 선택되어야만 다음 진행이 허용된다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handlePositionSelect('BACKEND')
    })
    expect(result.current.canNext(2)).toBe(true)

    act(() => {
      result.current.handleTechStackSelect(null)
    })
    expect(result.current.canNext(2)).toBe(false)
    expect(result.current.disabledHint(2)).toBe('기술 스택을 선택해주세요')
  })

  it('RESUME_BASED 선택 후 파일 미업로드 시 disabledHint 가 파일 안내로 분기한다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
    })

    expect(result.current.canNext(5)).toBe(false)
    expect(result.current.disabledHint(5)).toBe('이력서 PDF를 업로드해주세요')
  })

  it('Step 5 면접 유형 미선택 시 기본 hint 가 노출된다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    expect(result.current.canNext(5)).toBe(false)
    expect(result.current.disabledHint(5)).toBe('면접 유형을 하나 이상 선택해주세요')
  })

  it('RESUME_BASED 선택 시 기존 CS 세부 주제가 초기화된다', () => {
    const { result } = renderHook(() => useInterviewSetup())

    act(() => {
      result.current.handleTypeToggle('CS_FUNDAMENTAL')
    })
    act(() => {
      result.current.handleCsSubTopicToggle('OS')
      result.current.handleCsSubTopicToggle('NETWORK')
    })
    expect(result.current.csSubTopics).toEqual(['OS', 'NETWORK'])

    act(() => {
      result.current.handleTypeToggle('RESUME_BASED')
    })

    expect(result.current.csSubTopics).toEqual([])
    expect(result.current.interviewTypes).toEqual(['RESUME_BASED'])
  })
})
