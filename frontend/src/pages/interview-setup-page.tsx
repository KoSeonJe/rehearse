import { useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'
import { BackLink } from '@/components/ui/back-link'
import { useCreateInterview } from '@/hooks/use-interviews'
import { ApiError } from '@/lib/api-client'
import { formatFileSize } from '@/lib/format-utils'
import type {
  Position,
  Level,
  InterviewType,
  CsSubTopic,
} from '@/types/interview'
import {
  POSITION_LABELS,
  LEVEL_LABELS,
  INTERVIEW_TYPE_LABELS,
  CS_SUB_TOPIC_LABELS,
  POSITION_INTERVIEW_TYPES,
} from '@/types/interview'

const POSITIONS: Position[] = ['BACKEND', 'FRONTEND', 'DEVOPS', 'DATA_ENGINEER', 'FULLSTACK']
const LEVELS: Level[] = ['JUNIOR', 'MID', 'SENIOR']
const CS_SUB_TOPICS: CsSubTopic[] = ['DATA_STRUCTURE', 'OS', 'NETWORK', 'DATABASE']
const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

const DURATION_PRESETS = [
  { minutes: 15, label: '15분', description: '빠른 연습' },
  { minutes: 30, label: '30분', description: '기본 면접' },
  { minutes: 45, label: '45분', description: '심화 면접' },
  { minutes: 60, label: '60분', description: '풀 면접' },
] as const

type Step = 1 | 2 | 3 | 4

export const InterviewSetupPage = () => {
  const navigate = useNavigate()
  const createInterview = useCreateInterview()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [currentStep, setCurrentStep] = useState<Step>(1)
  const [position, setPosition] = useState<Position | null>(null)
  const [level, setLevel] = useState<Level | null>(null)
  const [durationMinutes, setDurationMinutes] = useState(30)
  const [interviewTypes, setInterviewTypes] = useState<InterviewType[]>([])
  const [csSubTopics, setCsSubTopics] = useState<CsSubTopic[]>([])
  const [resumeFile, setResumeFile] = useState<File | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const [dragOver, setDragOver] = useState(false)

  const isLoading = createInterview.isPending
  const totalSteps = 4

  const canNext = (step: Step): boolean => {
    switch (step) {
      case 1:
        return position !== null
      case 2:
        return level !== null
      case 3:
        return durationMinutes >= 5 && durationMinutes <= 120
      case 4:
        return interviewTypes.length > 0
    }
  }

  const isSubmitStep = currentStep === totalSteps

  const handleNext = () => {
    if (!canNext(currentStep)) return
    if (currentStep < totalSteps) {
      setCurrentStep((currentStep + 1) as Step)
    }
  }

  const handlePrev = () => {
    if (currentStep > 1) {
      setCurrentStep((currentStep - 1) as Step)
    }
  }

  const handlePositionSelect = (p: Position) => {
    if (position !== p) {
      setPosition(p)
      const availableTypes = POSITION_INTERVIEW_TYPES[p]
      setInterviewTypes((prev) => prev.filter((t) => availableTypes.includes(t)))
    }
  }

  const handleTypeToggle = (type: InterviewType) => {
    setInterviewTypes((prev) => {
      if (prev.includes(type)) {
        const next = prev.filter((t) => t !== type)
        if (type === 'CS_FUNDAMENTAL') setCsSubTopics([])
        if (type === 'RESUME_BASED') {
          setResumeFile(null)
          if (fileInputRef.current) fileInputRef.current.value = ''
        }
        return next
      }
      return [...prev, type]
    })
  }

  const handleCsSubTopicToggle = (topic: CsSubTopic) => {
    setCsSubTopics((prev) =>
      prev.includes(topic) ? prev.filter((t) => t !== topic) : [...prev, topic],
    )
  }

  const handleFileSelect = useCallback((file: File) => {
    if (file.type !== 'application/pdf') {
      setServerError('PDF 파일만 업로드할 수 있습니다.')
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      setServerError('파일 크기는 10MB 이하여야 합니다.')
      return
    }
    setServerError(null)
    setResumeFile(file)
  }, [])

  const handleFileRemove = () => {
    setResumeFile(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setDragOver(false)
      const file = e.dataTransfer.files[0]
      if (file) handleFileSelect(file)
    },
    [handleFileSelect],
  )

  const handleSubmit = () => {
    if (!position || !level || interviewTypes.length === 0 || isLoading) return
    setServerError(null)

    createInterview.mutate(
      {
        request: {
          position,
          level,
          interviewTypes,
          durationMinutes,
          csSubTopics: interviewTypes.includes('CS_FUNDAMENTAL') && csSubTopics.length > 0
            ? csSubTopics
            : undefined,
        },
        resumeFile,
      },
      {
        onSuccess: (response) => {
          navigate(`/interview/${response.data.id}/ready`)
        },
        onError: (error) => {
          if (error instanceof ApiError) {
            setServerError(error.message || '오류가 발생했습니다.')
          } else {
            setServerError('오류가 발생했습니다.')
          }
        },
      },
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
        <BackLink to="/" />
      </header>

      <main className="mx-auto max-w-2xl px-5 pb-32 pt-12 md:px-8">
        {/* Progress Bar */}
        <div className="mb-12">
          <div className="flex items-center justify-between mb-2">
            {Array.from({ length: totalSteps }, (_, i) => i + 1).map((step) => (
              <div key={step} className="flex items-center gap-1">
                <div
                  className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-black transition-all ${
                    step < currentStep
                      ? 'bg-accent text-white'
                      : step === currentStep
                        ? 'bg-accent text-white shadow-lg shadow-accent/20'
                        : 'bg-surface text-text-tertiary'
                  }`}
                >
                  {step < currentStep ? (
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                    </svg>
                  ) : (
                    step
                  )}
                </div>
              </div>
            ))}
          </div>
          <div className="flex gap-1">
            {Array.from({ length: totalSteps }, (_, i) => i + 1).map((step) => (
              <div
                key={step}
                className={`h-1 flex-1 rounded-full transition-all ${
                  step <= currentStep ? 'bg-accent' : 'bg-surface'
                }`}
              />
            ))}
          </div>
          <p className="mt-2 text-right text-[11px] font-bold text-text-tertiary">
            {currentStep} / {totalSteps}
          </p>
        </div>

        {/* Step 1: 직무 선택 */}
        {currentStep === 1 && (
          <section className="motion-safe:animate-fadeIn">
            <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
              Step 1 — Position
            </p>
            <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
              어떤 직무를 준비하고 계신가요?
            </h1>
            <p className="mt-3 text-base font-medium text-text-secondary">
              직무에 맞는 면접 유형을 추천해드립니다
            </p>

            <div className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-3">
              {POSITIONS.map((p) => (
                <button
                  key={p}
                  onClick={() => handlePositionSelect(p)}
                  disabled={isLoading}
                  className={`flex flex-col items-center gap-2 rounded-[20px] p-5 transition-all active:scale-95 ${
                    position === p
                      ? 'bg-accent text-white shadow-lg shadow-accent/20'
                      : 'bg-surface text-text-primary hover:bg-slate-200'
                  }`}
                >
                  <span className="text-base font-extrabold">{POSITION_LABELS[p].label}</span>
                  <span
                    className={`text-[11px] font-medium ${
                      position === p ? 'text-white/80' : 'text-text-tertiary'
                    }`}
                  >
                    {POSITION_LABELS[p].description}
                  </span>
                </button>
              ))}
            </div>
          </section>
        )}

        {/* Step 2: 레벨 선택 */}
        {currentStep === 2 && (
          <section className="motion-safe:animate-fadeIn">
            <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
              Step 2 — Level
            </p>
            <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
              경력 수준을 선택해주세요
            </h1>
            <p className="mt-3 text-base font-medium text-text-secondary">
              레벨에 따라 질문 난이도가 달라집니다
            </p>

            <div className="mt-10 space-y-3">
              {LEVELS.map((l) => (
                <button
                  key={l}
                  onClick={() => setLevel(l)}
                  disabled={isLoading}
                  className={`flex w-full items-center justify-between rounded-[20px] p-6 text-left transition-all active:scale-[0.98] ${
                    level === l
                      ? 'bg-accent text-white shadow-lg shadow-accent/20'
                      : 'bg-surface text-text-primary hover:bg-slate-200'
                  }`}
                >
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-extrabold">{LEVEL_LABELS[l].label}</span>
                      <span
                        className={`text-xs font-bold ${
                          level === l ? 'text-white/60' : 'text-text-tertiary'
                        }`}
                      >
                        {LEVEL_LABELS[l].description}
                      </span>
                    </div>
                    <span
                      className={`text-[13px] font-medium ${
                        level === l ? 'text-white/80' : 'text-text-secondary'
                      }`}
                    >
                      {LEVEL_LABELS[l].hint}
                    </span>
                  </div>
                  {level === l && (
                    <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white text-accent">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                      </svg>
                    </div>
                  )}
                </button>
              ))}
            </div>
          </section>
        )}

        {/* Step 3: 면접 시간 선택 (프리셋 카드) */}
        {currentStep === 3 && (
          <section className="motion-safe:animate-fadeIn">
            <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
              Step 3 — Duration
            </p>
            <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
              면접 시간을 선택해주세요
            </h1>
            <p className="mt-3 text-base font-medium text-text-secondary">
              시간에 맞춰 질문 수가 자동으로 조정됩니다
            </p>

            <div className="mt-10 grid grid-cols-2 gap-3">
              {DURATION_PRESETS.map((preset) => (
                <button
                  key={preset.minutes}
                  onClick={() => setDurationMinutes(preset.minutes)}
                  disabled={isLoading}
                  className={`flex flex-col items-center gap-2 rounded-[20px] p-6 transition-all active:scale-95 ${
                    durationMinutes === preset.minutes
                      ? 'bg-accent text-white shadow-lg shadow-accent/20'
                      : 'bg-surface text-text-primary hover:bg-slate-200'
                  }`}
                >
                  <span className="text-3xl font-extrabold">{preset.label}</span>
                  <span
                    className={`text-sm font-medium ${
                      durationMinutes === preset.minutes ? 'text-white/80' : 'text-text-secondary'
                    }`}
                  >
                    {preset.description}
                  </span>
                </button>
              ))}
            </div>

            <p className="mt-6 text-center text-xs font-medium text-text-tertiary">
              면접 종료 2분 전에 마무리 안내가 표시됩니다
            </p>
          </section>
        )}

        {/* Step 4: 면접 유형 선택 */}
        {currentStep === 4 && position && (
          <section className="motion-safe:animate-fadeIn">
            <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
              Step 4 — Interview Type
            </p>
            <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
              어떤 면접을 연습할까요?
            </h1>
            <p className="mt-3 text-base font-medium text-text-secondary">
              여러 개를 선택할 수 있습니다
            </p>

            <div className="mt-10 space-y-3">
              {POSITION_INTERVIEW_TYPES[position]
                .map((type) => (
                  <div key={type}>
                    <button
                      onClick={() => handleTypeToggle(type)}
                      disabled={isLoading}
                      className={`flex w-full items-center justify-between rounded-[20px] p-5 text-left transition-all active:scale-[0.98] ${
                        interviewTypes.includes(type)
                          ? 'bg-accent text-white shadow-lg shadow-accent/20'
                          : 'bg-surface text-text-primary hover:bg-slate-200'
                      }`}
                    >
                      <div className="flex flex-col gap-1">
                        <span className="text-base font-extrabold">
                          {INTERVIEW_TYPE_LABELS[type].label}
                        </span>
                        <span
                          className={`text-xs font-medium ${
                            interviewTypes.includes(type)
                              ? 'text-white/80'
                              : 'text-text-secondary'
                          }`}
                        >
                          {INTERVIEW_TYPE_LABELS[type].description}
                        </span>
                      </div>
                      <div
                        className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border-2 transition-all ${
                          interviewTypes.includes(type)
                            ? 'border-white bg-white text-accent'
                            : 'border-text-tertiary'
                        }`}
                      >
                        {interviewTypes.includes(type) && (
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                          </svg>
                        )}
                      </div>
                    </button>

                    {/* CS 세부 주제 */}
                    {type === 'CS_FUNDAMENTAL' && interviewTypes.includes('CS_FUNDAMENTAL') && (
                      <div className="mt-2 ml-4 flex flex-wrap gap-2 motion-safe:animate-fadeIn">
                        {CS_SUB_TOPICS.map((topic) => (
                          <button
                            key={topic}
                            onClick={() => handleCsSubTopicToggle(topic)}
                            disabled={isLoading}
                            className={`rounded-full px-4 py-2 text-xs font-bold transition-all active:scale-95 ${
                              csSubTopics.includes(topic)
                                ? 'bg-accent/10 text-accent ring-1 ring-accent/30'
                                : 'bg-surface text-text-secondary hover:bg-slate-200'
                            }`}
                          >
                            {CS_SUB_TOPIC_LABELS[topic]}
                          </button>
                        ))}
                        {csSubTopics.length === 0 && (
                          <span className="text-[11px] font-medium text-text-tertiary ml-1">
                            미선택 시 전체 출제
                          </span>
                        )}
                      </div>
                    )}

                    {/* 이력서 업로드 (인라인) */}
                    {type === 'RESUME_BASED' && interviewTypes.includes('RESUME_BASED') && (
                      <div className="mt-2 ml-4 motion-safe:animate-fadeIn">
                        {/* 안심 문구 */}
                        <div className="flex items-center gap-2 rounded-[12px] bg-green-50 px-3 py-2 mb-2">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-green-600 shrink-0">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                          </svg>
                          <span className="text-[11px] font-bold text-green-700">
                            이력서는 질문 생성에만 사용되며, 어디에도 저장되지 않습니다
                          </span>
                        </div>

                        {!resumeFile ? (
                          <div
                            onDragOver={(e) => {
                              e.preventDefault()
                              setDragOver(true)
                            }}
                            onDragLeave={() => setDragOver(false)}
                            onDrop={handleDrop}
                            onClick={() => fileInputRef.current?.click()}
                            className={`flex cursor-pointer items-center gap-3 rounded-[16px] border-2 border-dashed p-4 transition-all ${
                              dragOver
                                ? 'border-accent bg-accent/5'
                                : 'border-border bg-surface hover:border-accent/50 hover:bg-slate-50'
                            }`}
                          >
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-text-tertiary shrink-0">
                              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                              <polyline points="17 8 12 3 7 8" />
                              <line x1="12" y1="3" x2="12" y2="15" />
                            </svg>
                            <div>
                              <p className="text-xs font-bold text-text-primary">
                                PDF 파일을 드래그하거나 클릭하여 업로드
                              </p>
                              <p className="text-[11px] font-medium text-text-tertiary">
                                선택사항 · 최대 10MB
                              </p>
                            </div>
                          </div>
                        ) : (
                          <div className="flex items-center justify-between rounded-[16px] bg-surface p-4">
                            <div className="flex items-center gap-3">
                              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/10">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-accent">
                                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                  <polyline points="14 2 14 8 20 8" />
                                </svg>
                              </div>
                              <div>
                                <p className="text-xs font-bold text-text-primary">{resumeFile.name}</p>
                                <p className="text-[11px] font-medium text-text-tertiary">
                                  {formatFileSize(resumeFile.size)}
                                </p>
                              </div>
                            </div>
                            <button
                              onClick={handleFileRemove}
                              disabled={isLoading}
                              className="flex h-7 w-7 items-center justify-center rounded-full bg-white text-text-tertiary transition-all hover:bg-red-50 hover:text-red-500"
                            >
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18" />
                                <line x1="6" y1="6" x2="18" y2="18" />
                              </svg>
                            </button>
                          </div>
                        )}
                        <input
                          ref={fileInputRef}
                          type="file"
                          accept="application/pdf"
                          className="hidden"
                          onChange={(e) => {
                            const file = e.target.files?.[0]
                            if (file) handleFileSelect(file)
                          }}
                        />
                      </div>
                    )}
                  </div>
                ))}
            </div>
          </section>
        )}

        {/* Navigation Buttons */}
        <div className="mt-16 space-y-3">
          {isSubmitStep ? (
            <button
              onClick={handleSubmit}
              disabled={!canNext(currentStep) || isLoading}
              className="h-16 w-full rounded-[24px] bg-accent py-4 text-lg font-black text-white shadow-lg shadow-accent/20 transition-all active:scale-95 disabled:opacity-50"
            >
              {isLoading ? '면접관이 질문을 생성 중입니다...' : '면접 시작하기'}
            </button>
          ) : (
            <button
              onClick={handleNext}
              disabled={!canNext(currentStep) || isLoading}
              className={`h-16 w-full rounded-[24px] py-4 text-lg font-black text-white transition-all active:scale-95 shadow-lg ${
                canNext(currentStep) && !isLoading
                  ? 'bg-accent shadow-accent/20'
                  : 'bg-slate-200 cursor-not-allowed opacity-50'
              }`}
            >
              다음
            </button>
          )}

          {currentStep > 1 && (
            <button
              onClick={handlePrev}
              disabled={isLoading}
              className="h-14 w-full rounded-[24px] bg-surface py-3 text-base font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95 disabled:opacity-50"
            >
              이전
            </button>
          )}

          {serverError && (
            <p className="mt-2 text-center text-sm font-bold text-error" role="alert">
              {serverError}
            </p>
          )}
        </div>
      </main>
    </div>
  )
}
