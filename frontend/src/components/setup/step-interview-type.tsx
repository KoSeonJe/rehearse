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
      <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-violet-legacy mb-3">
        Step 4 — Interview Type
      </p>
      <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
        어떤 면접을 연습할까요?
      </h1>
      <p className="mt-3 text-base font-medium text-text-secondary">
        여러 개를 선택할 수 있습니다
      </p>

      {/* TODO(design): variant 판단 보류 — 선택 카드 패턴(active/inactive 조건부 스타일), 사용자 확인 필요 */}
      <div className="mt-10 space-y-3">
        {POSITION_INTERVIEW_TYPES[position].map((type) => (
          <div key={type}>
            <button
              onClick={() => onTypeToggle(type)}
              disabled={isLoading}
              className={`flex w-full items-center justify-between rounded-[20px] p-5 text-left transition-all active:scale-[0.98] ${
                interviewTypes.includes(type)
                  ? 'bg-violet-legacy text-white shadow-lg shadow-violet-legacy/20'
                  : 'bg-surface text-text-primary hover:bg-slate-200'
              }`}
            >
              <div className="flex flex-col gap-1">
                <span className="text-base font-extrabold">
                  {getInterviewTypeLabel(type, techStack, position).label}
                </span>
                <span
                  className={`text-xs font-medium ${
                    interviewTypes.includes(type)
                      ? 'text-white/80'
                      : 'text-text-secondary'
                  }`}
                >
                  {getInterviewTypeLabel(type, techStack, position).description}
                </span>
              </div>
              <div
                className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border-2 transition-all ${
                  interviewTypes.includes(type)
                    ? 'border-white bg-white text-text-primary'
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
                    onClick={() => onCsSubTopicToggle(topic)}
                    disabled={isLoading}
                    className={`rounded-full px-4 py-2 text-xs font-bold transition-all active:scale-95 ${
                      csSubTopics.includes(topic)
                        ? 'bg-secondary text-text-primary ring-1 ring-border'
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
        ))}
      </div>
    </section>
  )
}
