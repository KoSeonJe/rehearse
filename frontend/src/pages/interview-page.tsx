import { useCallback, useEffect, useMemo } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useInterview, useUpdateInterviewStatus, useFollowUpQuestion } from '../hooks/use-interviews'
import useMediaStream from '../hooks/use-media-stream'
import useMediaRecorder from '../hooks/use-media-recorder'
import useSpeechRecognition from '../hooks/use-speech-recognition'
import useAudioAnalyzer from '../hooks/use-audio-analyzer'
import VideoPreview from '../components/interview/video-preview'
import QuestionDisplay from '../components/interview/question-display'
import TranscriptDisplay from '../components/interview/transcript-display'
import InterviewControls from '../components/interview/interview-controls'
import AudioLevelIndicator from '../components/interview/audio-level-indicator'
import InterviewTimer from '../components/interview/interview-timer'
import type { TranscriptSegment, VoiceEvent } from '../types/interview'

const InterviewPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const interviewId = id ?? ''

  const { data: response } = useInterview(interviewId)
  const interview = response?.data
  const updateStatus = useUpdateInterviewStatus()
  const followUpMutation = useFollowUpQuestion()

  const {
    questions,
    currentQuestionIndex,
    phase,
    startTime,
    answers,
    currentTranscript,
    setInterview,
    startRecording,
    stopRecording,
    nextQuestion,
    prevQuestion,
    setCurrentTranscript,
    addTranscript,
    addVoiceEvent,
    setVideoBlob,
    completeInterview,
    followUpQuestions,
    isFollowUpLoading,
    addFollowUpQuestion,
    setFollowUpLoading,
  } = useInterviewStore()

  const mediaStream = useMediaStream()
  const recorder = useMediaRecorder()
  const stt = useSpeechRecognition()
  const audio = useAudioAnalyzer()

  // 면접 데이터 로드
  useEffect(() => {
    if (interview && phase === 'preparing') {
      setInterview(interview.id, interview.questions)
    }
  }, [interview, phase, setInterview])

  // STT 콜백 등록
  useEffect(() => {
    stt.onFinalResult((segment: TranscriptSegment) => {
      addTranscript(segment)
    })
  }, [stt, addTranscript])

  // 음성 이벤트 콜백 등록
  useEffect(() => {
    audio.onVoiceEvent((event: VoiceEvent) => {
      addVoiceEvent(event)
    })
  }, [audio, addVoiceEvent])

  // interim text 동기화
  useEffect(() => {
    setCurrentTranscript(stt.interimText)
  }, [stt.interimText, setCurrentTranscript])

  // 카메라 시작
  const handlePrepare = useCallback(async () => {
    await mediaStream.start()
  }, [mediaStream])

  useEffect(() => {
    if (phase === 'ready' && !mediaStream.isActive) {
      handlePrepare()
    }
  }, [phase, mediaStream.isActive, handlePrepare])

  // 상태를 IN_PROGRESS로 변경
  useEffect(() => {
    if (phase === 'ready' && interview?.status === 'READY') {
      updateStatus.mutate({ id: interview.id, data: { status: 'IN_PROGRESS' } })
    }
  }, [phase, interview?.status, interviewId, updateStatus])

  // 답변 시작
  const handleStartAnswer = useCallback(() => {
    if (!mediaStream.stream) return
    startRecording()
    if (!recorder.isRecording) {
      recorder.start(mediaStream.stream)
    } else {
      recorder.resume()
    }
    stt.start(currentQuestionIndex)
    audio.start(mediaStream.stream)
  }, [mediaStream.stream, startRecording, recorder, stt, audio, currentQuestionIndex])

  // 답변 완료 + 후속질문 요청
  const handleStopAnswer = useCallback(() => {
    stopRecording()
    stt.stop()
    recorder.pause()

    // 현재 질문에 대한 답변 텍스트 수집
    const currentAnswer = useInterviewStore.getState().answers[currentQuestionIndex]
    const answerText = currentAnswer?.transcripts
      .filter((t) => t.isFinal)
      .map((t) => t.text)
      .join(' ')

    if (answerText && interview) {
      setFollowUpLoading(true)
      followUpMutation.mutate(
        {
          id: interview.id,
          data: {
            questionContent: questions[currentQuestionIndex].content,
            answerText,
          },
        },
        {
          onSuccess: (res) => {
            addFollowUpQuestion(currentQuestionIndex, res.data)
            setFollowUpLoading(false)
          },
          onError: () => {
            setFollowUpLoading(false)
          },
        },
      )
    }
  }, [stopRecording, stt, recorder, currentQuestionIndex, interview, questions, followUpMutation, addFollowUpQuestion, setFollowUpLoading])

  // 면접 종료
  const handleFinishInterview = useCallback(async () => {
    stt.stop()
    audio.stop()

    const blob = await recorder.stop()
    setVideoBlob(blob)
    completeInterview()

    updateStatus.mutate({ id: interview!.id, data: { status: 'COMPLETED' } })

    mediaStream.stop()
    navigate(`/interview/${interview!.id}/complete`)
  }, [stt, audio, recorder, setVideoBlob, completeInterview, updateStatus, interviewId, mediaStream, navigate])

  // 클린업
  useEffect(() => {
    return () => {
      mediaStream.stop()
      audio.stop()
      stt.stop()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const currentQuestion = questions[currentQuestionIndex]
  const currentAnswer = answers[currentQuestionIndex]
  const finalTexts = useMemo(
    () => currentAnswer?.transcripts.filter((t) => t.isFinal).map((t) => t.text) ?? [],
    [currentAnswer],
  )

  if (!interview || !currentQuestion) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <p className="text-slate-500">면접 데이터를 불러오는 중...</p>
      </div>
    )
  }

  if (mediaStream.error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="max-w-md space-y-4 text-center">
          <p className="text-lg font-medium text-slate-900">미디어 접근 오류</p>
          <p className="text-sm text-slate-600">{mediaStream.error}</p>
          <button
            onClick={handlePrepare}
            className="rounded-xl bg-slate-900 px-6 py-3 text-sm font-medium text-white transition-colors hover:bg-slate-800"
          >
            다시 시도
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      {/* Header */}
      <header className="border-b border-slate-200 bg-white px-6 py-4">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-lg font-bold text-slate-900">Rehearse</h1>
            <span className="text-sm text-slate-500">면접 진행 중</span>
          </div>
          <div className="flex items-center gap-4">
            {phase === 'recording' && (
              <div className="flex items-center gap-2">
                <span className="h-2 w-2 animate-pulse rounded-full bg-red-500" />
                <span className="text-xs font-medium text-red-600">녹화 중</span>
              </div>
            )}
            <InterviewTimer startTime={startTime} />
            <AudioLevelIndicator level={audio.audioLevel} />
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto w-full max-w-3xl flex-1 space-y-6 px-6 py-8">
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
          <div className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
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

export default InterviewPage
