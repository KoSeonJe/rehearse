import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useInterview } from '../hooks/use-interviews'
import { useMediaStream } from '../hooks/use-media-stream'
import { useMediaRecorder } from '../hooks/use-media-recorder'
import { useSpeechRecognition } from '../hooks/use-speech-recognition'
import { useAudioAnalyzer } from '../hooks/use-audio-analyzer'
import { useInterviewSession } from '../hooks/use-interview-session'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import { VideoPreview } from '../components/interview/video-preview'
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
    autoTransitionMessage,
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
    isVadActive,
  } = useInterviewSession({
    interviewId,
    interview,
    mediaStream,
    recorder,
    stt,
    audio,
  })

  const [timeWarning, setTimeWarning] = useState(false)

  const currentQuestion = questions[currentQuestionIndex]
  const currentAnswer = answers[currentQuestionIndex]
  const finalTexts = useMemo(
    () => currentAnswer?.transcripts.filter((t) => t.isFinal).map((t) => t.text) ?? [],
    [currentAnswer],
  )

  if (!interview || !currentQuestion) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <div className="text-center space-y-4">
          <div className="h-1 w-24 bg-accent/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-accent animate-progress-loading" />
          </div>
          <p className="font-mono text-[10px] font-black uppercase tracking-widest text-accent">Initializing AI Studio</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-white text-text-primary">
      {/* Header */}
      <header className="px-6 py-6 flex items-center justify-between border-b border-border">
        <div className="flex items-center gap-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent shadow-lg shadow-accent/20">
            <Logo size={24} />
          </div>
          <div>
            <h1 className="text-sm font-black uppercase tracking-widest text-text-primary">AI Interview Session</h1>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="h-1.5 w-1.5 rounded-full bg-success animate-pulse" />
              <span className="text-[10px] font-bold text-success uppercase tracking-tighter">Live Connection Established</span>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-6">
          <InterviewTimer
            startTime={startTime}
            durationMinutes={interview.durationMinutes}
            onTimeWarning={() => setTimeWarning(true)}
            onTimeExpired={() => handleFinishInterview()}
          />
          <div className="h-8 w-[1px] bg-border" />
          <AudioLevelIndicator level={audio.audioLevel} />
        </div>
      </header>

      {/* Dual-View Layout */}
      <main className="flex-1 flex flex-col lg:flex-row gap-6 p-6 overflow-hidden">
        {/* Left: AI Interviewer Section */}
        <div className="flex-1 relative rounded-[32px] overflow-hidden bg-surface border border-border shadow-toss-lg">
          <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-white to-indigo-50/30" />

          {/* AI Character */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="relative z-10 text-center">
              <Character mood={phase === 'recording' ? 'happy' : 'default'} size={240} />
              <div className="mt-8 space-y-2">
                <p className="font-mono text-[10px] font-black text-accent uppercase tracking-[0.3em]">AI Interviewer</p>
                <div className="flex justify-center gap-1">
                  {[1, 2, 3].map(i => <div key={i} className="h-1 w-4 bg-accent/20 rounded-full" />)}
                </div>
              </div>
            </div>
          </div>

          {/* HUD Overlay Elements */}
          <div className="absolute inset-0 z-20 pointer-events-none p-8 flex flex-col justify-between">
            <div className="flex justify-between items-start">
              <div className="rounded-2xl bg-white/80 backdrop-blur-md border border-border p-4 shadow-toss">
                <p className="font-mono text-[9px] text-text-tertiary uppercase tracking-widest mb-3">System Analysis</p>
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <div className="h-1 w-12 bg-slate-100 rounded-full overflow-hidden">
                      <div className="h-full bg-accent w-[85%]" />
                    </div>
                    <span className="text-[9px] font-bold text-text-secondary">Emotional Pulse</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="h-1 w-12 bg-slate-100 rounded-full overflow-hidden">
                      <div className="h-full bg-success w-[92%]" />
                    </div>
                    <span className="text-[9px] font-bold text-text-secondary">Logic Consistency</span>
                  </div>
                </div>
              </div>
              <div className="rounded-full bg-accent px-4 py-1.5 shadow-lg shadow-accent/20">
                <span className="text-[10px] font-black uppercase tracking-widest text-white">Question {currentQuestionIndex + 1}</span>
              </div>
            </div>

            <div className="max-w-xl mx-auto w-full">
              {/* Time Warning Banner */}
              {timeWarning && (
                <div className="mb-4 flex justify-center">
                  <div className="rounded-full bg-warning/10 border border-warning/20 px-5 py-2 shadow-toss animate-fade-in">
                    <span className="text-sm font-bold text-warning">마무리할 시간입니다 - 곧 면접이 종료됩니다</span>
                  </div>
                </div>
              )}
              {/* Auto Transition Toast */}
              {autoTransitionMessage && (
                <div className="mb-4 flex justify-center">
                  <div className="rounded-full bg-accent/10 border border-accent/20 px-5 py-2 shadow-toss animate-fade-in">
                    <span className="text-sm font-bold text-accent">{autoTransitionMessage}</span>
                  </div>
                </div>
              )}
              <div className="rounded-[24px] bg-white border border-border p-8 shadow-toss">
                <p className="text-xl md:text-2xl font-extrabold leading-relaxed text-center tracking-tight text-text-primary">
                  {currentQuestion.content}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Right: User Preview & Transcript */}
        <div className="w-full lg:w-[400px] flex flex-col gap-6">
          {/* User Video */}
          <div className="relative aspect-video rounded-[32px] overflow-hidden bg-surface border border-border shadow-toss">
            <VideoPreview stream={mediaStream.stream} isRecording={phase === 'recording'} />
            <div className="absolute top-4 right-4">
              <div className="flex items-center gap-2 px-3 py-1.5 bg-white/80 backdrop-blur-md rounded-full border border-border">
                <div className={`h-1.5 w-1.5 rounded-full ${phase === 'recording' ? 'bg-error animate-pulse' : 'bg-text-tertiary'}`} />
                <span className="text-[10px] font-black uppercase tracking-tighter text-text-primary">{phase === 'recording' ? 'User Live' : 'Standby'}</span>
              </div>
            </div>
          </div>

          {/* Transcript Area */}
          <div className="flex-1 rounded-[32px] bg-surface border border-border p-6 flex flex-col gap-4 overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-black text-text-tertiary uppercase tracking-widest">Real-time Transcript</span>
              <div className="flex gap-1">
                {[1, 2, 3].map(i => <div key={i} className="h-1 w-1 rounded-full bg-text-tertiary/30" />)}
              </div>
            </div>
            <div className="flex-1 overflow-y-auto custom-scrollbar pr-2">
              <TranscriptDisplay interimText={currentTranscript} finalTexts={finalTexts} />
            </div>
          </div>
        </div>
      </main>

      {/* Floating Controls */}
      <div className="p-6">
        <InterviewControls
          phase={phase}
          currentIndex={currentQuestionIndex}
          totalQuestions={questions.length}
          isVadActive={isVadActive}
          onStartAnswer={handleStartAnswer}
          onStopAnswer={handleStopAnswer}
          onNextQuestion={nextQuestion}
          onPrevQuestion={prevQuestion}
          onFinishInterview={handleFinishInterview}
        />
      </div>
    </div>
  )
}
