import { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useInterview } from '../hooks/use-interviews'
import { useMediaStream } from '../hooks/use-media-stream'
import { useMediaRecorder } from '../hooks/use-media-recorder'
import { useSpeechRecognition } from '../hooks/use-speech-recognition'
import { useAudioAnalyzer } from '../hooks/use-audio-analyzer'
import { useInterviewSession } from '../hooks/use-interview-session'
import { Button } from '@/components/ui/button'
import { LogoIcon } from '@/components/ui/logo-icon'
import { Character } from '@/components/ui/character'
import { VideoPreview } from '../components/interview/video-preview'
import { QuestionDisplay } from '../components/interview/question-display'
import { TranscriptDisplay } from '../components/interview/transcript-display'
import { InterviewControls } from '../components/interview/interview-controls'
import { AudioLevelIndicator } from '../components/interview/audio-level-indicator'
import { InterviewTimer } from '../components/interview/interview-timer'

export const InterviewPage = () => {
  const { id } = useParams<{ id: string }>()
  const interviewId = id ?? ''

  const { data: response } = useInterview(interviewId)
  const interview = response?.data

  const {
    questions,
    currentQuestionIndex,
    phase,
    startTime,
    answers,
    currentTranscript,
    nextQuestion,
    prevQuestion,
    followUpQuestions,
    isFollowUpLoading,
  } = useInterviewStore()

  const mediaStream = useMediaStream()
  const recorder = useMediaRecorder()
  const stt = useSpeechRecognition()
  const audio = useAudioAnalyzer()

  const {
    handlePrepare,
    handleStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
  } = useInterviewSession({
    interviewId,
    interview,
    mediaStream,
    recorder,
    stt,
    audio,
  })

  const currentQuestion = questions[currentQuestionIndex]
  const currentAnswer = answers[currentQuestionIndex]
  const finalTexts = useMemo(
    () => currentAnswer?.transcripts.filter((t) => t.isFinal).map((t) => t.text) ?? [],
    [currentAnswer],
  )

  if (!interview || !currentQuestion) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-text-secondary">면접 데이터를 불러오는 중...</p>
      </div>
    )
  }

  if (mediaStream.error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="max-w-md space-y-4 text-center">
          <Character mood="confused" size={80} className="mx-auto mb-4" />
          <p className="text-lg font-medium text-text-primary">미디어 접근 오류</p>
          <p className="text-sm text-text-secondary">{mediaStream.error}</p>
          <Button variant="primary" onClick={handlePrepare}>
            다시 시도
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Header */}
      <header className="border-b border-border bg-surface px-4 py-4 sm:px-6">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <div className="flex items-center gap-3">
            <LogoIcon />
            <h1 className="text-lg font-bold text-text-primary">Rehearse</h1>
            <span className="hidden text-sm text-text-secondary sm:inline">면접 진행 중</span>
          </div>
          <div className="flex items-center gap-4">
            {phase === 'recording' && (
              <div className="flex items-center gap-2">
                <span className="h-2 w-2 animate-pulse rounded-full bg-error" />
                <span className="text-xs font-medium text-error">녹화 중</span>
              </div>
            )}
            <InterviewTimer startTime={startTime} />
            <AudioLevelIndicator level={audio.audioLevel} />
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto w-full max-w-3xl flex-1 space-y-6 px-4 py-6 sm:px-6 sm:py-8">
        <QuestionDisplay
          question={currentQuestion}
          currentIndex={currentQuestionIndex}
          totalCount={questions.length}
          followUp={followUpQuestions.get(currentQuestionIndex)}
          isFollowUpLoading={isFollowUpLoading}
        />

        <VideoPreview stream={mediaStream.stream} isRecording={phase === 'recording'} />

        <TranscriptDisplay interimText={currentTranscript} finalTexts={finalTexts} />

        {!stt.isSupported && (
          <div className="rounded-card bg-warning-light px-4 py-3 text-sm text-warning">
            이 브라우저는 음성 인식을 지원하지 않습니다. Chrome 브라우저를 권장합니다.
          </div>
        )}

        <InterviewControls
          phase={phase}
          currentIndex={currentQuestionIndex}
          totalQuestions={questions.length}
          onStartAnswer={handleStartAnswer}
          onStopAnswer={handleStopAnswer}
          onNextQuestion={nextQuestion}
          onPrevQuestion={prevQuestion}
          onFinishInterview={handleFinishInterview}
        />
      </main>
    </div>
  )
}
