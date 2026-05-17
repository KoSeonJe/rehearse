import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HelmetProvider } from 'react-helmet-async'
import { AdminQuestionPoolPage } from '../admin-question-pool-page'

const mutate = vi.fn()
const mockUseAdminQuestionPools = vi.fn()

vi.mock('@/hooks/use-admin-question-pool', () => ({
  useAdminQuestionPools: (...args: unknown[]) => mockUseAdminQuestionPools(...args),
  useCreateAdminQuestionPool: () => ({ mutate, isPending: false }),
}))

const renderPage = () =>
  render(
    <HelmetProvider>
      <AdminQuestionPoolPage />
    </HelmetProvider>,
  )

describe('AdminQuestionPoolPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAdminQuestionPools.mockReturnValue({
      data: {
        data: {
          content: [
            {
              id: 1,
              cacheKey: 'JUNIOR:CS_FUNDAMENTAL',
              content: '프로세스와 스레드의 차이는 무엇인가요?',
              ttsContent: '프로세스와 스레드의 차이는 무엇인가요?',
              category: '운영체제',
              bestAnswer: '프로세스는 독립 주소 공간을 가집니다.',
              isActive: true,
              createdAt: '2026-05-16T10:30:00',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          size: 20,
          number: 0,
        },
      },
      isLoading: false,
    })
  })

  it('질문 풀 목록을 렌더한다', () => {
    renderPage()

    expect(screen.getByRole('heading', { name: '질문 풀 관리' })).toBeInTheDocument()
    expect(screen.getAllByText('JUNIOR:CS_FUNDAMENTAL')[0]).toBeInTheDocument()
    expect(screen.getAllByText('운영체제')[0]).toBeInTheDocument()
    expect(screen.getAllByText('프로세스와 스레드의 차이는 무엇인가요?')[0]).toBeInTheDocument()
    expect(screen.getAllByText('프로세스는 독립 주소 공간을 가집니다.')[0]).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: '세부 주제' })).toHaveClass('whitespace-nowrap')
    expect(screen.getByRole('columnheader', { name: '상태' })).toHaveClass('whitespace-nowrap')
    expect(screen.getByRole('cell', { name: '활성' })).toHaveClass('whitespace-nowrap')
  })

  it('검색 버튼을 누르면 입력한 필터로 목록을 다시 조회한다', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('캐시 키'), 'JUNIOR')
    await user.type(screen.getByLabelText('세부 주제'), '운영체제')
    await user.type(screen.getByLabelText('검색어'), '스레드')
    await user.selectOptions(screen.getByLabelText('활성 상태'), 'true')
    await user.click(screen.getByRole('button', { name: '검색' }))

    expect(mockUseAdminQuestionPools).toHaveBeenLastCalledWith(
      {
        cacheKey: 'JUNIOR',
        category: '운영체제',
        isActive: 'true',
        keyword: '스레드',
      },
      0,
      20,
    )
  })

  it('선택한 조건으로 cacheKey를 자동 생성해 새 질문 생성 요청을 보낸다', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: '새 질문 추가' }))
    await user.selectOptions(screen.getByLabelText('면접 유형'), 'LANGUAGE_FRAMEWORK')
    await user.selectOptions(screen.getByLabelText('레벨'), 'JUNIOR')
    await user.selectOptions(screen.getByLabelText('직무'), 'BACKEND')
    await user.selectOptions(screen.getByLabelText('기술스택'), 'JAVA_SPRING')
    await user.selectOptions(screen.getAllByLabelText('세부 주제')[1], 'JPA')
    expect(screen.getByLabelText('생성될 캐시 키')).toHaveValue('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK')

    await user.type(screen.getByLabelText('질문 본문'), '가상 메모리를 설명해주세요.')
    await user.type(screen.getByLabelText('모범답안'), '가상 메모리는 보조기억장치를 활용합니다.')
    await user.click(screen.getByRole('button', { name: '추가' }))

    expect(mutate).toHaveBeenCalledWith(
      {
        cacheKey: 'BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK',
        content: '가상 메모리를 설명해주세요.',
        ttsContent: '',
        category: 'JPA',
        bestAnswer: '가상 메모리는 보조기억장치를 활용합니다.',
      },
      expect.any(Object),
    )
  })

  it('세부 주제에서 기타를 선택하면 직접 입력한 값을 생성 요청에 보낸다', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: '새 질문 추가' }))
    await user.selectOptions(screen.getAllByLabelText('세부 주제')[1], 'OTHER')
    await user.type(screen.getByLabelText('직접 입력할 세부 주제'), '컴파일러')
    await user.type(screen.getByLabelText('질문 본문'), '컴파일 과정을 설명해주세요.')
    await user.click(screen.getByRole('button', { name: '추가' }))

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        category: '컴파일러',
        content: '컴파일 과정을 설명해주세요.',
      }),
      expect.any(Object),
    )
  })

  it('공통 유형은 직무와 기술스택 없이 레벨과 유형만으로 cacheKey를 만든다', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: '새 질문 추가' }))

    expect(screen.getByLabelText('생성될 캐시 키')).toHaveValue('JUNIOR:CS_FUNDAMENTAL')
    expect(screen.queryByLabelText('직무')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('기술스택')).not.toBeInTheDocument()
  })
})
