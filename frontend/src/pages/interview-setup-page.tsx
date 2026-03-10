import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'
import { BackLink } from '@/components/ui/back-link'
import { Button } from '@/components/ui/button'
import { TextInput } from '@/components/ui/text-input'
import { SelectionCard } from '@/components/ui/selection-card'
import { useCreateInterview } from '@/hooks/use-interviews'
import { ApiError } from '@/lib/api-client'
import type { Level, InterviewType } from '@/types/interview'
import { LEVEL_LABELS, INTERVIEW_TYPE_LABELS } from '@/types/interview'

const LEVELS: Level[] = ['JUNIOR', 'MID', 'SENIOR']
const INTERVIEW_TYPES: InterviewType[] = ['CS', 'SYSTEM_DESIGN', 'BEHAVIORAL']

export const InterviewSetupPage = () => {
  const navigate = useNavigate()
  const createInterview = useCreateInterview()

  const [position, setPosition] = useState('')
  const [level, setLevel] = useState<Level | null>(null)
  const [interviewType, setInterviewType] = useState<InterviewType | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const isValid =
    position.trim().length > 0 && level !== null && interviewType !== null
  const isLoading = createInterview.isPending

  const handleSubmit = () => {
    if (!isValid || isLoading || !level || !interviewType) return
    setServerError(null)
    setFieldErrors({})

    createInterview.mutate(
      { position: position.trim(), level, interviewType },
      {
        onSuccess: (response) => {
          navigate(`/interview/${response.data.id}/ready`)
        },
        onError: (error) => {
          if (error instanceof ApiError) {
            if (error.errors.length > 0) {
              const errors: Record<string, string> = {}
              for (const err of error.errors) {
                errors[err.field] = err.reason
              }
              setFieldErrors(errors)
            } else {
              setServerError(error.message || '오류가 발생했습니다.')
            }
          } else {
            setServerError('오류가 발생했습니다.')
          }
        },
      },
    )
  }

  return (
    <div className="min-h-screen bg-white">
      {/* Toss-style Header */}
      <header className="flex items-center justify-between px-5 pt-8 md:px-8">
        <div className="flex items-center gap-2">
          <Logo size={80} />
          <span className="text-xl font-extrabold tracking-tight text-text-primary">
            리허설
          </span>
        </div>
        <BackLink to="/" />
      </header>

      <main className="mx-auto max-w-2xl px-5 pb-32 pt-16 md:px-8">
        {/* Step Intro */}
        <section className="mb-16">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
            Setup Interview
          </p>
          <h1 className="text-4xl font-extrabold tracking-tighter text-text-primary sm:text-5xl">
            면접 질문을<br />
            <span className="text-accent">준비해 드릴게요.</span>
          </h1>
          <p className="mt-4 text-lg font-medium text-text-secondary">
            당신에게 딱 맞는 면접 경험을 위해<br />
            몇 가지만 알려주세요.
          </p>
        </section>

        {/* Form Sections */}
        <div className="space-y-12">
          {/* Position Input */}
          <section>
            <h2 className="mb-4 text-sm font-black uppercase tracking-widest text-text-tertiary">
              01. 상세 직무
            </h2>
            <div className="relative">
              <input
                type="text"
                placeholder="예: 시니어 백엔드 엔지니어"
                value={position}
                onChange={(e) => setPosition(e.target.value)}
                maxLength={100}
                disabled={isLoading}
                className={`w-full rounded-[20px] bg-surface px-6 py-5 text-lg font-bold text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-2 focus:ring-accent transition-all ${
                  fieldErrors['position'] ? 'ring-2 ring-error' : ''
                }`}
              />
              {fieldErrors['position'] && (
                <p className="mt-2 pl-2 text-xs font-bold text-error">{fieldErrors['position']}</p>
              )}
            </div>
          </section>

          {/* Level Selection */}
          <section>
            <h2 className="mb-4 text-sm font-black uppercase tracking-widest text-text-tertiary">
              02. 레벨
            </h2>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              {LEVELS.map((l) => (
                <button
                  key={l}
                  onClick={() => setLevel(l)}
                  disabled={isLoading}
                  className={`flex flex-col items-center gap-1 rounded-[20px] p-6 transition-all active:scale-95 ${
                    level === l
                      ? 'bg-accent text-white shadow-lg'
                      : 'bg-surface text-text-secondary hover:bg-slate-200'
                  }`}
                >
                  <span className="text-lg font-extrabold">{LEVEL_LABELS[l].label}</span>
                  <span className={`text-[11px] font-bold ${level === l ? 'text-white/80' : 'text-text-tertiary'}`}>
                    {LEVEL_LABELS[l].description}
                  </span>
                </button>
              ))}
            </div>
          </section>

          {/* Interview Type Selection */}
          <section>
            <h2 className="mb-4 text-sm font-black uppercase tracking-widest text-text-tertiary">
              03. 면접 유형
            </h2>
            <div className="space-y-3">
              {INTERVIEW_TYPES.map((t) => (
                <button
                  key={t}
                  onClick={() => setInterviewType(t)}
                  disabled={isLoading}
                  className={`flex w-full items-center justify-between rounded-[24px] p-6 text-left transition-all active:scale-[0.98] ${
                    interviewType === t
                      ? 'bg-accent text-white shadow-lg shadow-accent/20'
                      : 'bg-surface text-text-primary hover:bg-slate-200'
                  }`}
                >
                  <div className="flex flex-col gap-1">
                    <span className="text-lg font-extrabold">{INTERVIEW_TYPE_LABELS[t].label}</span>
                    <span className={`text-xs font-medium ${interviewType === t ? 'text-white/80' : 'text-text-secondary'}`}>
                      {INTERVIEW_TYPE_LABELS[t].description}
                    </span>
                  </div>
                  {interviewType === t && (
                    <div className="flex h-6 w-6 items-center justify-center rounded-full bg-white text-accent">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                      </svg>
                    </div>
                  )}
                </button>
              ))}
            </div>
          </section>
        </div>

        {/* Submit Area */}
        <div className="mt-20">
          <button
            onClick={handleSubmit}
            disabled={!isValid || isLoading}
            className={`w-full h-18 rounded-[24px] py-5 text-xl font-black text-white transition-all active:scale-95 shadow-lg ${
              isValid && !isLoading ? 'bg-accent shadow-accent/20' : 'bg-slate-200 cursor-not-allowed opacity-50'
            }`}
          >
            {isLoading ? '면접관이 질문을 생성 중입니다...' : '질문 생성하기'}
          </button>
          {serverError && (
            <p className="mt-4 text-center text-sm font-bold text-error" role="alert">
              {serverError}
            </p>
          )}
        </div>
      </main>
    </div>
  )
}
