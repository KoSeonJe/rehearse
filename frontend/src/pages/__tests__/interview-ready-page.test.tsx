import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HelmetProvider } from 'react-helmet-async'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { InterviewReadyPage } from '../interview-ready-page'
import type { useInterview as UseInterviewType } from '@/hooks/use-interviews'

vi.mock('@/hooks/use-interviews', () => ({
  useInterview: vi.fn(),
  useUpdateInterviewStatus: () => ({ mutate: vi.fn(), isPending: false }),
  useRetryQuestions: () => ({ mutate: vi.fn(), isPending: false }),
}))

vi.mock('@/hooks/use-device-test', () => ({
  useDeviceTest: () => ({
    state: { camera: 'idle', mic: 'idle', speaker: 'idle' },
    micLevel: 0,
    videoRef: { current: null },
    allPassed: false,
    startCameraTest: vi.fn(),
    startMicTest: vi.fn(),
    startSpeakerTest: vi.fn(),
    confirmSpeaker: vi.fn(),
    resetMicTest: vi.fn(),
    resetSpeakerTest: vi.fn(),
  }),
}))

const { useInterview } = await import('@/hooks/use-interviews')
const mockUseInterview = vi.mocked(useInterview)

type UseInterviewReturn = ReturnType<typeof UseInterviewType>

const makeInterviewResponse = (
  interviewTypes: string[],
  questionGenerationStatus: string,
): UseInterviewReturn =>
  ({
    data: {
      data: {
        id: 1,
        publicId: 'abc',
        status: 'READY',
        position: 'BACKEND',
        level: 'JUNIOR',
        interviewTypes,
        csSubTopics: [],
        durationMinutes: 30,
        questionGenerationStatus,
        questionSets: [],
        failureReason: null,
      },
    },
    isLoading: false,
    isError: false,
    error: null,
  }) as unknown as UseInterviewReturn

const renderPage = () =>
  render(
    <HelmetProvider>
      <MemoryRouter initialEntries={['/interview/1/ready']}>
        <Routes>
          <Route path="/interview/:id/ready" element={<InterviewReadyPage />} />
        </Routes>
      </MemoryRouter>
    </HelmetProvider>,
  )

describe('InterviewReadyPage — 질문 생성 안내 문구', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('일반 면접 GENERATING 상태에서 기본 안내 문구를 렌더한다', () => {
    mockUseInterview.mockReturnValue(makeInterviewResponse(['BEHAVIORAL'], 'GENERATING'))
    renderPage()
    expect(screen.getByText('AI가 면접 질문을 생성하고 있어요...')).toBeInTheDocument()
    expect(screen.queryByText(/이력서를 분석하고/)).not.toBeInTheDocument()
  })

  it('일반 면접 PENDING 상태에서 기본 안내 문구를 렌더한다', () => {
    mockUseInterview.mockReturnValue(makeInterviewResponse(['CS_FUNDAMENTAL'], 'PENDING'))
    renderPage()
    expect(screen.getByText('AI가 면접 질문을 생성하고 있어요...')).toBeInTheDocument()
  })

  it('RESUME_BASED + GENERATING 상태에서 이력서 분석 안내 문구를 렌더한다', () => {
    mockUseInterview.mockReturnValue(makeInterviewResponse(['RESUME_BASED'], 'GENERATING'))
    renderPage()
    expect(screen.getByText('이력서를 분석하고 첫 질문을 준비하고 있어요...')).toBeInTheDocument()
    expect(screen.queryByText('AI가 면접 질문을 생성하고 있어요...')).not.toBeInTheDocument()
  })

  it('RESUME_BASED + PENDING 상태에서 이력서 분석 부제를 렌더한다', () => {
    mockUseInterview.mockReturnValue(makeInterviewResponse(['RESUME_BASED'], 'PENDING'))
    renderPage()
    expect(
      screen.getByText('이력서 내용을 바탕으로 맞춤 질문을 준비합니다 · 1분 내외 소요'),
    ).toBeInTheDocument()
  })

  it('일반 면접에서는 이력서 부제가 렌더되지 않는다', () => {
    mockUseInterview.mockReturnValue(makeInterviewResponse(['BEHAVIORAL'], 'PENDING'))
    renderPage()
    expect(screen.queryByText(/맞춤 질문을 준비합니다/)).not.toBeInTheDocument()
  })

  it('RESUME_BASED + COMPLETED 상태에서는 생성 중 스피너가 노출되지 않는다', () => {
    mockUseInterview.mockReturnValue(makeInterviewResponse(['RESUME_BASED'], 'COMPLETED'))
    renderPage()
    expect(screen.queryByText('이력서를 분석하고 첫 질문을 준비하고 있어요...')).not.toBeInTheDocument()
  })
})
