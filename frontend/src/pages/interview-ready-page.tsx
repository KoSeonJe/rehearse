import { useParams, useNavigate } from 'react-router-dom'
import { BackLink } from '@/components/ui/back-link'
import { Logo } from '@/components/ui/logo'
import { DeviceTestSection } from '@/components/interview/device-test-section'
import { Character } from '@/components/ui/character'
import { useInterview, useUpdateInterviewStatus, useRetryQuestions } from '@/hooks/use-interviews'
import { useDeviceTest } from '@/hooks/use-device-test'
import {
  LEVEL_LABELS,
  INTERVIEW_TYPE_LABELS,
  CS_SUB_TOPIC_LABELS,
} from '@/constants/interview-labels'
import type { CsSubTopic } from '@/types/interview'

export const InterviewReadyPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: response, isLoading, isError, error } = useInterview(id ?? '')
  const updateStatus = useUpdateInterviewStatus()
  const retryQuestions = useRetryQuestions()
  const {
    state,
    micLevel,
    videoRef,
    allPassed,
    startCameraTest,
    startMicTest,
    startSpeakerTest,
    confirmSpeaker,
    resetMicTest,
    resetSpeakerTest,
  } = useDeviceTest()

  const interview = response?.data

  const questionGenStatus = interview?.questionGenerationStatus
  const isQuestionReady = questionGenStatus === 'COMPLETED'
  const isQuestionFailed = questionGenStatus === 'FAILED'
  const isQuestionGenerating = questionGenStatus === 'PENDING' || questionGenStatus === 'GENERATING'
  const canStart = allPassed && isQuestionReady

  const handleStartInterview = () => {
    if (!interview) return
    updateStatus.mutate(
      { id: interview.id, data: { status: 'IN_PROGRESS' } },
      { onSuccess: () => navigate(`/interview/${interview.id}/conduct`) },
    )
  }

  const handleRetryQuestions = () => {
    if (!interview) return
    retryQuestions.mutate(interview.id)
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

      <main className="mx-auto max-w-3xl px-5 pb-32 pt-16 md:px-8">
        {/* Intro + Tags */}
        <section className="mb-12">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
            Device Check
          </p>
          <h1 className="text-4xl font-extrabold tracking-tighter text-text-primary sm:text-5xl">
            장치를 확인하고<br />
            <span className="text-accent">시작하세요.</span>
          </h1>

          {interview && (
            <div className="mt-6 flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
              <span className="shrink-0 rounded-full bg-surface px-4 py-2 text-xs font-bold text-text-secondary">
                {interview.position}
              </span>
              <span className="shrink-0 rounded-full bg-surface px-4 py-2 text-xs font-bold text-text-secondary">
                {LEVEL_LABELS[interview.level].label}
              </span>
              {interview.interviewTypes.map((type) => (
                <span key={type} className="shrink-0 rounded-full bg-surface px-4 py-2 text-xs font-bold text-text-secondary">
                  {INTERVIEW_TYPE_LABELS[type].label}
                </span>
              ))}
              {interview.csSubTopics?.map((topic) => (
                <span key={topic} className="shrink-0 rounded-full bg-accent/10 px-4 py-2 text-xs font-bold text-accent">
                  {CS_SUB_TOPIC_LABELS[topic as CsSubTopic] ?? topic}
                </span>
              ))}
              {interview.durationMinutes && (
                <span className="shrink-0 rounded-full bg-accent/10 px-4 py-2 text-xs font-bold text-accent">
                  {interview.durationMinutes}분
                </span>
              )}
            </div>
          )}
        </section>

        {/* Question Generation Status */}
        {!isLoading && interview && (
          <section className="mb-8">
            <div className="mb-4 flex items-center gap-2 px-1">
              <h2 className="text-xs font-black uppercase tracking-widest text-text-tertiary">
                질문 준비
              </h2>
            </div>
            <div className="rounded-2xl border border-border bg-surface/50 p-5">
              {isQuestionGenerating && (
                <div className="flex items-center gap-3">
                  <div className="h-5 w-5 animate-spin rounded-full border-2 border-accent border-t-transparent" />
                  <span className="text-sm font-bold text-text-secondary">
                    AI가 면접 질문을 생성하고 있어요...
                  </span>
                </div>
              )}
              {isQuestionReady && (
                <div className="flex items-center gap-3">
                  <svg className="h-5 w-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                  <span className="text-sm font-bold text-text-primary">
                    질문 준비 완료 ({interview.questionSets.length}개 질문세트)
                  </span>
                </div>
              )}
              {isQuestionFailed && (
                <div className="flex flex-col gap-3">
                  <div className="flex items-center gap-3">
                    <svg className="h-5 w-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                    <span className="text-sm font-bold text-red-600">
                      질문 생성에 실패했어요
                    </span>
                  </div>
                  {interview.failureReason && (
                    <p className="ml-8 text-xs text-text-tertiary">{interview.failureReason}</p>
                  )}
                  <button
                    onClick={handleRetryQuestions}
                    disabled={retryQuestions.isPending}
                    className="ml-8 w-fit rounded-xl bg-accent/10 px-4 py-2 text-xs font-bold text-accent transition-all hover:bg-accent/20 active:scale-95 disabled:opacity-50"
                  >
                    {retryQuestions.isPending ? '재시도 중...' : '다시 시도하기'}
                  </button>
                </div>
              )}
            </div>
          </section>
        )}

        {/* Device Test — Main Content */}
        {!isLoading && (
          <section>
            <div className="mb-6 flex items-center gap-2 px-1">
              <h2 className="text-xs font-black uppercase tracking-widest text-text-tertiary">
                장치 테스트
              </h2>
              <span className="text-[10px] font-bold text-text-tertiary">
                — 3개 모두 통과해야 시작할 수 있어요
              </span>
            </div>

            <DeviceTestSection
              state={state}
              micLevel={micLevel}
              videoRef={videoRef}
              onCameraTest={startCameraTest}
              onMicTest={startMicTest}
              onSpeakerTest={startSpeakerTest}
              onConfirmSpeaker={confirmSpeaker}
              onResetMic={resetMicTest}
              onResetSpeaker={resetSpeakerTest}
            />
          </section>
        )}

        {/* Start Button */}
        {!isLoading && (
          <div className="mt-16">
            <button
              onClick={handleStartInterview}
              disabled={!canStart || updateStatus.isPending}
              className="h-18 w-full rounded-[24px] bg-accent py-5 text-xl font-black text-white shadow-lg shadow-accent/20 transition-all active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
            >
              {updateStatus.isPending ? '시작하는 중...' : '면접 시작하기'}
            </button>
            {!canStart && (
              <p className="mt-3 text-center text-xs font-bold text-text-tertiary">
                {!isQuestionReady && !isQuestionFailed
                  ? '질문이 생성되는 동안 장치 테스트를 진행해주세요'
                  : !allPassed
                    ? '카메라, 마이크, 스피커 테스트를 모두 완료해주세요'
                    : '질문 생성이 완료되어야 시작할 수 있어요'}
              </p>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
