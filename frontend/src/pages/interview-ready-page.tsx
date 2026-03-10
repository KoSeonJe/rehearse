import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { BackLink } from '@/components/ui/back-link'
import { Logo } from '@/components/ui/logo'
import { QuestionCardSkeleton } from '@/components/interview/question-card-skeleton'
import { DeviceTestSection } from '@/components/interview/device-test-section'
import { Character } from '@/components/ui/character'
import {
  useInterview,
  useCreateInterview,
  useUpdateInterviewStatus,
} from '@/hooks/use-interviews'
import { useDeviceTest } from '@/hooks/use-device-test'
import { LEVEL_LABELS, INTERVIEW_TYPE_LABELS } from '@/types/interview'

export const InterviewReadyPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [showDeviceTest, setShowDeviceTest] = useState(false)

  const { data: response, isLoading, isError, error } = useInterview(id ?? '')
  const updateStatus = useUpdateInterviewStatus()
  const createInterview = useCreateInterview()
  const { permissions, micLevel, videoRef } = useDeviceTest(showDeviceTest)

  const interview = response?.data

  const handleStartInterview = () => {
    if (!interview) return
    updateStatus.mutate(
      { id: interview.id, data: { status: 'IN_PROGRESS' } },
      { onSuccess: () => navigate(`/interview/${interview.id}/conduct`) },
    )
  }

  const handleRegenerateQuestions = () => {
    if (!interview) return
    createInterview.mutate(
      {
        request: {
          position: interview.position,
          level: interview.level,
          interviewTypes: interview.interviewTypes,
        },
      },
      {
        onSuccess: (newResponse) => {
          navigate(`/interview/${newResponse.data.id}/ready`, { replace: true })
        },
      },
    )
  }

  if (isError) {
    const is404 = error?.message?.includes('404')
    return (
      <div className="min-h-screen bg-white">
        <header className="px-5 pt-8 md:px-8">
          <BackLink to="/interview/setup" />
        </header>
        <main className="mx-auto max-w-2xl px-5 pt-24 text-center">
          <Character mood="confused" size={120} className="mx-auto mb-8" />
          <h1 className="text-3xl font-extrabold text-text-primary">문제가 발생했어요</h1>
          <p className="mt-4 text-text-secondary">
            {is404 ? '세션을 찾을 수 없습니다.' : '일시적인 오류가 발생했습니다.'}
          </p>
          <div className="mt-12 flex flex-col gap-3">
            <button
              onClick={() => navigate(is404 ? '/' : 0 as never)}
              className="h-16 rounded-[24px] bg-accent font-black text-white transition-all active:scale-95"
            >
              {is404 ? '홈으로 돌아가기' : '다시 시도하기'}
            </button>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="flex items-center justify-between px-5 pt-8 md:px-8">
        <div className="flex items-center gap-2">
          <Logo size={80} />
          <span className="text-xl font-extrabold tracking-tight text-text-primary">
            리허설
          </span>
        </div>
        <BackLink to="/interview/setup" />
      </header>

      <main className="mx-auto max-w-2xl px-5 pb-32 pt-16 md:px-8">
        {/* Intro */}
        <section className="mb-12">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
            Ready to Start
          </p>
          <h1 className="text-4xl font-extrabold tracking-tighter text-text-primary sm:text-5xl">
            준비가 모두<br />
            <span className="text-accent">끝났습니다.</span>
          </h1>

          {interview && (
            <div className="mt-8 flex flex-wrap gap-2">
              <span className="rounded-full bg-surface px-4 py-2 text-xs font-bold text-text-secondary">
                {interview.position}
              </span>
              <span className="rounded-full bg-surface px-4 py-2 text-xs font-bold text-text-secondary">
                {LEVEL_LABELS[interview.level].label}
              </span>
              {interview.interviewTypes.map((type) => (
                <span key={type} className="rounded-full bg-surface px-4 py-2 text-xs font-bold text-text-secondary">
                  {INTERVIEW_TYPE_LABELS[type].label}
                </span>
              ))}
              {interview.durationMinutes && (
                <span className="rounded-full bg-accent/10 px-4 py-2 text-xs font-bold text-accent">
                  {interview.durationMinutes}분
                </span>
              )}
            </div>
          )}
        </section>

        {/* Questions List */}
        <section className="space-y-6">
          <div className="flex items-center justify-between px-1">
            <h2 className="text-xs font-black uppercase tracking-widest text-text-tertiary">
              면접 질문 목록
            </h2>
            <span className="text-[10px] font-bold text-text-tertiary">
              총 {interview?.questions.length || 0}개
            </span>
          </div>

          <ol className="space-y-4">
            {isLoading
              ? Array.from({ length: 3 }, (_, i) => <QuestionCardSkeleton key={i} />)
              : interview?.questions
                  .slice()
                  .sort((a, b) => a.order - b.order)
                  .map((question) => (
                    <div key={question.id} className="group relative rounded-[24px] bg-surface p-6 transition-all hover:bg-white hover:shadow-toss border border-transparent hover:border-border">
                      <div className="flex items-start gap-4">
                        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white text-[10px] font-black text-accent shadow-sm">
                          {question.order + 1}
                        </span>
                        <p className="text-[15px] font-bold leading-relaxed text-text-primary">
                          {question.content}
                        </p>
                      </div>
                    </div>
                  ))}
          </ol>
        </section>

        {/* Device Test Section */}
        {!isLoading && (
          <section className="mt-16">
            <button
              onClick={() => setShowDeviceTest((prev) => !prev)}
              className="flex w-full items-center justify-between rounded-[24px] bg-surface px-6 py-5 transition-all hover:bg-slate-100 border border-transparent hover:border-border"
            >
              <div className="flex items-center gap-3">
                <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-accent/10 text-sm">
                  {showDeviceTest ? '🎥' : '📷'}
                </span>
                <span className="text-sm font-bold text-text-primary">
                  카메라 / 마이크 테스트
                </span>
              </div>
              <span className="text-xs font-bold text-text-tertiary">
                {showDeviceTest ? '접기' : '펼치기'}
              </span>
            </button>

            {showDeviceTest && (
              <div className="mt-6 rounded-[24px] border border-border bg-white p-6 shadow-toss">
                <DeviceTestSection
                  permissions={permissions}
                  micLevel={micLevel}
                  videoRef={videoRef}
                />
              </div>
            )}
          </section>
        )}

        {/* Action Area */}
        {!isLoading && (
          <div className="mt-20 space-y-4">
            <button
              onClick={handleStartInterview}
              disabled={updateStatus.isPending}
              className="h-18 w-full rounded-[24px] bg-accent py-5 text-xl font-black text-white shadow-lg shadow-accent/20 transition-all active:scale-95 disabled:opacity-50"
            >
              {updateStatus.isPending ? '시작하는 중...' : '면접 시작하기'}
            </button>
            <button
              onClick={handleRegenerateQuestions}
              disabled={createInterview.isPending}
              className="h-16 w-full rounded-[24px] bg-surface py-4 text-base font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95 disabled:opacity-50"
            >
              {createInterview.isPending ? '다시 생성 중...' : '질문이 맘에 안 드나요? 다시 생성하기'}
            </button>
          </div>
        )}
      </main>
    </div>
  )
}
