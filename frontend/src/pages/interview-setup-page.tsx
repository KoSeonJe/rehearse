import { Helmet } from 'react-helmet-async'
import { Logo } from '@/components/ui/logo'
import { BackLink } from '@/components/ui/back-link'
import { useInterviewSetup } from '@/hooks/use-interview-setup'
import { StepPosition } from '@/components/setup/step-position'
import { StepTechStack } from '@/components/setup/step-tech-stack'
import { StepLevel } from '@/components/setup/step-level'
import { StepDuration } from '@/components/setup/step-duration'
import { StepInterviewType } from '@/components/setup/step-interview-type'
import { SetupNavigation } from '@/components/setup/setup-navigation'
import { PageGrid } from '@/components/layout/page-grid'
import { UtilityBar } from '@/components/layout/utility-bar'

const STEP_LABELS: Record<number, string> = {
  1: '직무 선택',
  2: '기술 스택',
  3: '경력 수준',
  4: '면접 시간',
  5: '면접 유형',
}

export const InterviewSetupPage = () => {
  const setup = useInterviewSetup()

  return (
    <div className="min-h-screen bg-background">
      <Helmet>
        <title>면접 설정 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>

      {/* 유틸리티 바 — SaaS 헤더 대신 얇은 컨텍스트 바 */}
      <UtilityBar
        chapter={`SETUP · ${setup.currentStep} / ${setup.totalSteps}`}
        actions={
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Logo size={60} />
              <span className="text-sm font-bold tracking-tight text-foreground hidden sm:block">
                리허설
              </span>
            </div>
            <BackLink to="/" />
          </div>
        }
      />

      {/* 12-col 4+8 split */}
      <PageGrid as="main" className="mt-0 pb-32 pt-8 lg:pt-12 items-start">

        {/* 좌 4-col — 스텝 목차 (lg+에서만 표시) */}
        <aside
          className="hidden lg:block lg:col-span-4 sticky top-[calc(var(--utility-bar-height)+2rem)] self-start"
          aria-label="설정 단계 목록"
        >
          <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.1em] text-muted-foreground mb-6">
            진행 상황
          </p>
          <ol className="space-y-1" aria-label="단계 목차">
            {Array.from({ length: setup.totalSteps }, (_, i) => i + 1).map((step) => {
              const isDone = step < setup.currentStep
              const isActive = step === setup.currentStep

              return (
                <li key={step} className="flex items-center gap-3 py-2">
                  <span
                    className={`flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-[11px] font-bold transition-colors ${
                      isDone
                        ? 'bg-foreground text-background'
                        : isActive
                          ? 'bg-foreground text-background ring-2 ring-foreground/20 ring-offset-2'
                          : 'bg-muted text-muted-foreground'
                    }`}
                    aria-hidden="true"
                  >
                    {isDone ? (
                      <svg width="10" height="10" viewBox="0 0 12 12" fill="currentColor" aria-hidden="true">
                        <path d="M2 6l3 3 5-5" stroke="currentColor" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    ) : step}
                  </span>
                  <span
                    className={`text-sm font-medium transition-colors ${
                      isActive ? 'text-foreground font-bold' : isDone ? 'text-foreground/60' : 'text-muted-foreground'
                    }`}
                  >
                    {STEP_LABELS[step]}
                  </span>
                </li>
              )
            })}
          </ol>

          {/* 선택 누적 요약 */}
          {setup.position && (
            <div className="mt-10 border-t border-foreground/10 pt-6 space-y-2">
              <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.1em] text-muted-foreground mb-3">
                선택 내역
              </p>
              {setup.position && (
                <p className="text-xs text-foreground/70">
                  <span className="font-semibold text-foreground">직무</span> · {setup.position}
                </p>
              )}
              {setup.level && (
                <p className="text-xs text-foreground/70">
                  <span className="font-semibold text-foreground">수준</span> · {setup.level}
                </p>
              )}
              {setup.durationMinutes && (
                <p className="text-xs text-foreground/70">
                  <span className="font-semibold text-foreground">시간</span> · {setup.durationMinutes}분
                </p>
              )}
            </div>
          )}
        </aside>

        {/* 모바일 — 상단 수평 탭바 (< lg) */}
        <div className="col-span-4 lg:hidden mb-6">
          <div
            className="flex gap-0 overflow-x-auto scrollbar-none border-b border-foreground/10 -mx-4 px-4"
            role="tablist"
            aria-label="설정 단계"
          >
            {Array.from({ length: setup.totalSteps }, (_, i) => i + 1).map((step) => (
              <div
                key={step}
                role="tab"
                aria-selected={step === setup.currentStep}
                className={`flex-shrink-0 px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors ${
                  step === setup.currentStep
                    ? 'border-foreground text-foreground'
                    : step < setup.currentStep
                      ? 'border-transparent text-foreground/50'
                      : 'border-transparent text-muted-foreground'
                }`}
              >
                {STEP_LABELS[step]}
              </div>
            ))}
          </div>
        </div>

        {/* 우 8-col — 메인 콘텐츠 */}
        <div className="col-span-4 md:col-span-8 lg:col-span-8 min-w-0">

          {setup.currentStep === 1 && (
            <StepPosition
              position={setup.position}
              isLoading={setup.isLoading}
              onSelect={setup.handlePositionSelect}
            />
          )}

          {setup.currentStep === 2 && setup.position && (
            <StepTechStack
              position={setup.position}
              techStack={setup.techStack}
              isLoading={setup.isLoading}
              onSelect={setup.handleTechStackSelect}
            />
          )}

          {setup.currentStep === 3 && (
            <StepLevel
              level={setup.level}
              isLoading={setup.isLoading}
              onSelect={setup.handleLevelSelect}
            />
          )}

          {setup.currentStep === 4 && (
            <StepDuration
              durationMinutes={setup.durationMinutes}
              isLoading={setup.isLoading}
              onSelect={setup.handleDurationSelect}
            />
          )}

          {setup.currentStep === 5 && setup.position && (
            <StepInterviewType
              position={setup.position}
              techStack={setup.techStack}
              interviewTypes={setup.interviewTypes}
              csSubTopics={setup.csSubTopics}
              resumeFile={setup.resumeFile}
              dragOver={setup.dragOver}
              isLoading={setup.isLoading}
              fileInputRef={setup.fileInputRef}
              onTypeToggle={setup.handleTypeToggle}
              onCsSubTopicToggle={setup.handleCsSubTopicToggle}
              onFileSelect={setup.handleFileSelect}
              onFileRemove={setup.handleFileRemove}
              onDrop={setup.handleDrop}
              onDragOver={setup.handleDragOver}
              onDragLeave={setup.handleDragLeave}
            />
          )}

          <SetupNavigation
            currentStep={setup.currentStep}
            isSubmitStep={setup.isSubmitStep}
            canNext={setup.canNext(setup.currentStep)}
            isLoading={setup.isLoading}
            serverError={setup.serverError}
            onNext={setup.handleNext}
            onPrev={setup.handlePrev}
            onSubmit={setup.handleSubmit}
          />
        </div>
      </PageGrid>
    </div>
  )
}
