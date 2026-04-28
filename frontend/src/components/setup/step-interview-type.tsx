import type { RefObject } from 'react'
import { CS_SUB_TOPICS } from '@/constants/setup'
import {
  CS_SUB_TOPIC_LABELS,
  POSITION_INTERVIEW_TYPES,
  getInterviewTypeLabel,
} from '@/constants/interview-labels'
import type { Position, InterviewType, CsSubTopic, TechStack } from '@/types/interview'
import { ResumeUpload } from '@/components/setup/resume-upload'

interface StepInterviewTypeProps {
  position: Position
  techStack: TechStack | null
  interviewTypes: InterviewType[]
  csSubTopics: CsSubTopic[]
  resumeFile: File | null
  dragOver: boolean
  isLoading: boolean
  isOtherTypesDisabled: boolean
  fileInputRef: RefObject<HTMLInputElement>
  onTypeToggle: (type: InterviewType) => void
  onCsSubTopicToggle: (topic: CsSubTopic) => void
  onFileSelect: (file: File) => void
  onFileRemove: () => void
  onDrop: (e: React.DragEvent) => void
  onDragOver: () => void
  onDragLeave: () => void
}

export const StepInterviewType = ({
  position,
  techStack,
  interviewTypes,
  csSubTopics,
  resumeFile,
  dragOver,
  isLoading,
  isOtherTypesDisabled,
  fileInputRef,
  onTypeToggle,
  onCsSubTopicToggle,
  onFileSelect,
  onFileRemove,
  onDrop,
  onDragOver,
  onDragLeave,
}: StepInterviewTypeProps) => {
  return (
    <section className="motion-safe:animate-fadeIn">
      <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
        어떤 면접을 연습할까요?
      </h1>
      <p className="mt-3 text-base font-medium text-text-secondary">
        {isOtherTypesDisabled ? '이력서 면접 단독 모드입니다' : '여러 개를 선택할 수 있습니다'}
      </p>

      {isOtherTypesDisabled && (
        <div
          role="status"
          aria-live="polite"
          className="mt-4 flex items-start gap-2 rounded-xl border border-brand/30 bg-brand-bg px-4 py-3"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="mt-0.5 shrink-0 text-brand"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <p className="text-sm font-medium text-brand">
            이력서 면접은 이력서 전용 모드로 진행됩니다. 다른 면접 유형과 함께 선택할 수 없습니다.
          </p>
        </div>
      )}

      <div className="mt-6 space-y-3">
        {POSITION_INTERVIEW_TYPES[position].map((type) => {
          const isActive = interviewTypes.includes(type)
          const isDisabled = isLoading || (isOtherTypesDisabled && type !== 'RESUME_BASED')

          return (
            <div key={type}>
              <button
                onClick={() => onTypeToggle(type)}
                disabled={isDisabled}
                aria-pressed={isActive}
                className={`flex w-full items-center justify-between rounded-2xl p-5 text-left transition-colors ${
                  isActive
                    ? 'bg-brand text-brand-foreground shadow-lg shadow-brand/25'
                    : isDisabled
                      ? 'cursor-not-allowed bg-muted/40 text-text-tertiary'
                      : 'bg-surface text-text-primary hover:bg-muted active:scale-[0.98]'
                }`}
              >
                <div className="flex flex-col gap-1">
                  <span className="text-base font-extrabold">
                    {getInterviewTypeLabel(type, techStack, position).label}
                  </span>
                  <span
                    className={`text-xs font-medium ${
                      isActive ? 'text-white/80' : 'text-text-secondary'
                    }`}
                  >
                    {getInterviewTypeLabel(type, techStack, position).description}
                  </span>
                </div>
                <div
                  aria-hidden="true"
                  className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border-2 transition-colors ${
                    isActive
                      ? 'border-brand-foreground bg-brand-foreground text-brand'
                      : 'border-text-tertiary'
                  }`}
                >
                  {isActive && (
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                      <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                    </svg>
                  )}
                </div>
              </button>

              {/* CS 세부 주제 */}
              {type === 'CS_FUNDAMENTAL' && isActive && (
                <div className="mt-2 ml-4 flex flex-wrap gap-2 motion-safe:animate-fadeIn">
                  {CS_SUB_TOPICS.map((topic) => (
                    <button
                      key={topic}
                      onClick={() => onCsSubTopicToggle(topic)}
                      disabled={isLoading}
                      className={`rounded-full px-4 py-2 text-xs font-bold transition-colors active:scale-95 ${
                        csSubTopics.includes(topic)
                          ? 'bg-secondary text-text-primary ring-1 ring-border'
                          : 'bg-surface text-text-secondary hover:bg-muted'
                      }`}
                    >
                      {CS_SUB_TOPIC_LABELS[topic]}
                    </button>
                  ))}
                  {csSubTopics.length === 0 && (
                    <span className="ml-1 text-[11px] font-medium text-text-tertiary">
                      미선택 시 전체 출제
                    </span>
                  )}
                </div>
              )}

              {/* 이력서 업로드 (인라인) */}
              {type === 'RESUME_BASED' && isActive && (
                <ResumeUpload
                  resumeFile={resumeFile}
                  dragOver={dragOver}
                  isLoading={isLoading}
                  fileInputRef={fileInputRef}
                  onFileSelect={onFileSelect}
                  onFileRemove={onFileRemove}
                  onDrop={onDrop}
                  onDragOver={onDragOver}
                  onDragLeave={onDragLeave}
                />
              )}
            </div>
          )
        })}
      </div>
    </section>
  )
}
