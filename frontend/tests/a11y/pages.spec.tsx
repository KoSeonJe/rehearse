/**
 * a11y Smoke Tests — 14 Pages
 *
 * axe-core로 각 페이지를 렌더하고 critical/serious 위반이 없는지 검사합니다.
 * - critical/serious 위반 → 테스트 실패
 * - minor/moderate 위반 → console.warn (백로그 이관)
 *
 * 전략:
 * - 모든 페이지는 공통 AllProviders wrapper로 감쌉니다.
 * - API / 라우터 / 미디어 의존성은 vi.mock으로 stubbing합니다.
 * - 복잡한 상태 의존 페이지(interview-page 등)는 loading skeleton만 렌더되게 합니다.
 */

import React from 'react'
import { describe, it, expect, vi, beforeAll, afterEach } from 'vitest'
import { render } from '@testing-library/react'
import { axe } from 'vitest-axe'
import { toHaveNoViolations } from 'vitest-axe/matchers'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

// ── extend expect ────────────────────────────────────────────────────────────
expect.extend(toHaveNoViolations)

// ── jsdom shims ──────────────────────────────────────────────────────────────
beforeAll(() => {
  // matchMedia
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })

  // ResizeObserver
  global.ResizeObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
  }))

  // IntersectionObserver — must be a real class to support `new`
  global.IntersectionObserver = class {
    observe = vi.fn()
    unobserve = vi.fn()
    disconnect = vi.fn()
    constructor(_cb: IntersectionObserverCallback, _options?: IntersectionObserverInit) {}
    takeRecords(): IntersectionObserverEntry[] { return [] }
    readonly root: Element | null = null
    readonly rootMargin: string = ''
    readonly thresholds: ReadonlyArray<number> = []
  }

  // MediaRecorder
  global.MediaRecorder = vi.fn().mockImplementation(() => ({
    start: vi.fn(),
    stop: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    state: 'inactive',
  })) as unknown as typeof MediaRecorder
  ;(global.MediaRecorder as unknown as { isTypeSupported: () => boolean }).isTypeSupported = vi.fn().mockReturnValue(true)

  // getUserMedia
  Object.defineProperty(navigator, 'mediaDevices', {
    writable: true,
    value: {
      getUserMedia: vi.fn().mockResolvedValue({
        getTracks: () => [],
        getAudioTracks: () => [],
        getVideoTracks: () => [],
      }),
      enumerateDevices: vi.fn().mockResolvedValue([]),
    },
  })

  // HTMLMediaElement
  Object.defineProperty(HTMLMediaElement.prototype, 'play', { writable: true, value: vi.fn() })
  Object.defineProperty(HTMLMediaElement.prototype, 'pause', { writable: true, value: vi.fn() })
  Object.defineProperty(HTMLMediaElement.prototype, 'load', { writable: true, value: vi.fn() })
})

// ── global mocks ─────────────────────────────────────────────────────────────

vi.mock('react-helmet-async', () => ({
  Helmet: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
  HelmetProvider: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
}))

vi.mock('sonner', () => ({ toast: vi.fn(), Toaster: () => null }))

vi.mock('next-themes', () => ({
  useTheme: () => ({ theme: 'light', setTheme: vi.fn(), resolvedTheme: 'light' }),
  ThemeProvider: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
}))

// Auth
vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({ user: null, isAuthenticated: false, isLoading: false }),
}))

vi.mock('@/hooks/use-logout', () => ({
  useLogout: () => vi.fn(),
}))

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: () => ({
    showLoginModal: false,
    openLoginModal: vi.fn(),
    closeLoginModal: vi.fn(),
  }),
}))

// Interviews
vi.mock('@/hooks/use-interviews', () => ({
  useInterviews: () => ({ data: undefined, isLoading: true }),
  useInterviewStats: () => ({ data: undefined, isLoading: true }),
  useDeleteInterview: () => ({ mutate: vi.fn(), isPending: false }),
  useInterview: () => ({ data: undefined, isLoading: true }),
  useInterviewByPublicId: () => ({ data: undefined, isLoading: true }),
  useUpdateInterviewStatus: () => ({ mutate: vi.fn(), isPending: false }),
  useRetryQuestions: () => ({ mutate: vi.fn(), isPending: false }),
}))

// Interview setup
vi.mock('@/hooks/use-interview-setup', () => ({
  useInterviewSetup: () => ({
    currentStep: 1,
    totalSteps: 5,
    position: null,
    techStack: null,
    level: null,
    durationMinutes: 30,
    interviewTypes: [],
    csSubTopics: [],
    resumeFile: null,
    isLoading: false,
    isSubmitStep: false,
    canNext: vi.fn(() => false),
    goNext: vi.fn(),
    goPrev: vi.fn(),
    handlePositionSelect: vi.fn(),
    handleTechStackSelect: vi.fn(),
    handleLevelSelect: vi.fn(),
    handleDurationSelect: vi.fn(),
    handleInterviewTypeToggle: vi.fn(),
    handleCsSubTopicToggle: vi.fn(),
    handleResumeFile: vi.fn(),
    handleSubmit: vi.fn(),
  }),
}))

// Question sets
vi.mock('@/hooks/use-question-sets', () => ({
  useQuestionSetFeedback: () => ({ data: undefined, isLoading: true }),
  useQuestionsWithAnswers: () => ({ data: undefined, isLoading: true }),
  // useAllQuestionSetStatuses returns an array of query results (useQueries pattern)
  useAllQuestionSetStatuses: () => [],
  useRetryAnalysis: () => ({ mutate: vi.fn(), isPending: false }),
}))

// Review bookmarks
vi.mock('@/hooks/use-review-bookmarks', () => ({
  useReviewBookmarkList: () => ({ data: undefined, isLoading: true }),
  useBookmarkExistsForInterview: () => ({ data: undefined, isLoading: true }),
  useCreateBookmark: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteBookmark: () => ({ mutate: vi.fn(), isPending: false }),
}))

// Service feedback
vi.mock('@/hooks/use-service-feedback', () => ({
  useFeedbackNeedCheck: () => ({ data: undefined }),
  useAdminFeedbacks: () => ({ data: undefined, isLoading: true }),
  useSubmitFeedback: () => ({ mutate: vi.fn(), isPending: false }),
  useSubmitServiceFeedback: () => ({ mutate: vi.fn(), isPending: false }),
  useVerifyAdminPassword: () => ({ mutate: vi.fn(), isPending: false }),
}))

// Interview session / media / store
vi.mock('@/hooks/use-media-stream', () => ({
  useMediaStream: () => ({ stream: null, error: null, isLoading: false }),
}))

vi.mock('@/hooks/use-media-recorder', () => ({
  useMediaRecorder: () => ({
    startRecording: vi.fn(),
    stopRecording: vi.fn(),
    isRecording: false,
    recordedChunks: [],
  }),
}))

vi.mock('@/hooks/use-interview-session', () => ({
  useInterviewSession: () => ({
    currentQuestion: null,
    isLoading: false,
    startSession: vi.fn(),
    submitAnswer: vi.fn(),
  }),
}))

vi.mock('@/hooks/use-interview-exit-guard', () => ({
  useInterviewExitGuard: () => ({ blocked: false, dismiss: vi.fn() }),
}))

vi.mock('@/hooks/use-device-test', () => ({
  useDeviceTest: () => ({
    videoRef: { current: null },
    isCameraOn: false,
    isMicOn: false,
    toggleCamera: vi.fn(),
    toggleMic: vi.fn(),
  }),
}))

vi.mock('@/hooks/use-feedback-sync', () => ({
  useFeedbackSync: () => undefined,
}))

vi.mock('@/stores/interview-store', () => {
  const storeState = {
    phase: 'preparing',
    interviewId: null,
    setPhase: vi.fn(),
    reset: vi.fn(),
    currentQuestionIndex: 0,
    questions: [],
  }
  const useInterviewStore = vi.fn(() => storeState) as unknown as typeof import('@/stores/interview-store').useInterviewStore
  // Zustand stores expose getState() as a static method
  ;(useInterviewStore as unknown as { getState: () => typeof storeState }).getState = () => storeState
  return {
    useInterviewStore,
    MAX_FOLLOWUP_ROUNDS: 2,
  }
})

// TanStack Query — apiClient
vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: vi.fn().mockResolvedValue({ data: null }),
    post: vi.fn().mockResolvedValue({ data: null }),
    put: vi.fn().mockResolvedValue({ data: null }),
    delete: vi.fn().mockResolvedValue({ data: null }),
  },
  ApiError: class ApiError extends Error {
    status: number
    constructor(message: string, status: number) {
      super(message)
      this.status = status
    }
  },
}))

// coach mark
vi.mock('@/components/feedback/review-coach-mark', () => ({
  ReviewTutorialProvider: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
  ReviewTutorialStack: () => null,
}))

// ── Providers wrapper ─────────────────────────────────────────────────────────

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })
}

interface WrapperProps {
  children: React.ReactNode
  initialPath?: string
  routePath?: string
}

function AllProviders({ children, initialPath = '/', routePath = '/' }: WrapperProps) {
  const queryClient = makeQueryClient()
  return (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path={routePath} element={children} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

function renderPage(ui: React.ReactElement, initialPath = '/', routePath = '/') {
  return render(
    <AllProviders initialPath={initialPath} routePath={routePath}>
      {ui}
    </AllProviders>,
  )
}

// ── axe helper ────────────────────────────────────────────────────────────────

/**
 * critical/serious 위반은 실패, minor/moderate는 console.warn
 */
async function assertA11y(container: HTMLElement) {
  const results = await axe(container, {
    runOnly: {
      type: 'tag',
      values: ['wcag2a', 'wcag2aa', 'wcag21aa', 'best-practice'],
    },
  })

  const critical = results.violations.filter((v) =>
    ['critical', 'serious'].includes(v.impact ?? ''),
  )
  const minor = results.violations.filter((v) =>
    ['minor', 'moderate'].includes(v.impact ?? ''),
  )

  if (minor.length > 0) {
    console.warn(
      `[a11y] minor/moderate 위반 ${minor.length}건 (백로그 이관):`,
      minor.map((v) => `${v.id} — ${v.description}`),
    )
  }

  // critical/serious만 실패 처리
  if (critical.length > 0) {
    const messages = critical
      .map((v) => `  [${v.impact}] ${v.id}: ${v.description}`)
      .join('\n')
    throw new Error(`critical/serious a11y 위반 ${critical.length}건:\n${messages}`)
  }
}

// ── Page imports ──────────────────────────────────────────────────────────────

import { HomePage } from '@/pages/home-page'
import { DashboardPage } from '@/pages/dashboard-page'
import { InterviewSetupPage } from '@/pages/interview-setup-page'
import { InterviewReadyPage } from '@/pages/interview-ready-page'
import { InterviewPage } from '@/pages/interview-page'
import { InterviewFeedbackPage } from '@/pages/interview-feedback-page'
import { InterviewAnalysisPage } from '@/pages/interview-analysis-page'
import { ReviewListPage } from '@/pages/review-list-page'
import { AboutPage } from '@/pages/about-page'
import { AdminFeedbacksPage } from '@/pages/admin-feedbacks-page'
import { FaqPage } from '@/pages/faq-page'
import { NotFoundPage } from '@/pages/not-found-page'
import { PrivacyPolicyPage } from '@/pages/privacy-policy-page'
// guide pages (3개 중 대표 1개로 축약 — guide 폴더 내 페이지)
import { AiMockInterviewGuidePage } from '@/pages/guide/ai-mock-interview-page'

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('a11y smoke — 14 pages', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('home: critical/serious 위반 0건', async () => {
    const { container } = renderPage(<HomePage />)
    await assertA11y(container)
  })

  it('dashboard: critical/serious 위반 0건', async () => {
    const { container } = renderPage(<DashboardPage />, '/dashboard', '/dashboard')
    await assertA11y(container)
  })

  it('interview-setup: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <InterviewSetupPage />,
      '/interview/setup',
      '/interview/setup',
    )
    await assertA11y(container)
  })

  it('interview-ready: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <InterviewReadyPage />,
      '/interview/1/ready',
      '/interview/:id/ready',
    )
    await assertA11y(container)
  })

  it('interview-page: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <InterviewPage />,
      '/interview/1',
      '/interview/:id',
    )
    await assertA11y(container)
  })

  it('interview-feedback: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <InterviewFeedbackPage />,
      '/interview/abc123/feedback',
      '/interview/:publicId/feedback',
    )
    await assertA11y(container)
  })

  it('interview-analysis: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <InterviewAnalysisPage />,
      '/interview/abc123/analysis',
      '/interview/:publicId/analysis',
    )
    await assertA11y(container)
  })

  it('review-list: critical/serious 위반 0건', async () => {
    const { container } = renderPage(<ReviewListPage />, '/review', '/review')
    await assertA11y(container)
  })

  it('about: critical/serious 위반 0건', async () => {
    const { container } = renderPage(<AboutPage />, '/about', '/about')
    await assertA11y(container)
  })

  it('admin-feedbacks: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <AdminFeedbacksPage />,
      '/admin/feedbacks',
      '/admin/feedbacks',
    )
    await assertA11y(container)
  })

  it('faq: critical/serious 위반 0건', async () => {
    const { container } = renderPage(<FaqPage />, '/faq', '/faq')
    await assertA11y(container)
  })

  it('guide/ai-mock-interview: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <AiMockInterviewGuidePage />,
      '/guide/ai-mock-interview',
      '/guide/ai-mock-interview',
    )
    await assertA11y(container)
  })

  it('privacy-policy: critical/serious 위반 0건', async () => {
    const { container } = renderPage(
      <PrivacyPolicyPage />,
      '/privacy-policy',
      '/privacy-policy',
    )
    await assertA11y(container)
  })

  it('not-found: critical/serious 위반 0건', async () => {
    const { container } = renderPage(<NotFoundPage />, '/404', '/404')
    await assertA11y(container)
  })
})
