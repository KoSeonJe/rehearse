import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useInterview } from '../hooks/use-interviews'
import { useMediaStream } from '../hooks/use-media-stream'
import { useMediaRecorder } from '../hooks/use-media-recorder'
import { useSpeechRecognition } from '../hooks/use-speech-recognition'
import { useAudioAnalyzer } from '../hooks/use-audio-analyzer'
import { useInterviewSession } from '../hooks/use-interview-session'
import { Logo } from '@/components/ui/logo'
import { AudioWaveform } from '../components/interview/audio-waveform'
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

  // 개별 selector로 구독 — currentTranscript/answers 등 빈번한 변경에 의한 리렌더 차단
  const questions = useInterviewStore((s) => s.questions)
  const currentQuestionIndex = useInterviewStore((s) => s.currentQuestionIndex)
  const phase = useInterviewStore((s) => s.phase)
  const startTime = useInterviewStore((s) => s.startTime)
  const greetingCompleted = useInterviewStore((s) => s.greetingCompleted)
  const autoTransitionMessage = useInterviewStore((s) => s.autoTransitionMessage)

  const mediaStream = useMediaStream()
  const recorder = useMediaRecorder()
  const stt = useSpeechRecognition()
  const audio = useAudioAnalyzer()

  const {
    handleStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
    isTtsSpeaking,
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

  const isGreeting = !greetingCompleted && (phase === 'greeting' || (phase === 'recording' && currentQuestionIndex === 0))

  if (!interview || (!currentQuestion && !isGreeting)) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <div className="text-center space-y-4">
          <div className="h-1 w-24 bg-accent/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-accent animate-progress-loading" />
          </div>
          <p className="font-mono text-[10px] font-black uppercase tracking-widest text-accent">AI 스튜디오 준비 중</p>
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
            <h1 className="text-base font-black uppercase tracking-widest text-text-primary">AI 모의 면접</h1>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="h-1.5 w-1.5 rounded-full bg-success animate-pulse" />
              <span className="text-xs font-bold text-success uppercase tracking-tighter">실시간 연결됨</span>
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
          <AudioLevelIndicator audioLevelRef={audio.audioLevelRef} />
        </div>
      </header>

      {/* Dual-View Layout */}
      <main className="flex-1 flex flex-col lg:flex-row gap-6 p-6 overflow-hidden">
        {/* Left: AI Interviewer Section */}
        <div className="flex-1 relative rounded-[32px] overflow-hidden bg-surface border border-border shadow-toss-lg">
          <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-white to-indigo-50/30" />

          {/* AI Waveform */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="relative z-10 text-center">
              <AudioWaveform isSpeaking={isTtsSpeaking} size={240} />
              <div className="mt-8 space-y-2">
                <p className="font-mono text-[10px] font-black text-accent uppercase tracking-[0.3em]">AI 면접관</p>
                <div className="flex justify-center gap-1">
                  {[1, 2, 3].map(i => <div key={i} className="h-1 w-4 bg-accent/20 rounded-full" />)}
                </div>
              </div>
            </div>
          </div>

          {/* HUD Overlay Elements */}
          <div className="absolute inset-0 z-20 pointer-events-none p-8 flex flex-col justify-between">
            <div className="flex justify-end items-start">
              <div className="rounded-full bg-accent px-4 py-1.5 shadow-lg shadow-accent/20">
                <span className="text-[10px] font-black uppercase tracking-widest text-white">
                  {isGreeting ? '자기소개' : `질문 ${currentQuestionIndex + 1}`}
                </span>
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
                  {isGreeting ? '간단하게 자기소개를 해주세요' : currentQuestion?.content}
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
                <span className="text-[10px] font-black uppercase tracking-tighter text-text-primary">{phase === 'recording' ? '답변 중' : '대기'}</span>
              </div>
            </div>
          </div>

          {/* Transcript Area */}
          <div className="flex-1 rounded-[32px] bg-surface border border-border p-6 flex flex-col gap-4 overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-black text-text-tertiary uppercase tracking-widest">실시간 음성 인식</span>
              <div className="flex gap-1">
                {[1, 2, 3].map(i => <div key={i} className="h-1 w-1 rounded-full bg-text-tertiary/30" />)}
              </div>
            </div>
            <div className="flex-1 overflow-y-auto custom-scrollbar pr-2">
              <TranscriptDisplay />
            </div>
          </div>
        </div>
      </main>

      {/* Floating Controls — 좌측 패널 기준 가운데 정렬 */}
      <div className="px-6 pb-6">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="flex-1">
            <InterviewControls
              phase={phase}
              isTtsSpeaking={isTtsSpeaking}
              onStartAnswer={handleStartAnswer}
              onStopAnswer={handleStopAnswer}
              onFinishInterview={handleFinishInterview}
            />
          </div>
          <div className="hidden lg:block lg:w-[400px]" />
        </div>
      </div>
    </div>
  )
}
