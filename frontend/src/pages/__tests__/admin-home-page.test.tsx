import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HelmetProvider } from 'react-helmet-async'
import { MemoryRouter } from 'react-router-dom'
import { AdminHomePage } from '../admin-home-page'

const renderPage = () =>
  render(
    <HelmetProvider>
      <MemoryRouter>
        <AdminHomePage />
      </MemoryRouter>
    </HelmetProvider>,
  )

describe('AdminHomePage', () => {
  it('관리 화면 진입 링크를 렌더한다', () => {
    renderPage()

    expect(screen.getByRole('heading', { name: '관리자 메뉴' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /서비스 피드백 관리/ })).toHaveAttribute(
      'href',
      '/admin/feedbacks',
    )
    expect(screen.getByRole('link', { name: /질문 풀 관리/ })).toHaveAttribute(
      'href',
      '/admin/question-pool',
    )
  })
})
